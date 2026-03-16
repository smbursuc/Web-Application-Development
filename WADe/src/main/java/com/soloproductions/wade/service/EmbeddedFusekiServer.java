package com.soloproductions.wade.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.jena.fuseki.auth.Auth;
import org.apache.jena.fuseki.auth.AuthPolicy;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Embedded Fuseki lifecycle service that initializes named graphs and exposes
 * the in-process SPARQL endpoint used by the application.
 */
@Service
public class EmbeddedFusekiServer
{
    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(EmbeddedFusekiServer.class);

    /** Host base URL for local Fuseki service. */
    private static final String HOST = "http://127.0.0.1:3030";

    /** TDB2 dataset directory path. */
    private static final String DATASET_PATH = "data/impr"; // Persistent TDB2 storage

    /** Service path mounted by Fuseki. */
    private static final String SERVICE_PATH = "/impr";

    /** Network port used by Fuseki server. */
    private static final int PORT = 3030;

    /** Directory containing initial TTL files for graph bootstrap. */
    private static final String TTL_DIR = "./ttl-new";

    /** Embedded Fuseki server instance. */
    private FusekiServer server;

    /** Mapping of TTL filenames to target graph IRIs. */
    private static final Map<String, String> GRAPH_MAPPINGS = Map.of
    (
            "cluster_data_skos_bsds300.ttl", "http://example.org/bsds300_clusters",
            "cluster_data_skos_cifar10.ttl", "http://example.org/cifar10_clusters",
            "heatmap_data_skos_bsds300.ttl", "http://example.org/bsds300_heatmap",
            "heatmap_data_skos_cifar10.ttl", "http://example.org/cifar10_heatmap"
    );

    /**
     * Starts the embedded Fuseki server and loads bootstrap TTL graphs.
     */
    @PostConstruct
    public void start()
    {
        Dataset dataset = TDB2Factory.connectDataset(DATASET_PATH);

        try
        {
            File ttlDir = new File(TTL_DIR);
            if (!ttlDir.exists() || !ttlDir.isDirectory())
            {
                LOG.warn("TTL directory '{}' not found. Skipping RDF load.", TTL_DIR);
            }
            else
            {
                dataset.begin(TxnType.WRITE);
                boolean anyDataLoaded = false;

                try
                {
                    for (var entry : GRAPH_MAPPINGS.entrySet())
                    {
                        String fileName = entry.getKey();
                        String graphName = entry.getValue();

                        Path filePath = Path.of(ttlDir.getPath(), fileName);
                        File file = filePath.toFile();

                        if (file.exists())
                        {
                            if (!dataset.containsNamedModel(graphName))
                            {
                                Model model = RDFDataMgr.loadModel(file.toURI().toString());
                                dataset.addNamedModel(graphName, model);
                                LOG.info("Loaded data from '{}' into graph '{}'", fileName, graphName);
                                anyDataLoaded = true;
                            }
                            else
                            {
                                LOG.info("Graph '{}' already contains data, skipping", graphName);
                            }
                        }
                        else
                        {
                            LOG.warn("TTL file '{}' not found. Skipping graph '{}'.", fileName, graphName);
                        }
                    }

                    if (anyDataLoaded)
                    {
                        dataset.commit();
                        LOG.info("Successfully committed all graph data to TDB2 storage");
                    }
                    else
                    {
                        dataset.abort();
                        LOG.info("No new data loaded, transaction aborted");
                    }

                }
                catch (Exception e)
                {
                    dataset.abort();
                    LOG.error("Error loading TTL files, transaction aborted", e);
                    throw e;
                }
            }
        }
        catch (Exception e)
        {
            LOG.error("Error initializing Fuseki dataset", e);
            throw new RuntimeException("Failed to initialize Fuseki dataset", e);
        }

        // Configure and start server with unrestricted access
        AuthPolicy unrestricted = Auth.ANY_ANON;
        server = FusekiServer.create()
                .port(PORT)
                .add(SERVICE_PATH, dataset, true)
                .addOperation(SERVICE_PATH, Operation.Query, unrestricted)
                .addOperation(SERVICE_PATH, Operation.Update, unrestricted)
                .addOperation(SERVICE_PATH, Operation.GSP_RW, unrestricted)
                .build();

        server.start();
        LOG.info("Fuseki server started at http://localhost:{}{}", PORT, SERVICE_PATH);
    }

    /**
     * Stops the embedded Fuseki server on application shutdown.
     */
    @PreDestroy
    public void stop()
    {
        if (server != null)
        {
            server.stop();
            LOG.info("Fuseki server stopped.");
        }
    }

    /**
     * Returns the embedded Fuseki server instance.
     *
     * @return  running Fuseki server instance
     */
    public FusekiServer getServer()
    {
        return server;
    }

    /**
     * Returns the service endpoint base path used for SPARQL requests.
     *
     * @return  endpoint URL string
     */
    public String getEndpointPath()
    {
        return HOST + SERVICE_PATH;
    }
}
package com.soloproductions.wade.datatype;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dataset.*;
import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.ExportRequest;
import com.soloproductions.wade.metadata.DatasetMetadata;
import com.soloproductions.wade.service.EmbeddedFusekiServer;
import com.soloproductions.wade.util.ApplicationContextProvider;
import org.apache.jena.query.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Data type controller that reads and writes dataset data against an Apache Jena Fuseki
 * SPARQL endpoint. 
 * 
 * <p> Handles only read requests. </p>
 */
public class RdfController extends AbstractDataTypeController
{
    /** Fuseki server instance used to resolve the SPARQL endpoint path. */
    private EmbeddedFusekiServer fusekiServer;

    /** Suffix appended to the dataset name to form the cluster graph name. */
    private static final String GRAPH_CLUSTERS_END_PATH = "clusters";

    /** Suffix appended to the dataset name to form the heatmap graph name. */
    private static final String GRAPH_HEATMAP_END_PATH = "heatmap";

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(RdfController.class);

    /** SPARQL query endpoint URL. */
    private String endpoint;

    /**
     * Constructs an RDF controller that operates on the given dataset model.
     * Resolves the SPARQL endpoint from the injected Fuseki server bean.
     *
     * @param   datasetData
     *          dataset model to operate on
     */
    public RdfController(DatasetData datasetData)
    {
        super(datasetData);
        fusekiServer = ApplicationContextProvider.getBean(EmbeddedFusekiServer.class);
        endpoint = fusekiServer.getEndpointPath();
    }

    /**
     * Executes a SPARQL-backed read request, dispatching to the cluster or heatmap
     * query builder based on the active dataset type.
     *
     * @return  populated dataset entity for the read request
     *
     * @throws  UnsupportedOperationException
     *          when the dataset type is unrecognized
     */
    public Object executeReadRequest()
    {
        DatasetType dt = getDatasetData().getDatasetType();
        switch (dt)
        {
            case CLUSTERS ->
            {
                ClusterDataset cd = (ClusterDataset) getDatasetData();

                String name = cd.getClusterName();
                SortDirection sort = cd.getSortDirection();
                Integer range = cd.getRange();
                Integer rangeStart = cd.getRangeStart();
                String graph = getGraphName(cd.getDatasetName(), cd.getDatasetType());

                String sparqlQuery = buildClusterQuery(name, sort, range, rangeStart, graph);
                List<Map<String, String>> queryResults = executeSparqlQuery(sparqlQuery);

                return processClusterResults(queryResults);
            }
            case HEATMAP ->
            {
                SimilarityDataset sd = (SimilarityDataset) getDatasetData();

                SimilaritySortCriteria sortType = sd.getSimilaritySortCriteria();
                SortDirection sort = sd.getSortDirection();
                int range = sd.getRange();
                int rangeStart = sd.getRangeStart();
                String graph = getGraphName(sd.getDatasetName(), sd.getDatasetType());

                String sparqlQuery = buildHeatmapQuery(sortType, sort, range, rangeStart, graph);
                List<Map<String, String>> queryResults = executeSparqlQuery(sparqlQuery);

                return processHeatmapResults(queryResults);
            }
        }
        throw UnimplementedFeature(dt.name());
    }

    /**
     * Builds a {@link ClusterNode} tree from the raw SPARQL result rows for a cluster query.
     *
     * @param   queryResults
     *          raw rows returned by the cluster SPARQL query
     *
     * @return  populated cluster node tree rooted at a synthetic "root" node
     */
    public DataEntity processClusterResults(List<Map<String, String>> queryResults)
    {
        ClusterDataset cd = (ClusterDataset) getDatasetData();
        ClusterNode root = new ClusterNode();
        root.setName("root");
        List<ClusterNode> rootChildren = new ArrayList<>();
        for (Map<String, String> result : queryResults)
        {
            String clusterName = result.get("clusterLabel");
            String objectName = result.get("predictedObjectLabel");
            String probability = result.get("probability");
            String imageURI = result.get("imageURI");

            if (!containsCluster(clusterName, rootChildren))
            {
                ClusterNode newChild = new ClusterNode();
                List<ClusterNode> newChildChildren = new ArrayList<>();
                newChild.setChildren(newChildChildren);
                newChild.setName(clusterName);
                rootChildren.add(newChild);
            }

            ClusterNode leafNode = new ClusterNode();
            leafNode.setName(objectName);
            leafNode.setUri(imageURI);
            leafNode.setProbability(Double.valueOf(probability));
            ClusterNode leafParent = getCluster(clusterName, rootChildren);
            leafParent.addChild(leafNode);
        }

        root.setChildren(rootChildren);
        cd.setRootNode(root);

        return root;
    }

    /**
     * Builds a {@link SimilarityDataset} from raw SPARQL result rows for a heatmap query.
     *
     * @param   queryResults
     *          raw rows returned by the heatmap SPARQL query
     *
     * @return  similarity dataset with objects and matrix populated from the query results
     */
    public DataEntity processHeatmapResults(List<Map<String, String>> queryResults)
    {
        SimilarityDataset sd = (SimilarityDataset) getDatasetData();
        Set<String> uniqueObjects = new HashSet<>();
        for (Map<String, String> row : queryResults)
        {
            uniqueObjects.add((String) row.get("fromLabel"));
            uniqueObjects.add((String) row.get("toLabel"));
        }

        List<String> objects = new ArrayList<>(uniqueObjects);
        int n = objects.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++)
        {
            Arrays.fill(matrix[i], 0.0);
        }

        for (Map<String, String> row : queryResults)
        {
            String fromObject = (String) row.get("fromLabel");
            String toObject = (String) row.get("toLabel");
            double similarityValue = Double.parseDouble(row.get("similarityValue"));

            int fromIndex = objects.indexOf(fromObject);
            int toIndex = objects.indexOf(toObject);

            if (fromIndex != -1 && toIndex != -1)
            {
                matrix[fromIndex][toIndex] = similarityValue;
            }
        }

        List<List<Double>> list = new ArrayList<>();
        for (double[] row : matrix)
        {
            List<Double> inner = new ArrayList<>();
            for (double val : row)
            {
                inner.add(val);
            }
            list.add(inner);
        }

        sd.setObjects(objects);
        sd.setMatrix(list);
        return sd;
    }

    /**
     * Builds a SPARQL SELECT query for the cluster graph.
     *
     * @param   name
     *          optional cluster label filter; {@code null} returns all clusters
     * @param   sort
     *          sort direction to apply, or {@code null} for no sort
     * @param   range
     *          LIMIT value, or {@code null} to omit
     * @param   rangeStart
     *          OFFSET value, or {@code null} to omit
     * @param   graphURI
     *          local part of the named graph URI
     *
     * @return  SPARQL query string
     */
    public String buildClusterQuery(String name, SortDirection sort, Integer range, Integer rangeStart, String graphURI)
    {
        ClusterDataset cd = (ClusterDataset) getDatasetData();
        StringBuilder query = new StringBuilder();

        query.append("PREFIX ex: <http://example.org/ontology#>\n");
        query.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
        query.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
        query.append("SELECT ?clusterLabel ?predictedObjectLabel ?probability ?imageURI\n");
        query.append("WHERE {\n");
        query.append("  GRAPH <http://example.org/").append(graphURI).append("> {\n");
        query.append("    ?cluster a skos:Concept ;\n");
        query.append("             skos:prefLabel ?clusterLabel ;\n");
        query.append("             ex:hasPrediction ?prediction .\n");
        query.append("    ?prediction ex:hasProbability ?probability ;\n");
        query.append("                ex:hasURI ?imageURI ;\n");
        query.append("                ex:predictedObject ?predictedObject .\n");
        query.append("    ?predictedObject skos:prefLabel ?predictedObjectLabel .\n");
        query.append("  }\n");

        // TODO: wtf was I checking a regex for?
//        if (name != null && !name.isEmpty())
//        {
//            query.append("  FILTER (regex(?clusterLabel, \"").append(name).append("\", \"i\"))\n");
//        }

        if (name != null && !name.isEmpty())
        {
            query.append("  FILTER (LCASE(?clusterLabel) = LCASE(\"").append(name).append("\"))\n");
        }

        query.append("}\n");

        boolean doSort = sort != null && sort != SortDirection.NO_SORT;
        if (doSort)
        {
            boolean reverse = sort != SortDirection.ASCENDING;
            if (reverse)
            {
                query.append("ORDER BY DESC(?probability)\n");
            }
            else
            {
                query.append("ORDER BY ASC(?probability)\n");
            }
        }

        // do a precautionary check to help if the query will silently fail due to bad offsets
        // because getting the metadata requires a query this will cause an infinite loop if we calculate the metadata now
        // use a stale metadata if available
        AbstractDatasetData dd = (AbstractDatasetData) getDatasetData();
        int maxRows = dd.getMetadataNoSet() == null ? 0 : dd.getMetadataNoSet().getSize();
        AbstractDatasetData.handleRanges(null, dd, null, null, maxRows);

        if (range != null)
        {
            query.append("LIMIT ").append(range).append("\n");
        }

        if (rangeStart != null)
        {
            query.append("OFFSET ").append(rangeStart).append("\n");
        }

        return query.toString();
    }

    /**
     * Fetches a small preview of raw triples from the SPARQL endpoint for diagnostic purposes.
     *
     * @return  list of at most 10 subject-predicate-object triple rows
     */
    public List<Map<String, String>> previewDatasetContents()
    {
        String SPARQL_ENDPOINT = getEndpoint();
        String sparqlQuery = """
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        SELECT * WHERE {
            ?sub ?pred ?obj .
        } LIMIT 10
        """;

        List<Map<String, String>> resultList = new ArrayList<>();

        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT)
                .query(QueryFactory.create(sparqlQuery))
                .build())
        {
            ResultSet results = qexec.execSelect();

            while (results.hasNext())
            {
                QuerySolution sol = results.nextSolution();
                Map<String, String> row = new HashMap<>();

                row.put("subject", sol.contains("sub") ? sol.get("sub").toString() : "");
                row.put("predicate", sol.contains("pred") ? sol.get("pred").toString() : "");
                row.put("object", sol.contains("obj") ? sol.get("obj").toString() : "");

                resultList.add(row);
            }

            LOG.info("Previewed {} triples from dataset at endpoint: {}", resultList.size(), SPARQL_ENDPOINT);
        }
        catch (Exception e)
        {
            LOG.error("Error previewing dataset contents from endpoint: {}", SPARQL_ENDPOINT, e);
        }

        return resultList;
    }

    /**
     * Builds a SPARQL SELECT query for the heatmap graph.
     *
     * @param   sortCriteria
     *          grouping/aggregation strategy to apply
     * @param   sort
     *          sort direction to apply, or {@code null} for no sort
     * @param   range
     *          LIMIT value, or {@code null} to omit
     * @param   rangeStart
     *          OFFSET value, or {@code null} to omit
     * @param   graphURI
     *          local part of the named graph URI
     *
     * @return  SPARQL query string
     */
    public String buildHeatmapQuery(SimilaritySortCriteria sortCriteria, SortDirection sort, Integer range, Integer rangeStart, String graphURI)
    {
        SimilarityDataset sd = (SimilarityDataset) getDatasetData();
        StringBuilder query = new StringBuilder();
        boolean doSort = sort != null && sort != SortDirection.NO_SORT;

        query.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
        query.append("PREFIX ex: <http://example.org/ontology#>\n");
        query.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
        query.append("SELECT ?fromLabel ?toLabel ?similarityValue\n");
        query.append("WHERE {\n");
        query.append("  GRAPH <http://example.org/").append(graphURI).append("> {\n"); // Include the GRAPH clause
        query.append("    ?sim ex:fromObject ?from .\n");
        query.append("    ?sim ex:toObject ?to .\n");
        query.append("    ?sim ex:hasCorrelationValue ?similarityValue .\n");
        query.append("    ?from a skos:Concept ;\n");
        query.append("          rdfs:label ?fromLabel .\n");
        query.append("    ?to a skos:Concept ;\n");
        query.append("          rdfs:label ?toLabel .\n");
        query.append("  }\n");
        query.append("  FILTER(?similarityValue < 0.99)");
        query.append("}\n");

        if (sortCriteria != null)
        {
            if (sortCriteria == SimilaritySortCriteria.AVERAGE_SIMILARITY)
            {
                query.append("GROUP BY ?fromLabel ?toLabel ?similarityValue\n");
                if (doSort)
                {
                    query.append("ORDER BY ");
                    if (sort == SortDirection.DESCENDING)
                    {
                        query.append("DESC(AVG(?similarityValue))\n");
                    }
                    else if (sort == SortDirection.ASCENDING)
                    {
                        query.append("ASC(AVG(?similarityValue))\n");
                    }
                }
            }
            else if (sortCriteria == SimilaritySortCriteria.STRONGEST_PAIR)
            {
                query.append("GROUP BY ?fromLabel ?toLabel ?similarityValue\n");
                if (doSort)
                {
                    query.append("ORDER BY ");
                    if (sort == SortDirection.DESCENDING)
                    {
                        query.append("DESC(MAX(?similarityValue))\n");
                    }
                    else if (sort == SortDirection.ASCENDING)
                    {
                        query.append("ASC(MAX(?similarityValue))\n");
                    }
                }
            }
        }

        if (doSort && sortCriteria == SimilaritySortCriteria.DEFAULT)
        {
            query.append("ORDER BY ");
            if (sort == SortDirection.DESCENDING)
            {
                query.append("DESC(?similarityValue)\n");
            }
            else if (sort == SortDirection.ASCENDING)
            {
                query.append("ASC(?similarityValue)\n");
            }
        }

        // do a precautionary check to help if the query will silently fail due to bad offsets
        // because getting the metadata requires a query this will cause an infinite loop if we calculate the metadata now
        // use a stale metadata if available
        AbstractDatasetData dd = (AbstractDatasetData) getDatasetData();
        int maxRows = dd.getMetadataNoSet() == null ? 0 : dd.getMetadataNoSet().getSize();
        AbstractDatasetData.handleRanges(null, dd, null, null, maxRows);

        if (range != null)
        {
            query.append("LIMIT ").append(range).append("\n");
        }

        if (rangeStart != null)
        {
            query.append("OFFSET ").append(rangeStart).append("\n");
        }

        return query.toString();
    }

    /**
     * Executes the given SPARQL SELECT query against the configured endpoint.
     *
     * @param   sparqlQuery
     *          SPARQL SELECT query string to execute
     *
     * @return  list of variable-to-string result rows, or {@code null} on failure
     */
    public List<Map<String, String>> executeSparqlQuery(String sparqlQuery)
    {
        String SPARQL_ENDPOINT = getEndpoint();
        Query query = QueryFactory.create(sparqlQuery);
        try
        {
            try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build();)
            {
                ResultSet results = qexec.execSelect();
                return processResultSet(results);
            }
        }
        catch (Exception e)
        {
            handleException(e);
            return null;
        }
    }

    /**
     * Converts a Jena {@link ResultSet} into a list of string-valued maps.
     * Which variables are extracted depends on the active dataset type.
     *
     * @param   results
     *          Jena result set to process
     *
     * @return  list of variable-to-string result rows
     */
    public List<Map<String, String>> processResultSet(ResultSet results)
    {
        DatasetData dd = getDatasetData();
        List<Map<String, String>> resultList = new ArrayList<>();
        switch (dd.getDatasetType())
        {
            case HEATMAP ->
            {
                while (results.hasNext())
                {
                    QuerySolution solution = results.nextSolution();
                    Map<String, String> row = new HashMap<>();
                    row.put("fromLabel", solution.getLiteral("fromLabel").getString());
                    row.put("toLabel", solution.getLiteral("toLabel").getString());
                    row.put("similarityValue", solution.getLiteral("similarityValue").getString());
                    resultList.add(row);
                }
            }

            case CLUSTERS ->
            {
                while (results.hasNext())
                {
                    QuerySolution solution = results.nextSolution();
                    Map<String, String> row = new HashMap<>();
                    row.put("clusterLabel", solution.getLiteral("clusterLabel").getString());
                    row.put("probability", solution.getLiteral("probability").getString());
                    row.put("imageURI", solution.getLiteral("imageURI").getString());
                    row.put("predictedObjectLabel", solution.getLiteral("predictedObjectLabel").getString());
                    resultList.add(row);
                }
            }
        }

        return resultList;
    }

    /**
     * Returns the SPARQL endpoint URL.
     *
     * @return  SPARQL endpoint URL string
     */
    public String getEndpoint()
    {
        return endpoint;
    }

    /**
     * Returns size metadata for the dataset by counting all rows available.
     *
     * @return  dataset metadata containing the row count
     */
    public DatasetMetadata getMetadata()
    {
        DatasetData dd = getDatasetData();
        String graph = getGraphName(dd.getDatasetName(), dd.getDatasetType());
        String sparqlQuery = "";
        switch (dd.getDatasetType())
        {
            case CLUSTERS ->
            {
                sparqlQuery = buildClusterQuery(null, null, null, null, graph);
            }
            case HEATMAP ->
            {
                sparqlQuery = buildHeatmapQuery(null, null, null, null, graph);
            }
        }
        List<Map<String, String>> resultList = executeSparqlQuery(sparqlQuery);

        DatasetMetadata md = new DatasetMetadata();
        md.setSize(resultList.size());
        return md;
    }

    /**
     * Executes the request. When a pending SPARQL query is attached to the dataset it is
     * executed first - otherwise dispatches to the standard CRUD flow via the base class.
     *
     * @param   adr
     *          request to execute
     *
     * @return  dataset result or raw result set wrapper
     */
    public Object executeQuery(AbstractDatasetRequest adr)
    {
        DatasetData dd = getDatasetData();
        String query = dd.getPendingQuery();
        if (query != null)
        {
            DataEntity result = null;
            List<Map<String, String>> queryResults = executeSparqlQuery(query);

            if (!dd.isRawResults())
            {
                switch (dd.getDatasetType())
                {
                    case CLUSTERS ->
                    {
                        result = processClusterResults(executeSparqlQuery(query));
                    }
                    case HEATMAP ->
                    {
                        result = processHeatmapResults(executeSparqlQuery(query));
                    }
                }
            }
            else
            {
                RdfResultSetWrapper rrsw = new RdfResultSetWrapper();
                rrsw.setResultSet(queryResults);
                result = rrsw;
            }
            dd.setPendingQuery(null);
            return result;
        }
        return super.executeQuery(adr);
    }

    /**
     * Gets the graph name for the given dataset. In RDF format, all datasets
     * are stored in named graphs - this is mandated by the structure of the TTL files. For default 
     * datasets the graph name follows a standard pattern.
     *
     * @param   dataset
     *          dataset identifier
     * @param   datasetType
     *          dataset category
     *
     * @return  graph name string in the form {@code {dataset}_{type}}
     */
    private String getGraphName(String dataset, DatasetType datasetType)
    {
        String endPath = switch (datasetType)
        {
            case HEATMAP -> GRAPH_HEATMAP_END_PATH;
            case CLUSTERS -> GRAPH_CLUSTERS_END_PATH;
        };
        return String.format("%s_%s", dataset, endPath);
    }

    /**
     * Logs a SPARQL execution failure.
     *
     * @param   e
     *          the exception that was thrown
     */
    private void handleException(Exception e)
    {
        // I thought this was going to be more complicated...
        LOG.error("SPARQL query has failed! Message: {}", e.getMessage());
    }

    /**
     * Checks whether a cluster with the given name already exists in the list.
     *
     * @param   clusterName
     *          name to look up
     * @param   rootChildren
     *          list of existing cluster nodes to search
     *
     * @return  {@code true} when a cluster with this name is found
     */
    private boolean containsCluster(String clusterName, List<ClusterNode> rootChildren)
    {
        for (ClusterNode cn : rootChildren)
        {
            if (cn.getName().equals(clusterName))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the cluster node with the given name from the list.
     *
     * @param   clusterName
     *          name to look up
     * @param   rootChildren
     *          list of candidate cluster nodes
     *
     * @return  matching cluster node, or {@code null} when not found
     */
    private ClusterNode getCluster(String clusterName, List<ClusterNode> rootChildren)
    {
        for (ClusterNode cn : rootChildren)
        {
            if (cn.getName().equals(clusterName))
            {
                return cn;
            }
        }
        return null;
    }
    
    /**
     * Exports the dataset. When filters are honoured the read request result is serialized
     * to JSON; otherwise the raw TTL file is returned as a string.
     *
     * @param   exportRequest
     *          export configuration
     *
     * @return  exported data payload
     *
     * @throws  RuntimeException
     *          when the TTL file cannot be read
     */
    @Override
    public Object exportData(ExportRequest exportRequest)
    {
        try
        {
            DatasetData dd = getDatasetData();
            String datasetName = dd.getDatasetName();
            DatasetType datasetType = dd.getDatasetType();
            
            if (exportRequest.isHonorFilters())
            {
                // Execute read request with filters to get filtered data
                Object filteredData = executeReadRequest();
                
                // Convert filtered results to JSON (clean escaped newlines / pretty-print)
                ObjectMapper mapper = new ObjectMapper();
                return serializeExportObject(filteredData, mapper);
            }
            else
            {
                // Return raw TTL file content
                String ttlFileName = resolveTtlFilePath(datasetName, datasetType);
                File file = new File(ttlFileName);
                
                if (!file.exists())
                {
                    throw new IllegalStateException("TTL file not found: " + ttlFileName);
                }
                
                return Files.readString(file.toPath());
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed to export RDF data", e);
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resolves the TTL file path for the given dataset and dataset type.
     * Follows the convention: {@code ttl-new/{type}_data_skos_{dataset}.ttl}.
     *
     * @param   datasetName
     *          dataset identifier
     * @param   datasetType
     *          dataset category
     *
     * @return  relative path to the TTL file
     */
    private String resolveTtlFilePath(String datasetName, DatasetType datasetType)
    {
        String typePrefix = switch (datasetType)
        {
            case HEATMAP -> "heatmap";
            case CLUSTERS -> "cluster";
        };
        
        return String.format("ttl-new/%s_data_skos_%s.ttl", typePrefix, datasetName);
    }
}

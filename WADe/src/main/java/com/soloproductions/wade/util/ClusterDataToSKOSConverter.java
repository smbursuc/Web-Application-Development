package com.soloproductions.wade.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.pojo.ClusterData;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Standalone converter that reads a hierarchical cluster JSON file and writes a
 * SKOS-annotated Turtle ({@code .ttl}) file using Apache Jena.
 *
 * <p>Each cluster becomes a {@code skos:Concept}; each labelled image prediction
 * is represented as a linked {@code ex:Prediction} resource connected to the
 * cluster via {@code ex:hasPrediction}.
 *
 * <p>Run via {@link #main(String[])} — intended as a one-off data pipeline step,
 * not a Spring-managed bean.
 */
public class ClusterDataToSKOSConverter
{
    /** Base URI for all generated RDF resources. */
    private static final String BASE_URI = "http://example.org/";

    /**
     * Entry point. Reads {@code data/hierarchical_structure_uri_cifar10.json},
     * converts it to a SKOS RDF model, and writes the result to
     * {@code ttl-new/cluster_data_skos_cifar10.ttl}. The JSON file is generated
     * from a Python script found in the project.
     *
     * @param   args    
     *          unused
     */
    public static void main(String[] args)
    {
        String outputFile = "ttl-new/cluster_data_skos_cifar10.ttl";

        try
        {
            ObjectMapper mapper = new ObjectMapper();
            ClusterData root = mapper.readValue(new File("data/hierarchical_structure_uri_cifar10.json"), ClusterData.class);

            Model model = ModelFactory.createDefaultModel();
            String skosUri = "http://www.w3.org/2004/02/skos/core#";
            String exUri = BASE_URI + "ontology#";

            model.setNsPrefix("skos", skosUri);
            model.setNsPrefix("ex", exUri);
            model.setNsPrefix("xsd", XSD.getURI());

            // SKOS Vocabulary
            Property skosPrefLabel = model.createProperty(skosUri, "prefLabel");
            Property skosBroader = model.createProperty(skosUri, "broader");
            Property skosNarrower = model.createProperty(skosUri, "narrower");

            // all properties
            Property exHasPrediction = model.createProperty(exUri, "hasPrediction");
            Property exPredictedObject = model.createProperty(exUri, "predictedObject");
            Property exHasProbability = model.createProperty(exUri, "hasProbability");
            Property exHasURI = model.createProperty(exUri, "hasURI");

            // convert Clusters and Objects
            for (ClusterData.Cluster cluster : root.getChildren())
            {
                String clusterUri = BASE_URI + "Cluster/" + cluster.getName().replace(" ", "_");
                Resource clusterResource = model.createResource(clusterUri)
                                                .addProperty(RDF.type, model.createResource(skosUri + "Concept"))
                                                .addProperty(skosPrefLabel, cluster.getName());

                for (int i = 0; i < cluster.getChildren().size(); i++)
                {
                    ClusterData.Cluster.ObjectData obj = cluster.getChildren().get(i);
                    addPredictionToCluster(model, clusterResource, obj, skosPrefLabel);
                }
            }

            FileWriter writer = new FileWriter(outputFile);
            model.write(writer, "TURTLE");
            writer.close();

            System.out.println("RDF successfully written to: " + outputFile);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Creates a {@code ex:Prediction} resource for the given object and links it
     * to the cluster via {@code ex:hasPrediction}.
     *
     * @param   model           
     *          the Jena model being built
     * @param   clusterResource 
     *          the parent cluster resource
     * @param   obj             
     *          image prediction data
     * @param   skosPrefLabel   
     *          the {@code skos:prefLabel} property
     */
    private static void addPredictionToCluster(Model model, Resource clusterResource, ClusterData.Cluster.ObjectData obj, Property skosPrefLabel)
    {
        // need a unique prediction URI because otherwise the query results bundle onto each other wrongly
        String predictionUri = BASE_URI + "Prediction/" + obj.getName().replace(" ", "_") + "_" + extractImageId(obj.getUri());
        Resource predictionResource = model.createResource(predictionUri)
                .addProperty(RDF.type, model.createResource("http://www.w3.org/2004/02/skos/core#Concept"))
                .addProperty(model.createProperty(BASE_URI + "ontology#predictedObject"), createObjectResource(model, obj.getName(), obj.getUri(), skosPrefLabel))
                .addLiteral(model.createProperty(BASE_URI + "ontology#hasProbability"), obj.getProbability())
                .addProperty(model.createProperty(BASE_URI + "ontology#hasURI"), obj.getUri());

        // link the prediction to the cluster
        clusterResource.addProperty(model.createProperty(BASE_URI + "ontology#hasPrediction"), predictionResource);
    }

    /**
     * Finds or creates the {@code ex:Object} resource for a labelled image.
     *
     * @param   model           
     *          the Jena model being built
     * @param   objectName      
     *          human-readable object label
     * @param   objectUri       
     *          source image URI
     * @param   skosPrefLabel   
     *          the {@code skos:prefLabel} property
     *
     * @return  the Jena resource representing this object
     */
    private static Resource createObjectResource(Model model, String objectName, String objectUri, Property skosPrefLabel)
    {
        String objectUriId = BASE_URI + "Object/" + objectName.replace(" ", "_");
        Resource objectResource = model.createResource(objectUriId)
                .addProperty(RDFS.label, objectName)
                .addProperty(RDF.type, model.createResource("http://www.w3.org/2004/02/skos/core#Concept"))
                .addProperty(skosPrefLabel, objectName) // Add skos:prefLabel
                .addProperty(model.createProperty(BASE_URI + "ontology#hasURI"), objectUri);
        return objectResource;
    }

    /**
     * Extracts a short image identifier from a full image URI, stripping path
     * prefixes, the {@code .png} extension, and the {@code _temp_image_} infix.
     *
     * @param   uri     image URI
     *
     * @return  short identifier suitable for use in an RDF URI
     */
    private static String extractImageId(String uri)
    {
        String[] parts = uri.split("/");
        return parts[parts.length - 1].replace(".png", "").replace("_temp_image_", "");
    }
}

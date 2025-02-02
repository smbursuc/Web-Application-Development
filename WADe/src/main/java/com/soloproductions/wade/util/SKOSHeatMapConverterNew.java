package com.soloproductions.wade.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SKOSHeatMapConverterNew
{
    private static final String BASE_URI = "http://example.org/";

    public static void main(String[] args) throws IOException
    {
        String inputFilePath = "E:\\repos\\Web-Application-Development\\text-classification\\similarity_data_500.json";
        String outputFilePath = "ttl/heatmap_data_skos_cifar10.ttl";

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> inputData = mapper.readValue(new File(inputFilePath), Map.class);

        List<String> objects = (List<String>) inputData.get("objects");
        List<List<Double>> matrixList = (List<List<Double>>) inputData.get("matrix");

        // Convert matrix list to a 2D array for processing
        double[][] matrix = new double[objects.size()][objects.size()];
        for (int i = 0; i < objects.size(); i++)
        {
            for (int j = 0; j < objects.size(); j++)
            {
                matrix[i][j] = matrixList.get(i).get(j);
            }
        }

        Model model = convertHeatmapToSKOS(objects, matrix);

        try (FileWriter writer = new FileWriter(outputFilePath))
        {
            model.write(writer, "TURTLE");
        }

        System.out.println("SKOS RDF data saved to: " + outputFilePath);
    }

    private static Model convertHeatmapToSKOS(List<String> objects, double[][] matrix)
    {
        Model model = ModelFactory.createDefaultModel();

        // Namespaces
        String skosUri = "http://www.w3.org/2004/02/skos/core#";
        String exUri = BASE_URI + "ontology#";
        String xsdUri = "http://www.w3.org/2001/XMLSchema#";

        model.setNsPrefix("skos", skosUri);
        model.setNsPrefix("ex", exUri);
        model.setNsPrefix("xsd", xsdUri);

        // Create properties
        Property hasCorrelationValue = model.createProperty(exUri, "hasCorrelationValue");
        Property fromObject = model.createProperty(exUri, "fromObject");
        Property toObject = model.createProperty(exUri, "toObject");

        // Declare the range of these properties
        Resource xsdFloat = model.createResource(xsdUri + "float");
        hasCorrelationValue.addProperty(RDFS.range, xsdFloat);

        Resource skosConcept = model.createResource(skosUri + "Concept");
        fromObject.addProperty(RDFS.range, skosConcept);
        toObject.addProperty(RDFS.range, skosConcept);

        // Optionally, you could also specify domains for these properties:
        // For example, declare that hasCorrelationValue is only used on instances of a custom class, e.g. ex:SimilarityRelation.
        Resource similarityRelation = model.createResource(exUri + "SimilarityRelation");
        hasCorrelationValue.addProperty(RDFS.domain, similarityRelation);
        fromObject.addProperty(RDFS.domain, similarityRelation);
        toObject.addProperty(RDFS.domain, similarityRelation);

        // Add objects as SKOS Concepts
        for (String object : objects)
        {
            // Use a URI-safe version of the object label (you might want to apply further sanitization)
            String objectUri = BASE_URI + "Object/" + object.replaceAll("\\s+", "_");
            model.createResource(objectUri)
                    .addProperty(RDFS.label, object)
                    .addProperty(RDF.type, model.createResource(skosUri + "Concept"));
        }

        // Create correlation triples for each pair of distinct objects
        for (int i = 0; i < objects.size(); i++)
        {
            for (int j = 0; j < objects.size(); j++)
            {
                // Skip self-correlation
                if (i != j)
                {
                    Resource correlation = model.createResource(); // a blank node representing this relation
                    String fromUri = BASE_URI + "Object/" + objects.get(i).replaceAll("\\s+", "_");
                    String toUri = BASE_URI + "Object/" + objects.get(j).replaceAll("\\s+", "_");
                    correlation.addProperty(fromObject, model.createResource(fromUri));
                    correlation.addProperty(toObject, model.createResource(toUri));
                    // Add the similarity value as a typed literal (float)
                    correlation.addLiteral(hasCorrelationValue, model.createTypedLiteral(matrix[i][j]));
                }
            }
        }

        return model;
    }
}


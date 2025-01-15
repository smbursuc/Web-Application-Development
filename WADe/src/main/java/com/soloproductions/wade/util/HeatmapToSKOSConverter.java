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

public class HeatmapToSKOSConverter
{
    private static final String BASE_URI = "http://example.org/";

    public static void main(String[] args) throws IOException
    {
        String inputFilePath = "E:\\repos\\Web-Application-Development\\text-classification\\similarity_data_500.json";
        String outputFilePath = "ttl/heatmap_data_skos.ttl";

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> inputData = mapper.readValue(new File(inputFilePath), Map.class);

        List<String> objects = (List<String>) inputData.get("objects");
        List<List<Double>> matrixList = (List<List<Double>>) inputData.get("matrix");

        // convert matrix list to array for processing
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

        // namespaces
        String skosUri = "http://www.w3.org/2004/02/skos/core#";
        String exUri = BASE_URI + "ontology#";
        model.setNsPrefix("skos", skosUri);
        model.setNsPrefix("ex", exUri);

        Property hasCorrelationValue = model.createProperty(exUri, "hasCorrelationValue");
        Property fromObject = model.createProperty(exUri, "fromObject");
        Property toObject = model.createProperty(exUri, "toObject");

        // add objects as SKOS Concepts
        for (String object : objects)
        {
            String objectUri = BASE_URI + "Object/" + object.replace(" ", "_");
            model.createResource(objectUri)
                    .addProperty(RDFS.label, object)
                    .addProperty(RDF.type, model.createResource(skosUri + "Concept"));
        }

        // correlations
        for (int i = 0; i < objects.size(); i++)
        {
            for (int j = 0; j < objects.size(); j++)
            {
                // skip self-correlation
                if (i != j)
                {
                    Resource correlation = model.createResource();
                    correlation.addProperty(fromObject, model.createResource(BASE_URI + "Object/" + objects.get(i).replace(" ", "_")));
                    correlation.addProperty(toObject, model.createResource(BASE_URI + "Object/" + objects.get(j).replace(" ", "_")));
                    correlation.addLiteral(hasCorrelationValue, matrix[i][j]);
                }
            }
        }

        return model;
    }
}


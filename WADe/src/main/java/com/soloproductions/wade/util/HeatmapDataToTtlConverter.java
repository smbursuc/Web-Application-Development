package com.soloproductions.wade.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.pojo.HeatmapData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class HeatmapDataToTtlConverter
{

    public static void main(String[] args)
    {
        String baseUri = "http://example.org/";
        String outputPath = "ttl/heatmap_data.ttl";

        try
        {
            ObjectMapper mapper = new ObjectMapper();
            String filepath = "E:\\repos\\Web-Application-Development\\text-classification\\similarity_data_500.json";
            HeatmapData heatmapData = mapper.readValue(new File(filepath), HeatmapData.class);

            StringBuilder ttlBuilder = new StringBuilder();
            ttlBuilder.append("@prefix ex: <").append(baseUri).append("ontology#> .\n");
            ttlBuilder.append("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n");

            for (String object : heatmapData.getObjects())
            {
                String objectUri = baseUri + "Object/" + object.replace(" ", "_");
                ttlBuilder.append("<").append(objectUri).append("> a ex:Object ;\n")
                        .append("    ex:hasName \"").append(object).append("\"^^xsd:string .\n\n");
            }

            String[] objects = heatmapData.getObjects();
            double[][] matrix = heatmapData.getMatrix();
            for (int i = 0; i < objects.length; i++)
            {
                for (int j = 0; j < objects.length; j++)
                {
                    // avoid self-similarity
                    if (i != j)
                    {
                        String objectUri1 = baseUri + "Object/" + objects[i].replace(" ", "_");
                        String objectUri2 = baseUri + "Object/" + objects[j].replace(" ", "_");

                        ttlBuilder.append("<").append(objectUri1).append("> ex:hasSimilarity [\n")
                                .append("    ex:relatedTo <").append(objectUri2).append("> ;\n")
                                .append("    ex:similarityValue \"").append(matrix[i][j]).append("\"^^xsd:decimal\n")
                                .append("] .\n\n");
                    }
                }
            }

            Files.createDirectories(Paths.get("ttl")); // Ensure directory exists
            Files.write(Paths.get(outputPath), ttlBuilder.toString().getBytes());
            System.out.println("TTL file saved at: " + outputPath);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

package com.soloproductions.wade.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.pojo.ClusterData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClusterDataToTtlConverter
{

    public static void main(String[] args)
    {
        String baseUri = "http://example.org/";

        try
        {
            ObjectMapper mapper = new ObjectMapper();
            ClusterData root = mapper.readValue(new File("data/hierarchical_structure_uri.json"), ClusterData.class);

            StringBuilder ttlBuilder = new StringBuilder();

            ttlBuilder.append("@prefix ex: <").append(baseUri).append("ontology#> .\n");
            ttlBuilder.append("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n");

            for (ClusterData.Cluster cluster : root.getChildren())
            {
                String clusterUri = baseUri + cluster.getName().replace(" ", "_");

                ttlBuilder.append("<").append(clusterUri).append("> a ex:Cluster ;\n");

                // add children
                for (int i = 0; i < cluster.getChildren().size(); i++)
                {
                    ClusterData.Cluster.ObjectData obj = cluster.getChildren().get(i);
                    String objectUri = baseUri + "Object/" + obj.getName().replace(" ", "_").replace(",", "_");

                    ttlBuilder.append("    ex:hasChild <").append(objectUri).append(">");

                    // add a semicolon for all but the last child, else a period
                    if (i < cluster.getChildren().size() - 1)
                    {
                        ttlBuilder.append(" ;\n");
                    }
                    else
                    {
                        ttlBuilder.append(" .\n\n");
                    }
                }

                // add details for each object
                for (ClusterData.Cluster.ObjectData obj : cluster.getChildren())
                {
                    String objectUri = baseUri + "Object/" + obj.getName().replace(" ", "_").replace(",", "_");

                    ttlBuilder.append("<").append(objectUri).append("> a ex:Object ;\n")
                            .append("    ex:hasName \"").append(obj.getName()).append("\"^^xsd:string ;\n")
                            .append("    ex:hasProbability \"").append(obj.getProbability()).append("\"^^xsd:decimal ;\n")
                            .append("    ex:hasURI \"").append(obj.getUri()).append("\"^^xsd:anyURI .\n\n");
                }
            }

            String outputPath = "ttl/cluster_data.ttl";
            Files.createDirectories(Paths.get("ttl")); // paths.get used for ensuring directory exists
            Files.write(Paths.get(outputPath), ttlBuilder.toString().getBytes());

            System.out.println("TTL file saved at: " + outputPath);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}



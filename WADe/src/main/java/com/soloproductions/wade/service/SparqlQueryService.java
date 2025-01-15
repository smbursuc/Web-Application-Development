package com.soloproductions.wade.service;

import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SparqlQueryService
{

    private static final String SPARQL_ENDPOINT = "http://127.0.0.1:3030/impr_skos";

    public String buildClusterQuery(String name, String sort, int range, int rangeStart)
    {
        StringBuilder query = new StringBuilder();

        query.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
        query.append("PREFIX ex: <http://example.org/ontology#>\n");
        query.append("SELECT ?clusterName ?predictionURI ?probability ?imageURI ?objectName\n");
        query.append("WHERE {\n");
        query.append("  ?cluster a skos:Concept ;\n");
        query.append("           skos:prefLabel ?clusterName ;\n");
        query.append("           ex:hasPrediction ?prediction .\n");
        query.append("  ?prediction ex:hasProbability ?probability ;\n");
        query.append("              ex:hasURI ?imageURI ;\n");
        query.append("              ex:predictedObject ?object .\n");
        query.append("  ?object skos:prefLabel ?objectName .\n");

        if (name != null)
        {
            query.append("  FILTER (regex(?clusterName, \"").append(name).append("\", \"i\"))\n");
        }

        query.append("}\n");

        // Add sorting
        if (sort.equalsIgnoreCase("highest_probability"))
        {
            query.append("ORDER BY DESC(?probability)\n");
        }
        else if (sort.equalsIgnoreCase("lowest_probability"))
        {
            query.append("ORDER BY ASC(?probability)\n");
        }

        // Add range and offset
        query.append("LIMIT ").append(range).append("\n");
        query.append("OFFSET ").append(rangeStart).append("\n");

        return query.toString();
    }

    public List<Map<String, String>> executeClusterSparqlQuery(String sparqlQuery)
    {
        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build();)
        {
            ResultSet results = qexec.execSelect();

            List<Map<String, String>> resultList = new ArrayList<>();
            while (results.hasNext())
            {
                QuerySolution solution = results.nextSolution();
                Map<String, String> row = new HashMap<>();
                row.put("clusterName", solution.getLiteral("clusterName").getString());
//                row.put("predictionURI", solution.getResource("predictionURI").getURI());
                row.put("probability", solution.getLiteral("probability").getString());
                row.put("imageURI", solution.getLiteral("imageURI").getString());
                row.put("objectName", solution.getLiteral("objectName").getString());
                resultList.add(row);
            }
            return resultList;
        }
    }

    public String buildHeatmapQuery(String sortType, String sort, int range, int rangeStart)
    {
        StringBuilder query = new StringBuilder();

        query.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
        query.append("PREFIX ex: <http://example.org/ontology#>\n");
        query.append("SELECT DISTINCT ?fromObject ?toObject ?similarityValue\n");
        query.append("WHERE {\n");
        query.append("  {\n");
        query.append("    # Retrieve explicit correlations\n");
        query.append("    ?correlation ex:fromObject ?fromObject ;\n");
        query.append("                 ex:toObject ?toObject ;\n");
        query.append("                 ex:hasCorrelationValue ?similarityValue .\n");
        query.append("  }\n");
        query.append("  UNION\n");
        query.append("  {\n");
        query.append("    # Add self-relations with similarityValue = 1.0\n");
        query.append("    ?fromObject a skos:Concept .\n");
        query.append("    BIND(?fromObject AS ?toObject)\n");
        query.append("    BIND(1.0 AS ?similarityValue)\n");
        query.append("  }\n");
        query.append("}\n");

        // sort
        if ("average_similarity".equalsIgnoreCase(sortType))
        {
            query.append("GROUP BY ?fromObject\n");
            query.append("HAVING (COUNT(?similarityValue) > 0)\n");
            query.append("ORDER BY ");
            if ("highest".equalsIgnoreCase(sort))
            {
                query.append("DESC(AVG(?similarityValue))\n");
            }
            else if ("lowest".equalsIgnoreCase(sort))
            {
                query.append("ASC(AVG(?similarityValue))\n");
            }
        }
        else if ("strongest_pair".equalsIgnoreCase(sortType))
        {
            query.append("GROUP BY ?fromObject\n");
            query.append("HAVING (COUNT(?similarityValue) > 0)\n");
            query.append("ORDER BY ");
            if ("highest".equalsIgnoreCase(sort))
            {
                query.append("DESC(MAX(?similarityValue))\n");
            }
            else if ("lowest".equalsIgnoreCase(sort))
            {
                query.append("ASC(MAX(?similarityValue))\n");
            }
        }

        query.append("LIMIT ").append(range).append("\n");
        query.append("OFFSET ").append(rangeStart).append("\n");

        return query.toString();
    }

    public List<Map<String, Object>> executeHeatmapSparqlQuery(String sparqlQuery)
    {
        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build();)
        {
            ResultSet results = qexec.execSelect();

            List<Map<String, Object>> resultList = new ArrayList<>();
            while (results.hasNext())
            {
                QuerySolution solution = results.nextSolution();
                Map<String, Object> row = new HashMap<>();
                row.put("fromObject", solution.getResource("fromObject").getURI());
                row.put("toObject", solution.getResource("toObject").getURI());
                row.put("similarityValue", solution.getLiteral("similarityValue").getDouble());
                resultList.add(row);
            }
            return resultList;
        }
    }

    public List<String> getAllHeatmapObjectsFromRDF() throws Exception
    {
        String queryStr = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "SELECT DISTINCT ?object\n" +
                "WHERE {\n" +
                "  ?object a skos:Concept .\n" +
                "}" +
                "LIMIT 500";

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build();)
        {
            ResultSet results = qexec.execSelect();

            List<String> objects = new ArrayList<>();
            while (results.hasNext())
            {
                QuerySolution solution = results.nextSolution();
                objects.add(solution.getResource("object").getURI());
            }

            if (objects.isEmpty())
            {
                throw new Exception("No objects found in the RDF store.");
            }

            return objects;
        }
    }


}

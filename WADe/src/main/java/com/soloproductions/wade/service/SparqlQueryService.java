package com.soloproductions.wade.service;

import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SparqlQueryService
{

    private static final String SPARQL_ENDPOINT = "http://127.0.0.1:3030/impr";

    public String buildClusterQuery(String name, String sort, int range, int rangeStart, String graphURI) {
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

        if (name != null && !name.isEmpty()) {
            query.append("  FILTER (regex(?clusterLabel, \"").append(name).append("\", \"i\"))\n");
        }

        query.append("}\n");

        // Add sorting logic if provided
        if (sort != null) {
            if ("highest_probability".equalsIgnoreCase(sort)) {
                query.append("ORDER BY DESC(?probability)\n");
            } else if ("lowest_probability".equalsIgnoreCase(sort)) {
                query.append("ORDER BY ASC(?probability)\n");
            }
        }

        // Add pagination settings
        if (range != 0)
        {
            query.append("LIMIT ").append(range).append("\n");
        }
        if (rangeStart != 0)
        {
            query.append("OFFSET ").append(rangeStart).append("\n");
        }

        return query.toString();
    }

    public List<Map<String, String>> executeClusterSparqlQuery(String sparqlQuery) {
        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build();) {
            ResultSet results = qexec.execSelect();

            List<Map<String, String>> resultList = new ArrayList<>();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Map<String, String> row = new HashMap<>();
                row.put("clusterLabel", solution.getLiteral("clusterLabel").getString());
                row.put("probability", solution.getLiteral("probability").getString());
                row.put("imageURI", solution.getLiteral("imageURI").getString());
                row.put("predictedObjectLabel", solution.getLiteral("predictedObjectLabel").getString());
                resultList.add(row);
            }
            return resultList;
        }
    }

    public String buildHeatmapQueryOld(String sortType, String sort, int range, int rangeStart, String graph)
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

        if (range != 0)
        {
            query.append("LIMIT ").append(range).append("\n");
        }
        if (rangeStart != 0)
        {
            query.append("OFFSET ").append(rangeStart).append("\n");
        }

        return query.toString();
    }

    public String buildHeatmapQuery(String sortType, String sort, int range, int rangeStart, String graphURI) {
        StringBuilder query = new StringBuilder();

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

        // Append sorting logic based on the sortType and sort direction
        if ("average_similarity".equalsIgnoreCase(sortType)) {
            query.append("GROUP BY ?fromLabel ?toLabel ?similarityValue\n");
            query.append("ORDER BY ");
            if ("highest_probability".equalsIgnoreCase(sort)) {
                query.append("DESC(AVG(?similarityValue))\n");
            } else if ("lowest_probability".equalsIgnoreCase(sort)) {
                query.append("ASC(AVG(?similarityValue))\n");
            }
        } else if ("strongest_pair".equalsIgnoreCase(sortType)) {
            query.append("GROUP BY ?fromLabel ?toLabel ?similarityValue\n");
            query.append("ORDER BY ");
            if ("highest_probability".equalsIgnoreCase(sort)) {
                query.append("DESC(MAX(?similarityValue))\n");
            } else if ("lowest_probability".equalsIgnoreCase(sort)) {
                query.append("ASC(MAX(?similarityValue))\n");
            }
        }

        if (range != 0)
        {
            query.append("LIMIT ").append(range).append("\n");
        }
        if (rangeStart != 0)
        {
            query.append("OFFSET ").append(rangeStart).append("\n");
        }

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
                row.put("fromLabel", solution.getLiteral("fromLabel").getString());
                row.put("toLabel", solution.getLiteral("toLabel").getString());
                row.put("similarityValue", solution.getLiteral("similarityValue").getDouble());
                resultList.add(row);
            }
            return resultList;
        }
    }

}

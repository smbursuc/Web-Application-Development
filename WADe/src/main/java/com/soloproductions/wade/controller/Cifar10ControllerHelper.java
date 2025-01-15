package com.soloproductions.wade.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dto.ClusterFetchRequest;

public class Cifar10ControllerHelper
{
    public static Map<String, List<Object>> getClustersForPlotly(String url, ClusterFetchRequest cfDTO)
    {
        String queryParams = toQueryParams(cfDTO);
        String fullUrl = url + "?" + queryParams;

        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> rootNode = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> clusters = (List<Map<String, Object>>) rootNode.get("children");

            // Step 3: Initialize lists to store labels, parents, and values
            List<String> labels = new ArrayList<>();
            List<String> parents = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            List<String> uris = new ArrayList<>();

            // Step 4: Sort clusters by their average probability
//            clusters.sort((a, b) -> Double.compare(
//                    calculateAverageProbability((List<Map<String, Object>>) b.get("children")),
//                    calculateAverageProbability((List<Map<String, Object>>) a.get("children"))
//            ));

            // Step 5: Process each cluster and its children
            for (Map<String, Object> cluster : clusters)
            {
//                String name = (String) cluster.get("name");
//                if (clusterName != null && !name.isEmpty() && !name.equals(clusterName))
//                {
//                    continue;
//                }
                processCluster(cluster, "Root", labels, parents, values, uris);
            }

            // Step 6: Return the lists in a structured format
            Map<String, List<Object>> result = new HashMap<>();
            result.put("labels", new ArrayList<>(labels));
            result.put("parents", new ArrayList<>(parents));
            result.put("values", new ArrayList<>(values));
            result.put("uris", new ArrayList<>(uris));

            return result;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch or process clusters for Plotly");
        }
    }

    public static Map<String, List<Object>> transformClusterResultsForPlotly(List<Map<String, String>> queryResults) {
        List<Object> labels = new ArrayList<>();
        List<Object> parents = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<Object> uris = new ArrayList<>();

        for (Map<String, String> result : queryResults) {
            String clusterName = result.get("clusterName");
            String objectName = result.get("objectName");
            String probability = result.get("probability");
            String imageURI = result.get("imageURI");

            // add cluster (if not already added)
            if (!labels.contains(clusterName)) {
                labels.add(clusterName);
                parents.add("Root"); // clusters have no parent
            }

            labels.add(objectName);
            parents.add(clusterName); // the parent of the object is its cluster
            values.add(Double.valueOf(probability));
            uris.add(imageURI);
        }

        Map<String, List<Object>> plotlyData = new HashMap<>();
        plotlyData.put("labels", labels);
        plotlyData.put("parents", parents);
        plotlyData.put("values", values);
        plotlyData.put("uris", uris);

        return plotlyData;
    }

    public static Map<String, Object> transformHeatmapResultsForPlotly(List<Map<String, Object>> sparqlResults) {
        Set<String> uniqueObjects = new HashSet<>();
        for (Map<String, Object> row : sparqlResults) {
            uniqueObjects.add((String) row.get("fromObject"));
            uniqueObjects.add((String) row.get("toObject"));
        }

        // plotly format requires a list with labels, and a matrix for each label and its relation with the other
        // objects based on index
        List<String> objects = new ArrayList<>(uniqueObjects);
        double[][] matrix = new double[objects.size()][objects.size()];

        // fill with values
        for (Map<String, Object> row : sparqlResults) {
            String fromObject = (String) row.get("fromObject");
            String toObject = (String) row.get("toObject");
            double similarityValue = (double) row.get("similarityValue");

            // get the indices for the matrix
            int fromIndex = objects.indexOf(fromObject);
            int toIndex = objects.indexOf(toObject);

            if (fromIndex != -1 && toIndex != -1) {
                matrix[fromIndex][toIndex] = similarityValue;
            }
        }

        // expecting a JSON with two keys and the aforementioned arrays
        Map<String, Object> response = new HashMap<>();
        response.put("objects", objects);
        response.put("matrix", matrix);

        return response;
    }

    private static void processCluster(
            Map<String, Object> cluster,
            String parent,
            List<String> labels,
            List<String> parents,
            List<Double> values,
            List<String> uris
    )
    {
        String clusterName = (String) cluster.get("name");
        labels.add(clusterName);
        parents.add(parent);

        // for each cluster the average probability of it is calculated
        List<Map<String, Object>> children = (List<Map<String, Object>>) cluster.get("children");
        double avgProbability = calculateAverageProbability(children);
        values.add(avgProbability); // add it
        uris.add((String) cluster.get("URI"));

        // and add all children of the cluster
        if (children != null)
        {
            for (Map<String, Object> child : children)
            {
                String childName = (String) child.get("name");
                String URI = (String) child.get("URI");
                labels.add(childName);
                parents.add(clusterName);
                values.add((Double) child.getOrDefault("Probability", 0.0)); // Use the object's probability
                uris.add(URI);
            }
        }
    }

    private static double calculateAverageProbability(List<Map<String, Object>> children)
    {
        if (children == null || children.isEmpty())
        {
            return 0.0;
        }
        return children.stream()
                .mapToDouble(child -> (Double) child.getOrDefault("Probability", 0.0))
                .average()
                .orElse(0.0);
    }

    private static String toQueryParams(Object dto)
    {
        // since some query params are not mandatory dynamically parse them
        return Arrays.stream(dto.getClass().getDeclaredFields())
                .filter(field ->
                {
                    field.setAccessible(true);
                    try
                    {
                        return field.get(dto) != null; // include only non-null fields
                    }
                    catch (IllegalAccessException e)
                    {
                        return false;
                    }
                })
                .map(field ->
                {
                    try
                    {
                        String key = URLEncoder.encode(field.getName(), StandardCharsets.UTF_8);
                        String value = URLEncoder.encode(field.get(dto).toString(), StandardCharsets.UTF_8);
                        return key + "=" + value;
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException("Failed to encode query parameter", e);
                    }
                })
                .collect(Collectors.joining("&"));
    }
}


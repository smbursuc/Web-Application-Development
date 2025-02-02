package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dto.SemanticFetchRequest;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.service.SparqlQueryService;
import jakarta.servlet.http.Part;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@RestController
@RequestMapping("/api/{dataset}")
public class DatasetController
{
    private final SparqlQueryService sparqlQueryService;

    private final String URL = "http://127.0.0.1:3030/impr";

    public DatasetController(SparqlQueryService sparqlQueryService)
    {
        this.sparqlQueryService = sparqlQueryService;
    }

    @GetMapping("/clusters/json")
    public ResponseEntity<StandardResponse<?>> getClusters(
            @PathVariable String dataset,
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    )
    {
        // this is the servlet under the hood in Flask, JSON data is retrieved from here
        String url = String.format("http://127.0.0.1:5000/api/clusters/%s", dataset);

        // create expected data for plotly, a JSON with 3 arrays: values, parents, labels
        Map<String, List<Object>> plotlyData = SemanticRepresentationHelper.getClustersForPlotlyJSON(url, sfDTO);

        StandardResponse<Map<String, List<Object>>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/clusters/rdf")
    public ResponseEntity<StandardResponse<?>> getClustersRDF(
            @PathVariable String dataset,
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    )
    {
        String name = sfDTO.getName();
        String sort = sfDTO.getSort();
        int range = sfDTO.getRange();
        int rangeStart = sfDTO.getRangeStart();

        String graph = String.format("%s_clusters", dataset);

        // string construction of the query
        String sparqlQuery = sparqlQueryService.buildClusterQuery(name, sort, range, rangeStart, graph);

        List<Map<String, String>> queryResults = sparqlQueryService.executeClusterSparqlQuery(sparqlQuery);

        // comply with plotly format
        Map<String, List<Object>> plotlyData = SemanticRepresentationHelper.getClustersForPlotlyRDF(queryResults);

        StandardResponse<Map<String, List<Object>>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/heatmaps/json")
    public ResponseEntity<StandardResponse<?>> getJSONHeatmap(
            @PathVariable String dataset,
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    )
    {
        // this is the servlet under the hood in Flask, JSON data is retrieved from here
        String url = String.format("http://127.0.0.1:5000/api/correlations/%s", dataset);

        // create expected data for plotly, a JSON with 3 arrays: values, parents, labels
        Map<String, List<Object>> plotlyData = SemanticRepresentationHelper.getHeatmapJSON(url, sfDTO);

        StandardResponse<Map<String, List<Object>>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/heatmaps/rdf")
    public ResponseEntity<StandardResponse<?>> getRDFHeatmap(
            @PathVariable String dataset,
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    ) throws Exception
    {
        String sortType = sfDTO.getSortType();
        String sort = sfDTO.getSort();
        int range = sfDTO.getRange();
        int rangeStart = sfDTO.getRangeStart();

        String graph = String.format("%s_heatmap", dataset);

        // string construction of the query
        String sparqlQuery = sparqlQueryService.buildHeatmapQuery(sortType, sort, range, rangeStart, graph);

        List<Map<String, Object>> queryResults = sparqlQueryService.executeHeatmapSparqlQuery(sparqlQuery);

        // comply with the plotly structure
        Map<String, Object> plotlyData = SemanticRepresentationHelper.transformHeatmapResultsForPlotly(queryResults);

        StandardResponse<Map<String, Object>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/metadata/{dataModel}/{representation}")
    public ResponseEntity<StandardResponse<?>> getMetadata(
            @PathVariable String dataModel,
            @PathVariable String dataset,
            @PathVariable String representation
    ) throws IOException, InterruptedException
    {
        Map<String, Object> metadata = new HashMap<>();
        if (dataModel.equalsIgnoreCase("rdf"))
        {
            String graph = String.format("%s_%s", dataset, representation);
            String sparqlQuery = "";
            int rows = 0;
            if (representation.equalsIgnoreCase("clusters"))
            {
                sparqlQuery = sparqlQueryService.buildClusterQuery(null, null, 0, 0, graph);
                List<Map<String, String>> queryResults = sparqlQueryService.executeClusterSparqlQuery(sparqlQuery);
                rows = queryResults.size();
            }
            else if (representation.equalsIgnoreCase("heatmaps"))
            {
                sparqlQuery = sparqlQueryService.buildHeatmapQuery(null, null, 0, 0, graph);
                List<Map<String, Object>> queryResults = sparqlQueryService.executeHeatmapSparqlQuery(sparqlQuery);
                rows = queryResults.size();
            }
            metadata.put("size", rows);
        }
        else if (dataModel.equalsIgnoreCase("json"))
        {
            String fullUrl = String.format("http://127.0.0.1:5000/api/metadata/%s/%s", dataset, representation);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            ObjectMapper objectMapper = new ObjectMapper();
            metadata = objectMapper.readValue(body, Map.class);
        }

        StandardResponse<Map<String, Object>> response = new StandardResponse<>(
                "success",
                "Metadata retrieved successfully",
                metadata
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

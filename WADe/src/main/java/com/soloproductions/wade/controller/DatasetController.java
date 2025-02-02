package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dto.SemanticFetchRequest;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.service.SparqlQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(summary = "Retrieve JSON Clusters",
            description = "Fetches cluster data in JSON format for a specified dataset, formatted for visualization with Plotly. This includes arrays of values, parents, and labels.",
            tags = {"Clusters"})
    @ApiResponse(responseCode = "200",
            description = "JSON cluster data retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StandardResponse.class),
                    examples = @ExampleObject(name = "SuccessExample",
                            summary = "Example of a successful JSON cluster retrieval",
                            value = "{\"status\": \"success\", \"message\": \"Clusters retrieved successfully\", \"data\": {\"values\": [\"Value1\", \"Value2\"], \"parents\": [\"Parent1\", \"Parent2\"], \"labels\": [\"Label1\", \"Label2\"]}}")))
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<StandardResponse<?>> getClusters(
            @Parameter(description = "The dataset name for which to retrieve the cluster data", required = true)
            @PathVariable String dataset,

            @Parameter(description = "Semantic fetch request containing additional parameters for data retrieval")
            @ModelAttribute SemanticFetchRequest sfDTO,

            HttpServletRequest request)
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
    @Operation(summary = "Retrieve RDF Clusters",
            description = "Fetches RDF cluster data for a specified dataset, formatted for visualization with Plotly.",
            tags = {"Clusters"})
    @ApiResponse(responseCode = "200",
            description = "RDF clusters retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StandardResponse.class),
                    examples = @ExampleObject(name = "SuccessExample",
                            summary = "Example of a successful response",
                            value = "{\"status\": \"success\", \"message\": \"Clusters retrieved successfully\", \"data\": {\"values\": [\"Node1\", \"Node2\"], \"parents\": [\"Parent1\", \"Parent2\"], \"labels\": [\"Label1\", \"Label2\"]}}")))
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<StandardResponse<?>> getClustersRDF(
            @Parameter(description = "The dataset name for which to retrieve the cluster data", required = true)
            @PathVariable String dataset,

            @Parameter(description = "Semantic fetch request containing filtering and sorting parameters")
            @ModelAttribute SemanticFetchRequest sfDTO,

            HttpServletRequest request)
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
    @Operation(summary = "Retrieve JSON Heatmap",
            description = "Fetches JSON formatted heatmap data for a specified dataset. The data includes arrays of values, parents, and labels suitable for visualization with Plotly.",
            tags = {"Heatmap"})
    @ApiResponse(responseCode = "200",
            description = "JSON heatmap data retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StandardResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<StandardResponse<?>> getJSONHeatmap(
            @Parameter(description = "The dataset name for which to retrieve the heatmap data", required = true)
            @PathVariable String dataset,

            @Parameter(description = "Semantic fetch request containing additional parameters for data retrieval")
            @ModelAttribute SemanticFetchRequest sfDTO,

            HttpServletRequest request)
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
    @Operation(summary = "Retrieve RDF Heatmap Data",
            description = "Fetches RDF heatmap data for a specified dataset based on sort and range criteria.",
            tags = {"Heatmap"})
    @ApiResponse(responseCode = "200",
            description = "Heatmap data retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StandardResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<StandardResponse<?>> getRDFHeatmap(
            @Parameter(description = "Dataset name for which to retrieve the heatmap data", required = true)
            @PathVariable String dataset,
            @Parameter(description = "Semantic fetch request containing sorting and range parameters", required = true)
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
    @Operation(summary = "Retrieve Metadata",
            description = "Fetches metadata for a specified data model and representation.",
            tags = {"Metadata"})
    @ApiResponse(responseCode = "200",
            description = "Metadata retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StandardResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<StandardResponse<?>> getMetadata(
            @Parameter(description = "The data model from which to retrieve metadata (e.g., 'rdf' or 'json')", required = true)
            @PathVariable String dataModel,

            @Parameter(description = "The dataset name relevant to the data model", required = true)
            @PathVariable String dataset,

            @Parameter(description = "The representation type of the data model (e.g., 'clusters' or 'heatmaps')", required = true)
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

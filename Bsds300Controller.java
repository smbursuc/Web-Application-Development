package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.SemanticFetchRequest;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.service.SparqlQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@RestController
@RequestMapping("/api/bsds300")
public class Bsds300Controller
{
    private final SparqlQueryService sparqlQueryService;
    
    private final String URL = "http://127.0.0.1:3030/impr";

    public Bsds300Controller(SparqlQueryService sparqlQueryService)
    {
        this.sparqlQueryService = sparqlQueryService;
    }

    @GetMapping("/clusters/json")
    public ResponseEntity<StandardResponse<?>> getClusters(
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    )
    {
        String url = "http://127.0.0.1:5000/api/clusters/bsds300";

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
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    )
    {
        String name = sfDTO.getName();
        String sort = sfDTO.getSort();
        int range = sfDTO.getRange();
        int rangeStart = sfDTO.getRangeStart();

        String graph = "bsds300_clusters";

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
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    )
    {
        // create expected data for plotly, a JSON with 3 arrays: values, parents, labels
        Map<String, List<Object>> plotlyData = Map.of();

        StandardResponse<Map<String, List<Object>>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/heatmaps/rdf")
    public ResponseEntity<StandardResponse<?>> getRDFHeatmap(
            @ModelAttribute SemanticFetchRequest sfDTO,
            HttpServletRequest request
    ) throws Exception
    {
        String name = sfDTO.getName();
        String sort = sfDTO.getSort();
        int range = sfDTO.getRange();
        int rangeStart = sfDTO.getRangeStart();

        String graph = "bsds300_heatmap";

        // string construction of the query
        String sparqlQuery = sparqlQueryService.buildHeatmapQuery(name, sort, range, rangeStart, graph);
        
        List<Map<String, Object>> queryResults = sparqlQueryService.executeHeatmapSparqlQuery(sparqlQuery);

        // for debug this gets all objects from the heatmap
//        List<String> objects = sparqlQueryService.getAllHeatmapObjectsFromRDF();

        // comply with the plotly structure
        Map<String, Object> plotlyData = SemanticRepresentationHelper.transformHeatmapResultsForPlotly(queryResults);

        StandardResponse<Map<String, Object>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

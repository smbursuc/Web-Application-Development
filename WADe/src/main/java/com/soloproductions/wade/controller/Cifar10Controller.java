package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.ClusterFetchRequest;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.service.SparqlQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/cifar10")
public class Cifar10Controller
{
    private SparqlQueryService sparqlQueryService;

    public Cifar10Controller(SparqlQueryService sparqlQueryService)
    {
        this.sparqlQueryService = sparqlQueryService;
    }

    @GetMapping("/clusters/json")
    public ResponseEntity<StandardResponse<?>> getClusters(
            @ModelAttribute ClusterFetchRequest cfDTO,
            HttpServletRequest request
    )
    {
        String url = "http://127.0.0.1:5000/api/clusters/cifar10";

        // create expected data for plotly, a JSON with 3 arrays: values, parents, labels
        Map<String, List<Object>> plotlyData = Cifar10ControllerHelper.getClustersForPlotly(url, cfDTO);

        StandardResponse<Map<String, List<Object>>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/clusters/rdf")
    public ResponseEntity<StandardResponse<?>> getClustersRDF(
            @ModelAttribute ClusterFetchRequest cfDTO,
            HttpServletRequest request
    )
    {
        String name = cfDTO.getName();
        String sort = cfDTO.getSort();
        int range = cfDTO.getRange();
        int rangeStart = cfDTO.getRangeStart();

        // string construction of the query
        String sparqlQuery = sparqlQueryService.buildClusterQuery(name, sort, range, rangeStart);

        List<Map<String, String>> queryResults = sparqlQueryService.executeClusterSparqlQuery(sparqlQuery);

        // comply with plotly format
        Map<String, List<Object>> plotlyData = Cifar10ControllerHelper.transformClusterResultsForPlotly(queryResults);

        StandardResponse<Map<String, List<Object>>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/heatmaps/rdf")
    public ResponseEntity<StandardResponse<?>> getRDFHeatmap(
            @ModelAttribute ClusterFetchRequest cfDTO,
            HttpServletRequest request
    ) throws Exception
    {
        String name = cfDTO.getName();
        String sort = cfDTO.getSort();
        int range = cfDTO.getRange();
        int rangeStart = cfDTO.getRangeStart();

        // string construction of the query
        String sparqlQuery = sparqlQueryService.buildHeatmapQuery(name, sort, range, rangeStart);

        List<Map<String, Object>> queryResults = sparqlQueryService.executeHeatmapSparqlQuery(sparqlQuery);

        // for debug this gets all objects from the heatmap
//        List<String> objects = sparqlQueryService.getAllHeatmapObjectsFromRDF();

        // comply with the plotly structure
        Map<String, Object> plotlyData = Cifar10ControllerHelper.transformHeatmapResultsForPlotly(queryResults);

        StandardResponse<Map<String, Object>> response = new StandardResponse<>(
                "success",
                "Clusters retrieved successfully",
                plotlyData
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}

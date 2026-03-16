package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.*;
import com.soloproductions.wade.service.DatasetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The list of all the endpoints related to the dataset API.
 */
@RestController
@RequestMapping("/api")
public class DatasetController
{
    /** The dataset service used to handle dataset-related requests. */
    private final DatasetService datasetService;

    /**
     * Constructor for DatasetController. Initializes the dataset service.
     *
     * @param datasetService the dataset service to be used in the controller
     */
    public DatasetController(DatasetService datasetService)
    {
        this.datasetService = datasetService;
    }

    /**
     * Endpoint to retrieve dataset data based on the specified parameters. The particular endpoint is serving
     * GET requests aimed specifically for cluster datasets, and uses its own special request {@link ClusterDatasetRequest}.
     * 
     * @param   cdr       
     *          the cluster dataset request containing the parameters for the request
     * @param   request   
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset data
     */
    @GetMapping(value = "/{datasetName}/{datasetType}/{dataType}", params = {"mode=cluster", "export!=true"})
    public ResponseEntity<StandardResponse<?>> getData(
            @ModelAttribute ClusterDatasetRequest cdr,
            HttpServletRequest request)
    {
        cdr.setRequestType("get");
        StandardResponse<Object> response = datasetService.handleRequest(cdr);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve dataset data based on the specified parameters. The particular endpoint is serving
     * GET requests aimed specifically for heatmap datasets, and uses its own special request {@link SimilarityDatasetRequest}.
     * 
     * @param   sdr       
     *          the similarity dataset request containing the parameters for the request
     * @param   request   
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset data
     */
    @GetMapping(value = "/{datasetName}/{datasetType}/{dataType}", params = {"mode=similarity", "export!=true"})
    public ResponseEntity<StandardResponse<?>> getData(
            @ModelAttribute SimilarityDatasetRequest sdr,
            HttpServletRequest request)
    {
        sdr.setRequestType("get");
        StandardResponse<Object> response = datasetService.handleRequest(sdr);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Fetches the metadata for a respective dataset. The metadata includes all necessary information, besides the dataset data
     * itself, needed to display the dataset in the client. It includes metadata related to its size, information (set in configuration)
     * such as the description, sorting methods, available data types etc.
     * 
     * @param   datasetName   
     *          the name of the dataset to fetch the metadata for
     * @param   dataType      
     *          the data type of the dataset to fetch the metadata for (e.g., rdf, tabular, etc.)
     * @param   datasetType   
     *          the dataset type of the dataset to fetch the metadata for (e.g., heatmap, clusters, etc.)
     * @param   generalInfo   
     *          whether to include all the information regarding the dataset besides its shape, or vice-versa
     * @param   request       
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset metadata
     */
    @GetMapping("/metadata")
    public ResponseEntity<StandardResponse<?>> getMetadata(
            @RequestParam String datasetName,
            @RequestParam String dataType,
            @RequestParam String datasetType,
            @RequestParam Boolean generalInfo,
            HttpServletRequest request)
    {
        // TODO: investigate why not using RequestParam adds a bunch of trailing spaces
        MetadataRequest mr = new MetadataRequest();
        mr.setDatasetName(datasetName);
        mr.setGeneralInfo(generalInfo);
        mr.setDataType(dataType);
        mr.setDatasetType(datasetType);
        mr.setRequestType("get");

        StandardResponse<Object> response = datasetService.handleRequest(mr);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint responsible for executing queries generated in the Playground mode. Executes the SPARQL query for the input
     * dataset and returns the result.
     * 
     * @param   datasetName   
     *          the name of the dataset to execute the SPARQL query for
     * @param   datasetType   
     *          the dataset type of the dataset to execute the SPARQL query for, heatmap or clusters
     * @param   rawResults    
     *          whether to return the raw results from the SPARQL query execution or to parse them into a more user-friendly format
     * @param   dto           
     *          the SPARQL query request containing the SPARQL query to execute and other relevant parameters
     * @param   request       
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the SPARQL query results
     */
    @PostMapping("/{datasetName}/{datasetType}/rdf")
    public ResponseEntity<StandardResponse<?>> runSparqlQuery(
            @PathVariable String datasetName,
            @PathVariable String datasetType,
            @RequestParam(defaultValue = "false") boolean rawResults,
            @RequestBody SparqlQueryRequest dto,
            HttpServletRequest request)
    {
        // we only send the query as the body, add the rest of the parameters manually
        dto.setDatasetName(datasetName);
        dto.setDatasetType(datasetType);
        dto.setDataType("rdf");
        dto.setRawResults(rawResults);
        dto.setRequestType("update");

        StandardResponse<Object> response = datasetService.handleRequest(dto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint responsible for creating a new dataset. Accepts a dataset creation request and returns the result.
     * 
     * @param   dto
     *          the dataset creation request containing the dataset details
     * @param   request
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset creation result
     */
    @PostMapping("/create")
    public ResponseEntity<StandardResponse<?>> postDataset(
            @RequestBody DatasetPostRequest dto,
            HttpServletRequest request)
    {
        dto.setRequestType("post");
        dto.setCreation(true);

        StandardResponse<Object> response = datasetService.handleRequest(dto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint responsible for updating an existing dataset. Accepts a dataset update request and returns the result.
     * 
     * @param   datasetName   
     *          the name of the dataset to update
     * @param   datasetType   
     *          the dataset type of the dataset to update, heatmap or clusters
     * @param   dataType      
     *          the data type of the dataset to update (e.g., rdf, json, etc.)
     * @param   dto           
     *          the dataset update request containing the updated dataset details
     * @param   request       
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset update result
     */
    @PostMapping("/{datasetName}/{datasetType}/{dataType}")
    public ResponseEntity<StandardResponse<?>> postDataset(
            @PathVariable String datasetName,
            @PathVariable String datasetType,
            @PathVariable String dataType,
            @RequestBody DatasetPostRequest dto,
            HttpServletRequest request)
    {
        dto.setDatasetName(datasetName);
        dto.setDatasetType(datasetType);
        dto.setDataType(dataType);
        dto.setRequestType("post");
        dto.setCreation(false);

        StandardResponse<Object> response = datasetService.handleRequest(dto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint responsible for updating an existing dataset. Accepts a dataset update request and returns the result.
     * 
     * @param   datasetName   
     *          the name of the dataset to update
     * @param   datasetType   
     *          the dataset type of the dataset to update, heatmap or clusters
     * @param   dataType      
     *          the data type of the dataset to update (e.g., rdf, json, etc.)
     * @param   dto           
     *          the dataset update request containing the updated dataset details
     * @param   request       
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset update result
     */
    @PutMapping("/{datasetName}/{datasetType}/{dataType}")
    public ResponseEntity<StandardResponse<?>> putDataset(
            @PathVariable String datasetName,
            @PathVariable String datasetType,
            @PathVariable String dataType,
            @RequestBody DatasetPutRequest dto,
            HttpServletRequest request)
    {
        dto.setDatasetName(datasetName);
        dto.setDatasetType(datasetType);
        dto.setDataType(dataType);
        dto.setRequestType("put");

        StandardResponse<Object> response = datasetService.handleRequest(dto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint responsible for deleting an existing dataset. Accepts a dataset delete request and returns the result.
     * 
     * @param   datasetName   
     *          the name of the dataset to delete
     * @param   datasetType   
     *          the dataset type of the dataset to delete, heatmap or clusters
     * @param   dataType      the data type of the dataset to delete (e.g., rdf, json, etc.)
     * @param   dto           the dataset delete request containing the dataset details
     * @param   request       the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset delete result
     */
    @DeleteMapping("/{datasetName}/{datasetType}/{dataType}")
    public ResponseEntity<StandardResponse<?>> deleteDataset(
            @PathVariable String datasetName,
            @PathVariable String datasetType,
            @PathVariable String dataType,
            @RequestBody DatasetDeleteRequest dto,
            HttpServletRequest request)
    {
        dto.setDatasetName(datasetName);
        dto.setDatasetType(datasetType);
        dto.setDataType(dataType);
        dto.setRequestType("delete");

        StandardResponse<Object> response = datasetService.handleRequest(dto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint responsible for exporting an existing dataset. Accepts a dataset export request and returns 
     * the result of the export operation.
     * 
     * @param   exportRequest 
     *          the dataset export request containing the details of the dataset to export
     * @param   request       
     *          the HTTP servlet request
     * 
     * @return  the response entity containing the standard response with the dataset export result
     */
    @GetMapping(value = "/{datasetName}/{datasetType}/{dataType}", params = {"export=true"})
    public ResponseEntity<StandardResponse<?>> exportDataset(
            @ModelAttribute ExportRequest exportRequest,
            HttpServletRequest request)
    {
        exportRequest.setRequestType("get");

        StandardResponse<Object> response = datasetService.handleRequest(exportRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

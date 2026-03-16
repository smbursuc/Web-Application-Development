package com.soloproductions.wade.datatype;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dataset.*;
import com.soloproductions.wade.dto.ExportRequest;
import com.soloproductions.wade.metadata.DatasetMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Data type controller that reads and writes dataset data from and to JSON files stored on disk.
 * 
 * <p> As of this moment, no DDL operations are implemented for this data type. </p>
 */
public class JsonController extends AbstractDataTypeController
{
    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(JsonController.class);

    /**
     * Constructs a JSON controller that operates on the given dataset model.
     *
     * @param   datasetData
     *          dataset model to operate on
     */
    public JsonController(DatasetData datasetData)
    {
        super(datasetData);
    }

    /**
     * Executes a read request by delegating to the appropriate dataset-type handler.
     *
     * @return  dataset result for the current read request
     *
     * @throws  UnsupportedOperationException
     *          when the dataset type is unrecognized
     */
    public DataEntity executeReadRequest()
    {
        DatasetType dt = getDatasetData().getDatasetType();
        switch (dt)
        {
            case CLUSTERS ->
            {
                ClusterDataset cd = (ClusterDataset) getDatasetData();
                String clusterName = cd.getClusterName();
                Integer range = cd.getRange();
                Integer rangeStart = cd.getRangeStart();
                return executeQueryClusters(clusterName, range, rangeStart);
            }
            case HEATMAP ->
            {
                SimilarityDataset sd = (SimilarityDataset) getDatasetData();
                SimilaritySortCriteria ssc = sd.getSimilaritySortCriteria();
                return executeQueryHeatmap(ssc);
            }
        }
        throw AbstractDataTypeController.UnimplementedFeature(dt.name());
    }

    /**
     * Executes a heatmap read request, sorting and slicing the matrix according to
     * the active sort criteria and direction.
     *
     * @param   ssc
     *          similarity sort criteria to apply
     *
     * @return  filtered and sorted similarity dataset
     */
    public DataEntity executeQueryHeatmap(SimilaritySortCriteria ssc)
    {
        SimilarityDataset sd = (SimilarityDataset) getDatasetData();
        SimilarityDataset sdNew = new SimilarityDataset();

        List<List<Double>> matrix = sd.getMatrix();

        boolean changedSortCriteria = ssc != sd.getSimilaritySortCriteriaCache();
        SortDirection sortDirection = sd.getSortDirection();
        SortDirection sortDirectionCache = sd.getSortCache();
        if (sortDirection != sortDirectionCache || changedSortCriteria)
        {
            List<String> objects = sd.getObjects();

            AbstractDatasetData.handleRanges(null, sd, null, null, objects.size());
            int range = sd.getRange();
            int rangeStart = sd.getRangeStart();

            SimilarityDataset sorted = sortAndSliceSimilarityMatrix(objects, 
                                                                    matrix, 
                                                                    ssc, 
                                                                    sortDirection, 
                                                                    rangeStart, 
                                                                    rangeStart + range);

            sdNew.setObjects(sorted.getObjects());
            sdNew.setMatrix(sorted.getMatrix());
            sd.setSortCache(sortDirection);
        }

        return sdNew;
    }

    /**
     * Executes a cluster read request, optionally filtering by cluster name and
     * applying pagination and sorting.
     *
     * @param   clusterName
     *          name of a single cluster to return, or {@code null} to return all clusters
     * @param   range
     *          maximum number of clusters to return
     * @param   rangeStart
     *          starting offset into the cluster list
     *
     * @return  cluster node tree filtered to the requested slice
     *
     * @throws  IllegalStateException
     *          when the root node contains no children
     */
    public DataEntity executeQueryClusters(String clusterName, Integer range, Integer rangeStart)
    {
        ClusterDataset cd = (ClusterDataset) getDatasetData();

        ClusterNode root = cd.getRootNode();
        if (root.getChildren() == null || root.getChildren().isEmpty())
        {
            throw new IllegalStateException("Cannot apply filters with no data!");
        }

        List<ClusterNode> children = root.getChildren();
        int nrClusters = children.size();

        // Filter by cluster name
        if (clusterName != null && !clusterName.isEmpty())
        {
            for (ClusterNode cluster : root.getChildren())
            {
                if (cluster.getName().equals(clusterName))
                {
                    ClusterNode result = new ClusterNode();
                    result.setName(root.getName());
                    result.setChildren(Collections.singletonList(cluster));
                    return result;
                }
            }
        }

        SortDirection sd = cd.getSortDirection();
        if (sd != cd.getSortCache())
        {
            sortClusterChildren(root, sd);
            cd.setSortCache(sd);
        }

        AbstractDatasetData.handleRanges(null, cd, null, null, nrClusters);

        int rangeEnd = Math.min(rangeStart + range, nrClusters);
        List<ClusterNode> limited = children.subList(rangeStart, rangeEnd);

        ClusterNode result = new ClusterNode();
        result.setName(root.getName());
        result.setChildren(limited);

        return result;
    }

    /**
     * Returns metadata for the active dataset by computing the number of top-level items
     * directly from the in-memory data.
     *
     * @return  dataset metadata containing the item count
     */
    public DatasetMetadata getMetadata()
    {
        DatasetData dd = getDatasetData();
        DatasetMetadata md = new DatasetMetadata();
        switch (dd.getDatasetType())
        {
            case CLUSTERS ->
            {
                ClusterDataset cd = (ClusterDataset) dd;
                int size = cd.getRootNode().getChildren().size();
                md.setSize(size);
            }
            case HEATMAP ->
            {
                SimilarityDataset sd = (SimilarityDataset) dd;
                int size = sd.getObjects().size();
                md.setSize(size);
            }
        }
        return md;
    }

    /**
     * Exports the dataset as a JSON string. When filters are honoured the read request
     * is executed and its result is serialized; otherwise the raw JSON file is returned.
     *
     * @param   exportRequest
     *          export configuration
     *
     * @return  JSON string representation of the exported dataset
     *
     * @throws  RuntimeException
     *          when the file cannot be read or serialized
     */
    @Override
    public Object exportData(ExportRequest exportRequest)
    {
        try
        {
            DatasetData dd = getDatasetData();
            String filePath = ((AbstractDatasetData) dd).resolveFilePath(dd.getDatasetName());
            LOG.info("JsonController.exportData called for dataset={}, filePath={}, honorFilters={}", dd.getDatasetName(), filePath, exportRequest.isHonorFilters());
            
            if (exportRequest.isHonorFilters())
            {
                // Execute read request with filters applied to get filtered data
                Object filteredData = executeReadRequest();
                
                // Convert the filtered data to JSON (clean escaped newlines / pretty-print)
                ObjectMapper mapper = new ObjectMapper();
                String json = serializeExportObject(filteredData, mapper);
                LOG.info("JsonController.exportData filtered result size={}", json == null ? 0 : json.length());
                return json;
            }
            else
            {
                // Return raw file content
                File file = new File(filePath);
                if (!file.exists())
                {
                    LOG.warn("JsonController.exportData file not found: {}", filePath);
                    throw new IllegalStateException("Data file not found: " + filePath);
                }
                String content = Files.readString(file.toPath());
                ObjectMapper mapper = new ObjectMapper();
                String cleaned = serializeExportObject(content, mapper);
                LOG.info("JsonController.exportData raw file length={}", cleaned == null ? 0 : cleaned.length());
                return cleaned;
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed to export JSON data", e);
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }
}

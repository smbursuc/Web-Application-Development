package com.soloproductions.wade.datatype;

import com.soloproductions.wade.dataset.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.DatasetBodyRequest;
import com.soloproductions.wade.metadata.DatasetMetadata;
import com.soloproductions.wade.repository.UserDatasetRepository;
import com.soloproductions.wade.entity.UserDataset;
import com.soloproductions.wade.util.ApplicationContextProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base implementation shared by all data-type-specific query controllers.
 * Provides default (unimplemented) CRUD stubs, shared sorting helpers for cluster
 * and heatmap data, and common export serialization support.
 */
public abstract class AbstractDataTypeController implements DataTypeController
{
    /** Dataset model instance that this controller operates on. */
    private DatasetData datasetData;

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(AbstractDataTypeController.class);

    /**
     * Constructs a controller that operates on the given dataset model.
     *
     * @param   datasetData
     *          dataset model to operate on
     */
    public AbstractDataTypeController(DatasetData datasetData)
    {
        this.datasetData = datasetData;
    }

    /**
     * Registers a user-created dataset in the persistent dataset registry.
     * 
     * <p> No-op for built-in default datasets. </p>
     *
     * @param   username
     *          owner of the dataset
     * @param   datasetName
     *          name of the dataset to register
     * @param   type
     *          high-level dataset category
     */
    protected void registerDatasetMetadata(String username, String datasetName, DatasetType type)
    {
        if (AbstractDatasetData.isDefaultDataset(datasetName))
        {
            return; // Don't register default datasets in the user registry
        }
        try
        {
            DataType dataType = getDatasetData().getDataType();
            UserDatasetRepository repo = ApplicationContextProvider.getBean(UserDatasetRepository.class);
            if (repo != null && !repo.findByUsernameAndDatasetNameAndDatasetTypeAndDataType(username, datasetName, type.name(), dataType.name()).isPresent())
            {
                repo.save(new UserDataset(username, datasetName, type.name(), dataType.name()));
                LOG.info("Registered dataset {} (Type: {}, Source: {}) for user {}", datasetName, type, dataType, username);
            }
        }
        catch (Exception e)
        {
            String msg = "Failed to register dataset metadata for dataset " + datasetName + ": " + e.getMessage();
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Dispatches the request to the correct CRUD handler based on its HTTP method type.
     *
     * @param   adr
     *          request to dispatch
     *
     * @return  result from the invoked CRUD handler
     *
     * @throws  IllegalStateException
     *          when the dataset type or request type is unrecognized
     */
    public Object executeQuery(AbstractDatasetRequest adr)
    {
        DatasetType dt = getDatasetData().getDatasetType();
        if (dt != DatasetType.CLUSTERS && dt != DatasetType.HEATMAP)
        {
            throw new IllegalStateException("Dataset type not recognized! " + adr.getDatasetType());
        }

        DatasetRequestType drt = AbstractDatasetData.resolveRequestType(adr.getRequestType());
        Object result;
        if (drt == DatasetRequestType.READ)
        {
            result = executeReadRequest();
        }
        else if (drt == DatasetRequestType.CREATE)
        {
            result = executeCreateRequest(adr);
        }
        else if (drt == DatasetRequestType.UPDATE)
        {
            result = executeUpdateRequest(adr);
        }
        else if (drt == DatasetRequestType.DELETE)
        {
            result = executeDeleteRequest(adr);
        }
        else
        {
            throw new IllegalStateException("Request type not recognized! Request type: " + adr.getRequestType());
        }
        return result;
    }

    /**
     * Executes a read (GET) request. Subclasses must override this method.
     *
     * @return  dataset result for the read request
     *
     * @throws  UnsupportedOperationException
     *          when not overridden by a concrete subclass
     */
    public Object executeReadRequest()
    {
        throw UnimplementedFeature("READ (GET) for " + getDatasetData().getDatasetType());
    }

    /**
     * Executes an update (PUT) request. Subclasses must override this method.
     *
     * @param   adr
     *          request carrying the update payload
     *
     * @return  result of the update operation
     *
     * @throws  UnsupportedOperationException
     *          when not overridden by a concrete subclass
     */
    public Object executeUpdateRequest(AbstractDatasetRequest adr)
    {
        throw UnimplementedFeature("UPDATE (PUT) for " + getDatasetData().getDatasetType());
    }

    /**
     * Executes a create (POST) request. Subclasses must override this method.
     *
     * @param   adr
     *          request carrying the creation payload
     *
     * @return  result of the create operation
     *
     * @throws  UnsupportedOperationException
     *          when not overridden by a concrete subclass
     */
    public Object executeCreateRequest(AbstractDatasetRequest adr)
    {
        throw UnimplementedFeature("CREATE (POST) for " + getDatasetData().getDatasetType());
    }

    /**
     * Executes a delete (DELETE) request. Subclasses must override this method.
     *
     * @param   adr
     *          request carrying the delete criteria
     *
     * @return  result of the delete operation
     *
     * @throws  UnsupportedOperationException
     *          when not overridden by a concrete subclass
     */
    public Object executeDeleteRequest(AbstractDatasetRequest adr)
    {
        throw UnimplementedFeature("DELETE for " + getDatasetData().getDatasetType());
    }

    /**
     * Exports dataset data. Subclasses must override this method.
     *
     * @return  exported data payload
     *
     * @throws  UnsupportedOperationException
     *          when not overridden by a concrete subclass
     */
    public Object exportData()
    {
        DataType dt = getDatasetData().getDataType();
        String message = String.format("Exporting data for this type for data type %s is not supported!", dt.name());
        throw UnimplementedFeature(message);
    }

    /**
     * Executes a SPARQL query against the configured endpoint.
     * This is a no-op placeholder; the actual execution is performed in
     * {@link RdfController#executeSparqlQuery(String)}.
     *
     * @param   sparqlQuery
     *          SPARQL query string to execute
     *
     * @return  list of result rows as variable-to-string maps, or {@code null} in the base implementation
     */
    public List<Map<String, String>> executeSparqlQuery(String sparqlQuery)
    {
        // no-op, executed by subclass
        return null;
    }

    /**
     * Creates a standardized {@link UnsupportedOperationException} for unimplemented features.
     *
     * @param   message
     *          description of the feature that is not yet implemented
     *
     * @return  exception instance ready to be thrown
     */
    public static UnsupportedOperationException UnimplementedFeature(String message) throws UnsupportedOperationException
    {
        return new UnsupportedOperationException("The feature " + message + " has not been implemented!");
    }

    /**
     * Updates the dataset using the given request body. Subclasses may override this method.
     *
     * @param   dbr
     *          request body carrying the update payload
     *
     * @return  {@code true} when the update succeeds; always {@code false} in the base implementation
     */
    public boolean update(DatasetBodyRequest dbr)
    {
        return false;
    }

    /**
     * Returns metadata for the current dataset.
     * Returns an empty metadata object in the base implementation; subclasses override this.
     *
     * @return  dataset metadata
     */
    public DatasetMetadata getMetadata()
    {
        return new DatasetMetadata();
    }

    /**
     * Returns the dataset model that this controller operates on.
     *
     * @return  dataset model instance
     */
    public DatasetData getDatasetData()
    {
        return datasetData;
    }

    /**
     * Serializes an object for export. If the object is already a {@link String} containing
     * escaped newlines (e.g. {@code "\\r\\n"}) or serialized JSON, this will try to unescape
     * and pretty-print it for readability. Otherwise the object is serialized using the
     * provided {@link ObjectMapper} with the default pretty printer.
     *
     * @param   filteredData
     *          the object to serialize; may be a raw {@link String} or any serializable type
     * @param   mapper
     *          Jackson mapper to use; if {@code null} a default instance is created
     *
     * @return  pretty-printed JSON string representing the data
     */
    protected String serializeExportObject(Object filteredData, ObjectMapper mapper)
    {
        if (filteredData == null) 
        {
            return "null";
        }

        if (mapper == null) mapper = new ObjectMapper();

        if (filteredData instanceof String)
        {
            String s = (String) filteredData;
            // Try parsing the string as JSON first
            try
            {
                Object parsed = mapper.readValue(s, Object.class);
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
            }
            catch (IOException ignore)
            {
            }

            // Replace common escaped newline sequences with real newlines and try again
            String unescaped = s.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
            try
            {
                Object parsed = mapper.readValue(unescaped, Object.class);
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
            }
            catch (IOException ignore)
            {

            }

            // Not JSON — return unescaped raw text for readability
            return unescaped;
        }

        try
        {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredData);
        }
        catch (JsonProcessingException e)
        {
            try
            {
                return mapper.writeValueAsString(filteredData);
            }
            catch (JsonProcessingException ex)
            {
                return String.valueOf(filteredData);
            }
        }
    }

    /**
     * Replaces the dataset model that this controller operates on.
     *
     * @param   datasetData
     *          new dataset model to use
     */
    public void setDatasetData(DatasetData datasetData)
    {
        this.datasetData = datasetData;
    }

    /**
     * Computes the average probability of the leaf nodes within a cluster.
     * Used as the sort key when ordering clusters by probability.
     *
     * @param   cluster
     *          cluster node whose leaf probabilities should be averaged
     *
     * @return  average leaf probability, or {@code 0.0} when the cluster has no children
     */
    protected static double calculateAverageProbability(ClusterNode cluster)
    {
        if (cluster.getChildren() == null || cluster.getChildren().isEmpty())
        {
            return 0.0;
        }

        return cluster.getChildren().stream()
                .map(ClusterNode::getProbability)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Sorts the top-level children of a cluster root in-place by average leaf
     * probability. No-op when {@code sortDirection} is {@code null} or
     * {@link SortDirection#NO_SORT}.
     *
     * @param   root
     *          the cluster tree root whose children will be reordered
     * @param   sortDirection
     *          the desired sort direction
     *
     * @return  the same {@code root} for call-chaining convenience
     */
    protected static ClusterNode sortClusterChildren(ClusterNode root, SortDirection sortDirection)
    {
        List<ClusterNode> children = root.getChildren();
        if (children == null || children.isEmpty()) 
        {
            return root;
        }

        boolean doSort = sortDirection != null && sortDirection != SortDirection.NO_SORT;
        if (!doSort) 
        {
            return root;
        }

        boolean reverse = sortDirection != SortDirection.ASCENDING;
        Comparator<ClusterNode> comparator = Comparator.comparingDouble(AbstractDataTypeController::calculateAverageProbability);
        if (reverse)
        {
            comparator = comparator.reversed();
        }

        root.setChildren(
                children.stream()
                        .sorted(comparator)
                        .collect(Collectors.toList())
        );
        return root;
    }

    /**
     * Sorts a similarity matrix by the given criteria and direction, then slices
     * the result to the range {@code [rangeStart, rangeEnd)}.
     * Both rows and columns are reordered using the sorted index permutation so
     * the returned sub-matrix is internally consistent.
     * Pass {@code rangeStart = 0} and {@code rangeEnd = n} to retain the full matrix.
     *
     * @param   objects
     *          object labels corresponding to matrix rows and columns
     * @param   matrix
     *          the full square similarity matrix
     * @param   ssc
     *          sort criteria ({@code AVERAGE_SIMILARITY}, {@code STRONGEST_PAIR}, or {@code DEFAULT})
     * @param   sortDirection
     *          sort direction; {@code null} or {@link SortDirection#NO_SORT} preserves original order
     * @param   rangeStart
     *          inclusive start index of the output slice
     * @param   rangeEnd
     *          exclusive end index of the output slice
     *
     * @return  a new {@link SimilarityDataset} containing the sorted and sliced data
     */
    protected static SimilarityDataset sortAndSliceSimilarityMatrix(List<String> objects, 
                                                                    List<List<Double>> matrix,
                                                                    SimilaritySortCriteria ssc, 
                                                                    SortDirection sortDirection,
                                                                    int rangeStart, 
                                                                    int rangeEnd)
    {
        int n = objects.size();

        // Compute sort metric per row
        double[] sortMetric = new double[n];
        if (ssc == null) ssc = SimilaritySortCriteria.DEFAULT;
        for (int i = 0; i < n; i++)
        {
            List<Double> row = matrix.get(i);
            switch (ssc)
            {
                case AVERAGE_SIMILARITY:
                    sortMetric[i] = row.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    break;
                case STRONGEST_PAIR:
                    sortMetric[i] = row.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                    break;
                case DEFAULT:
                    sortMetric[i] = i;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid sortType option. Use " + AbstractDatasetData.SIMILARITY_SORT_CRITERIAS);
            }
        }

        // Build sorted index permutation
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);

        boolean doSort = sortDirection != null && sortDirection != SortDirection.NO_SORT;
        if (doSort)
        {
            boolean reverse = sortDirection != SortDirection.ASCENDING;
            indices.sort((a, b) -> reverse
                    ? Double.compare(sortMetric[b], sortMetric[a])
                    : Double.compare(sortMetric[a], sortMetric[b]));
        }

        // Reorder objects and matrix rows+columns using sorted indices, then slice
        List<String> sortedObjects = indices.stream().map(objects::get).collect(Collectors.toList());
        int end = Math.min(rangeEnd, n);
        int start = Math.min(rangeStart, end);

        List<List<Double>> sortedMatrix = new ArrayList<>();
        for (int i = start; i < end; i++)
        {
            List<Double> newRow = new ArrayList<>();
            for (int j = start; j < end; j++)
            {
                newRow.add(matrix.get(indices.get(i)).get(indices.get(j)));
            }
            sortedMatrix.add(newRow);
        }

        SimilarityDataset result = new SimilarityDataset();
        result.setObjects(sortedObjects.subList(start, end));
        result.setMatrix(sortedMatrix);
        return result;
    }
}

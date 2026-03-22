package com.soloproductions.wade.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soloproductions.wade.datatype.DataTypeController;
import com.soloproductions.wade.datatype.JsonController;
import com.soloproductions.wade.datatype.RdfController;
import com.soloproductions.wade.datatype.SqlController;
import com.soloproductions.wade.dto.*;
import com.soloproductions.wade.metadata.DatasetMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Base implementation shared by dataset models such as heatmaps and clusters.
 * 
 * <p> It provides common request handling, type resolution, metadata access, file loading,
 * and range validation behavior. </p>
 */
public abstract class AbstractDatasetData implements DatasetData
{
    /** Current sort direction for the dataset. See {@link SortDirection}. */
    private SortDirection sortDirection = SortDirection.NO_SORT;

    /** 
     * Cached sort direction used for temporary storage during sorting operations. 
     * Used for performance reasons to avoid recalculating the sort direction repeatedly.
     */
    private SortDirection sortCache;

    /** Metadata associated with the dataset. */
    private DatasetMetadata datasetMetadata;

    /** Type of data contained in the dataset. See {@link DataType}. */
    private DataType dataType;

    /** Controllers for each supported data type. The key is the data type. */
    private Map<DataType, DataTypeController> dtc = new HashMap<>();

    /** Name of the dataset. */
    private String datasetName;

    /** Range value for read requests. */
    private Integer range;

    /** Range start value for read requests. */
    private Integer rangeStart;

    /** Pending query string for the dataset. If not null, a SPARQL query is pending execution. */
    private String pendingQuery;

    /** Flag indicating whether for this dataset the last SPARQL query request returned raw results. */
    private boolean rawResults;

    /** Default datasets that are not user created and available for all users. */
    public static final List<String> DATASETS = List.of("bsds300", "cifar10");

    /** Supported sort direction values accepted by dataset requests. */
    public static final List<String> SORT_DIRECTIONS = List.of(
            "ascending",
            "descending",
            "no_sort",
            "highest_probability",
            "lowest_probability"
    );

    /** Supported persistence/data source types. */
    public static final List<String> DATA_TYPES = List.of("rdf","json","sql");

    /** Supported dataset model types. */
    public static final List<String> DATASET_TYPES = List.of("heatmap", "clusters");

    /** Supported similarity sorting strategies. Only used for heatmaps. */
    public static final List<String> SIMILARITY_SORT_CRITERIAS = List.of("average_similarity", "strongest_pair", "default");

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(AbstractDatasetData.class);

    /** Maximum allowed range value for paginated requests. */
    public static final int MAX_RANGE = 100;

    /** Default range value used when the request does not provide one. */
    public static final int DEFAULT_RANGE = 20;

    /**
     * Resolves the JSON file path for the given dataset name.
     * Default datasets are loaded from the shared data directory, while user datasets
     * are created through the API and stored persistently in a database. So for dynamic
     * datasets this is a no-op.
     *
     * @param   dataset 
     *          dataset identifier to resolve
     * 
     * @return  relative path to the dataset JSON file
     * 
     * @throws  IllegalArgumentException when the dataset is not a default dataset
     */
    public String resolveFilePath(String dataset)
    {
        String basepath = switch (getDatasetType())
        {
            case CLUSTERS -> "hierarchical_structure_uri";
            case HEATMAP -> "similarity_data";
        };

        Datasets datasetEnum = resolveDatasetFromString(dataset);
        String comp = null;
        if (datasetEnum == Datasets.CIFAR10 && getDatasetType() == DatasetType.HEATMAP)
        {
            // the similarity data is actually just 500 entries because it's too heavy to load fully in memory
            comp = "500";
        }
        else
        {
            comp = dataset;
        }

        if (isDefaultDataset(dataset)) 
        {
            return String.format("data/%s_%s.json", basepath, comp);
        }
        else
        {
            String msg = String.format("Dataset %s is not a default dataset. No file path can be resolved.", dataset);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Converts a dataset enum value to its external string representation. Only for default datasets.
     *
     * @param   dataset 
     *          dataset enum value to convert
     * 
     * @return  dataset name string, or {@code null} when the input is {@code null}
     */
    public static String resolveDatasetFromEnum(Datasets dataset)
    {
        if (dataset == null)
        {
            return null;
        }

        return switch (dataset)
        {
            case BSDS300 -> "bsds300";
            case CIFAR10 -> "cifar10";
        };
    }

    /**
     * Converts a dataset name string to the matching enum value. Only for default datasets.
     *
     * @param   name 
     *          dataset name to resolve
     * 
     * @return  matching dataset enum value, or {@code null} when the value is unknown
     */
    public static Datasets resolveDatasetFromString(String name)
    {
        if (name == null)
        {
            return null;
        }

        return switch (name)
        {
            case "bsds300" -> Datasets.BSDS300;
            case "cifar10" -> Datasets.CIFAR10;
            default -> null;
        };
    }

    /**
     * Resolves an external sort direction string to the internal enum value.
     * A missing sort parameter defaults to {@link SortDirection#NO_SORT}.
     *
     * @param   direction 
     *          sort direction name from a request
     * 
     * @return  matching sort direction, {@link SortDirection#NO_SORT} when omitted, or {@code null} when unknown
     */
    public static SortDirection resolveSortDirectionFromString(String direction)
    {
        if (direction == null)
        {
            return SortDirection.NO_SORT;
        }

        return switch (direction.trim().toLowerCase(Locale.ROOT))
        {
            case "ascending", "lowest_probability" -> SortDirection.ASCENDING;
            case "descending", "highest_probability" -> SortDirection.DESCENDING;
            case "no_sort" -> SortDirection.NO_SORT;
            default -> null;
        };
    }

    /**
     * Resolves a heatmap sorting string to the internal enum value.
     *
     * @param   criteria 
     *          similarity sort criteria from a request
     * 
     * @return  matching similarity sort criteria, {@link SimilaritySortCriteria#DEFAULT} when omitted, or {@code null} when unknown
     */
    public static SimilaritySortCriteria resolveSimilaritySortCriteriaFromString(String criteria)
    {
        if (criteria == null)
        {
            return SimilaritySortCriteria.DEFAULT;
        }

        return switch (criteria)
        {
            case "average_similarity" -> SimilaritySortCriteria.AVERAGE_SIMILARITY;
            case "strongest_pair" -> SimilaritySortCriteria.STRONGEST_PAIR;
            default -> null;
        };
    }

    /**
     * Resolves a data type string to the matching enum value.
     *
     * @param   type 
     *          data type name, possibly from a payload
     * 
     * @return  matching data type, or {@code null} when the value is missing or unknown
     */
    public static DataType resolveDataTypeFromString(String type)
    {
        if (type == null)
        {
            return null;
        }

        return switch (type)
        {
            case "rdf" -> DataType.RDF;
            case "json" -> DataType.JSON;
            case "sql" -> DataType.SQL;
            default -> null;
        };
    }

    /**
     * Resolves a dataset type string to the matching enum value.
     *
     * @param   type 
     *          dataset type name, possibly from a payload
     * 
     * @return  matching dataset type, or {@code null} when the value is missing or unknown
     */
    public static DatasetType resolveDatasetTypeFromString(String type)
    {
        if (type == null)
        {
            return null;
        }

        return switch (type)
        {
            case "heatmap" -> DatasetType.HEATMAP;
            case "clusters" -> DatasetType.CLUSTERS;
            default -> null;
        };
    }

    /**
     * Resolves an HTTP method name to the corresponding dataset request type.
     *
     * @param   type 
     *          lowercase HTTP method name
     * 
     * @return  matching dataset request type, or {@code null} when unsupported
     */
    public static DatasetRequestType resolveRequestType(String type)
    {
        DatasetRequestType requestType = null;
        switch (type)
        {
            case "post" -> requestType = DatasetRequestType.CREATE;
            case "delete" -> requestType = DatasetRequestType.DELETE;
            case "put" -> requestType = DatasetRequestType.UPDATE;
            case "get" -> requestType = DatasetRequestType.READ;
        }
        return requestType;
    }

    /**
     * Loads a dataset (heatmap/cluster) from a JSON file.
     *
     * @param   filePath 
     *          path to the JSON file
     * @param   dataType 
     *          entity type to deserialize into
     * 
     * @return  deserialized dataset entity
     * 
     * @throws IOException when the file cannot be read or parsed
     */
    protected DataEntity loadFromFile(String filePath, Class<? extends DataEntity> dataType) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), dataType);
    }

    /**
     * Applies common filter and pagination values from a request to this dataset instance.
     *
     * @param   adr 
     *          request whose filter values should be copied onto the dataset
     */
    public void applyFilters(AbstractDatasetRequest adr)
    {
        SortDirection sd = AbstractDatasetData.resolveSortDirectionFromString(adr.getSortDirection());
        DataType dt = AbstractDatasetData.resolveDataTypeFromString(adr.getDataType());

        String dataset = adr.getDatasetName();
        Integer range = adr.getRange();
        Integer rangeStart = adr.getRangeStart();
        setRange(range);
        setRangeStart(rangeStart);
        setSortDirection(sd);
        setDataType(dt);
        setDatasetName(dataset);
    }

    /**
     * Returns the controller responsible for the dataset's current data type.
     * The controller instance is created lazily and cached per data type.
     *
     * @return  controller for the current data type
     */
    @JsonIgnore
    public DataTypeController getDataTypeController()
    {
        DataType dt = getDataType();
        if (dtc.get(dt) == null)
        {
            DataTypeController dtcNew = null;
            switch (dt)
            {
                case JSON ->
                {
                    dtcNew = new JsonController(this);
                }
                case RDF ->
                {

                    dtcNew = new RdfController(this);
                }
                case SQL ->
                {
                    dtcNew = new SqlController(this);
                }
            };
            dtc.put(dt, dtcNew);
        }

        return dtc.get(dt);
    }

    /**
     * Validates and normalizes range values for either a request or a dataset context.
     * Exactly one of {@code adr} or {@code dd} must be non-null.
     *
     * @param   adr request context to mutate when validating request ranges
     * @param   dd dataset context to mutate when validating dataset ranges
     * @param   range requested number of items to return
     * @param   rangeStart requested starting offset
     * @param   maxPossibleRange upper bound allowed by the current dataset or query
     */
    public static void handleRanges(AbstractDatasetRequest adr,
                                    DatasetData dd,
                                    Integer range,
                                    Integer rangeStart,
                                    Integer maxPossibleRange)
    {
        if (dd != null)
        {
            if (range == null)
            {
                range = dd.getRange();
            }

            if (rangeStart == null)
            {
                rangeStart = dd.getRangeStart();
            }
        }

        if (adr == null && dd == null || adr != null && dd != null)
        {
            throw new IllegalStateException("Cannot compute context for range validation. Please only pass one non-null context," +
                    "either for a request or for a query validation using a dataset.");
        }

        // when fetching data for a single cluster node we do not need to check the passed ranges
        boolean skipRangeValidation = false;
        if (adr != null)
        {
            if (adr.isClustersRequest())
            {
                ClusterDatasetRequest cdr = (ClusterDatasetRequest) adr;
                if (cdr.getClusterName() != null)
                {
                    skipRangeValidation = true;
                }
            }
        }

        if (dd != null)
        {
            if (dd instanceof ClusterDataset cd)
            {
                if (cd.getClusterName() != null)
                {
                    skipRangeValidation = true;
                }
            }
        }

        if (skipRangeValidation)
        {
            return;
        }

        if (range == null)
        {
            LOG.warn("Empty range parameter. Using the default value of " + DEFAULT_RANGE + ".");
            if (dd != null)
            {
                dd.setRange(DEFAULT_RANGE);
            }
            else
            {
                adr.setRange(DEFAULT_RANGE);
            }
            range = DEFAULT_RANGE;
        }

        if (rangeStart == null)
        {
            LOG.warn("Empty range parameter. Setting rangeStart from the start (0).");
            if (dd != null)
            {
                dd.setRangeStart(0);
            }
            else
            {
                adr.setRangeStart(0);
            }
            rangeStart = 0;
        }

        if (range > MAX_RANGE)
        {
            throw new IllegalArgumentException("The provided range is too high for this type of request. Use a value lower than" +
                    "100");
        }

        if (range < 0 || rangeStart < 0)
        {
            throw new IllegalArgumentException("Range values provided include negative values. This is not allowed.");
        }

        if (maxPossibleRange != null && maxPossibleRange != 0)
        {
            // RDF query will silently fail while the JSON data query will not be able to proceed due to splicing and index
            // out of bounds exceptions
            if (rangeStart >= maxPossibleRange)
            {
                if (dd != null && dd.getDataType() == DataType.RDF)
                {
                    LOG.warn("Query detected that is using an OFFSET outside of the possible range!");
                }
                else
                {
                    String errorMsg = String.format("Invalid rangeStart value. Must be between possible bounds [%d, %d].", 0, maxPossibleRange);
                    throw new IllegalArgumentException(errorMsg);
                }
            }

            int sum = range + rangeStart;
            if (sum > maxPossibleRange)
            {
                if (dd != null && dd.getDataType() == DataType.RDF)
                {
                    LOG.warn("Query detected that is using a LIMIT + OFFSET value outside of the possible range!");
                }
                else
                {
                    String errorMsg = String.format("Invalid rangeStart/range values. Their sum (%d) goes over the %d limit.", sum, maxPossibleRange);
                    throw new IllegalArgumentException(errorMsg);
                }
            }
        }
    }

    /**
     * Applies special request behavior that does not belong to normal filter handling.
     *
     * @param   adr request to inspect
     * 
     * @return  {@code true} when the request is metadata, SPARQL, or export specific
     */
    public boolean handleSpecialRequest(AbstractDatasetRequest adr)
    {
        if (adr.isSparqlQueryRequest())
        {
            SparqlQueryRequest sqd = (SparqlQueryRequest) adr;
            setPendingQuery(sqd.getQuery());
            setRawResults(sqd.isRawResults());
        }

        return adr.isMetadataRequest() || adr.isSparqlQueryRequest() || adr.isExportRequest();
    }

    /**
     * Executes the request using the currently resolved data type controller.
     *
     * @param   adr 
     *          request to execute
     * 
     * @return  request result produced by the active data type controller
     */
    public Object executeRequest(AbstractDatasetRequest adr)
    {
        return getDataTypeController().executeQuery(adr);
    }

    /**
     * Exports dataset data using the currently resolved data type controller.
     *
     * @param   exportRequest export configuration
     * 
     * @return  exported data payload
     */
    public Object exportData(ExportRequest exportRequest)
    {
        return getDataTypeController().exportData(exportRequest);
    }

    /**
     * Checks whether a dataset name refers to one of the built-in datasets.
     *
     * @param   dataset 
     *          dataset identifier to test
     * 
     * @return  {@code true} when the dataset is one of the predefined built-in datasets
     */
    public static boolean isDefaultDataset(String dataset)
    {
        return AbstractDatasetData.resolveDatasetFromString(dataset) != null;
    }

    /**
     * Returns the active sort direction.
     *
     * @return  current sort direction
     */
    public SortDirection getSortDirection()
    {
        return sortDirection;
    }

    /**
     * Sets the active sort direction.
     *
     * @param   sortDirection 
     * sort direction to store
     */
    public void setSortDirection(SortDirection sortDirection)
    {
        this.sortDirection = sortDirection;
    }

    /**
     * Returns the cached sort direction.
     *
     * @return  cached sort direction
     */
    public SortDirection getSortCache()
    {
        return sortCache;
    }

    /**
     * Sets the cached sort direction.
     *
     * @param   sortCache 
     *          cached sort direction value
     */
    public void setSortCache(SortDirection sortCache)
    {
        this.sortCache = sortCache;
    }

    /**
     * Returns the active data type.
     *
     * @return  current data type
     */
    public DataType getDataType()
    {
        return dataType;
    }

    /**
     * Sets the active data type.
     *
     * @param   dataType 
     *          data type to use
     */
    public void setDataType(DataType dataType)
    {
        this.dataType = dataType;
    }

    /**
     * Returns the dataset name.
     *
     * @return  current dataset name
     */
    public String getDatasetName()
    {
        return datasetName;
    }

    /**
     * Sets the dataset name.
     *
     * @param   datasetName 
     *          dataset name to store
     */
    public void setDatasetName(String datasetName)
    {
        this.datasetName = datasetName;
    }

    /**
     * Returns the requested range size.
     *
     * @return  current range size
     */
    public Integer getRange()
    {
        return range;
    }

    /**
     * Sets the requested range size.
     *
     * @param   range 
     * range size to store
     */
    public void setRange(Integer range)
    {
        this.range = range;
    }

    /**
     * Returns the starting offset for paginated access.
     *
     * @return  current starting offset
     */
    public Integer getRangeStart()
    {
        return rangeStart;
    }

    /**
     * Sets the starting offset for paginated access.
     *
     * @param   rangeStart 
     *          starting offset to store
     */
    public void setRangeStart(Integer rangeStart)
    {
        this.rangeStart = rangeStart;
    }

    /**
     * Stores resolved dataset metadata on this dataset instance.
     *
     * @param   datasetMetadata 
     *          metadata to store
     */
    public void setMetadata(DatasetMetadata datasetMetadata)
    {
        this.datasetMetadata = datasetMetadata;
    }

    /**
     * Loads and returns metadata for the current dataset, including static and dynamic entries.
     *
     * @return   resolved dataset metadata
     * 
     * @throws   IOException when metadata sources cannot be read
     */
    public DatasetMetadata getMetadata() throws IOException
    {
        // get the persistence related metadata
        DatasetMetadata dm = getDataTypeController().getMetadata();

        // expose the server-side constant so clients can cap their range slider
        dm.setMaxRange(MAX_RANGE);

        // complete the data with static metadata
        setMetadataStatic(dm, DatasetMetadata.CONFIG_PATH);
        setMetadata(dm);
        dm.addDynamicDatasets();
        return datasetMetadata;
    }

    /**
     * Loads static metadata with the default datasets data and also user generated datasets.
     *
     * @param   dm 
     *          metadata object to populate
     * 
     * @throws  IOException 
     *          when the metadata file cannot be read
     */
    public static void setMetadataStatic(DatasetMetadata dm) throws IOException
    {
        setMetadataStatic(dm, DatasetMetadata.CONFIG_PATH);
        dm.addDynamicDatasets();
    }

    /**
     * Loads static metadata from the specified configuration path.
     *
     * @param   dm 
     *          metadata object to populate
     * @param   path 
     *          metadata configuration file path
     * 
     * @throws  IOException 
     *          when the metadata file cannot be read
     */
    public static void setMetadataStatic(DatasetMetadata dm, String path) throws IOException
    {
        dm.loadMetadataFile(path);
    }

    /**
     * Returns the metadata cached for this dataset, but does not compute it if missing.
     *
     * @return  cached metadata instance
     */
    public DatasetMetadata getMetadataNoSet()
    {
        return datasetMetadata;
    }

    /**
     * Returns a pending SPARQL query associated with this dataset.
     *
     * @return  pending query text
     */
    public String getPendingQuery()
    {
        return pendingQuery;
    }

    /**
     * Sets a pending SPARQL query associated with this dataset.
     *
     * @param   pendingQuery 
     *          query text to store
     */
    public void setPendingQuery(String pendingQuery)
    {
        this.pendingQuery = pendingQuery;
    }

    /**
     * Returns whether raw query results should be returned without mapping.
     *
     * @return  {@code true} when raw results should be preserved
     */
    public boolean isRawResults()
    {
        return rawResults;
    }

    /**
     * Sets whether raw query results should be returned without mapping.
     *
     * @param   rawResults 
     *          whether raw results should be preserved
     */
    public void setRawResults(boolean rawResults)
    {
        this.rawResults = rawResults;
    }
}

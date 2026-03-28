package com.soloproductions.wade.dto;

import com.soloproductions.wade.dataset.*;
import com.soloproductions.wade.metadata.DatasetMetadata;
import com.soloproductions.wade.metadata.FeatureTuple;
import com.soloproductions.wade.metadata.StaticDatasetMetadata;
import com.soloproductions.wade.service.DatasetService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base DTO for dataset API requests. This class contains parameters shared by read, 
 * write, metadata, export, and SPARQL-related requests. Subclasses add operation-specific fields as needed.
 */
public abstract class AbstractDatasetRequest
{
    /**
     * Sort direction requested by the client. Accepted values are resolved through
     * {@link AbstractDatasetData#resolveSortDirectionFromString(String)}.
     */
    private String sortDirection;

    /**
     * Target dataset name.
     */
    private String datasetName;

    /**
     * Data source/persistence type for the request ({@code json}, {@code rdf}, or {@code sql}).
     */
    private String dataType;

    /**
     * Dataset shape/type to query ({@code heatmap} or {@code clusters}).
     */
    private String datasetType;

    /**
     * Maximum number of rows/items to return for paginated operations.
     */
    private Integer range;

    /**
     * Zero-based starting offset for paginated operations.
     */
    private Integer rangeStart;

    /**
     * Operation type, typically mapped from HTTP verb semantics: GET, POST, PUT, DELETE.
     */
    private String requestType;

    /**
     * Runs common validation for request parameters.
     *
     * @return  {@code true} when validation succeeds
     *
     * @throws  IllegalArgumentException
     *          when one or more parameters are invalid
     */
    public boolean validateParams()
    {
        validateDatasetName();
        validateDataType();
        validateDatasetType();
        validateSortType();
//        DatasetType dst = AbstractDatasetData.resolveDatasetTypeFromString(getDatasetType());
//        DataType dt = AbstractDatasetData.resolveDataTypeFromString(getDataType());
//        if (DatasetService.existsDatasetData(getDatasetName(), dst, dt))
//        {
//
//        }
        AbstractDatasetData.handleRanges(this, null, range, rangeStart, null);
        return true;
    }

    /**
     * Validates that {@link #datasetName} references either a built-in dataset or an existing user dataset.
     *
     * @throws  IllegalArgumentException
     *          when the dataset does not exist
     */
    public void validateDatasetName()
    {
        String datasetName = getDatasetName();
        if (datasetName == null || !datasetName.matches("[a-zA-Z0-9_\\-]{1,100}"))
        {
            throw new IllegalArgumentException(
                "Dataset name must contain only letters, digits, hyphens, or underscores (1–100 characters).");
        }
        Datasets dataset = AbstractDatasetData.resolveDatasetFromString(datasetName);
        if (dataset == null)
        {
            if (!DatasetService.containsDataset(datasetName))
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("The dataset name \"%s\" does not exist! ", datasetName));
                sb.append("Available options are: ");
                Set<String> datasetSet = new TreeSet<>(List.of(DatasetService.getDatasetNames()));
                sb.append(datasetSet);
                sb.append(".");
                throw new IllegalArgumentException(sb.toString());
            }
        }
    }

    /**
     * Validates that {@link #dataType} is supported and available for the selected dataset.
     * For built-in datasets, this also checks metadata-defined model availability.
     *
     * @throws  IllegalArgumentException
     *          when the data type is invalid or unavailable
     */
    public void validateDataType()
    {
        String dataType = getDataType();
        DataType dt = AbstractDatasetData.resolveDataTypeFromString(dataType);
        DatasetType dst = AbstractDatasetData.resolveDatasetTypeFromString(getDatasetType());
        String datasetName = getDatasetName();
        if (dt == null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("The data type \"%s\" is invalid.", dataType));
            sb.append(" Must either be: ");
            sb.append(AbstractDatasetData.DATA_TYPES);
            sb.append(".");
            throw new IllegalArgumentException(sb.toString());
        }

        // all dataset data instances are lazily instantiated, but because there are already existing datasets this check will
        // not work, skip it for those cases
        boolean isDefaultDataset = AbstractDatasetData.isDefaultDataset(getDatasetName());
        if (!AbstractDatasetRequest.isRequestType(this, DatasetRequestType.CREATE) && !isDefaultDataset)
        {
            validateDatasetType();
            Set<String> dTypes = DatasetService.getDatasetDataTypes(datasetName, dst);
            boolean found = false;
            for (String type : dTypes)
            {
                if (type.equalsIgnoreCase(dataType))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                String message = String.format("Tried to read/update/delete from dataset with missing data type \"%s\"! ",
                                               dataType);
                StringBuilder available = new StringBuilder();
                if (dTypes.isEmpty())
                {
                    available.append("There are no available types (maybe the dataset wasn't created?");
                }
                else
                {
                    available.append("Available types are: ");
                    for (String type : dTypes)
                    {
                        available.append(type).append(" ");
                    }
                }
                message += available;
                throw new IllegalArgumentException(message);
            }
        }
        if (AbstractDatasetData.isDefaultDataset(datasetName))
        {
            DatasetMetadata dm = new DatasetMetadata();
            try
            {
                AbstractDatasetData.setMetadataStatic(dm);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load static dataset metadata for validation!");
            }

            StaticDatasetMetadata sdm = dm.getStaticMetadata().get(datasetName);
            if (sdm != null)
            {
                List<FeatureTuple> models = sdm.getDataModels();
                List<String> strModels = new ArrayList<>();
                if (models != null)
                {
                    for (FeatureTuple ft : models)
                    {
                        if (ft != null && ft.getName() != null)
                        {
                            strModels.add(ft.getName());
                        }
                    }
                }
                else
                {
                    if (sdm.getDatasetInfo() != null)
                    {
                        var map = sdm.getDatasetInfo().getDatasetTypeAndDataTypes();
                        if (map != null)
                        {
                            Set<String> candidates = map.get(getDatasetType());
                            if (candidates == null)
                            {
                                candidates = map.get(getDatasetType() + "s");
                            }
                            if (candidates == null)
                            {
                                candidates = new java.util.HashSet<>();
                                for (Set<String> s : map.values())
                                {
                                    if (s != null) candidates.addAll(s);
                                }
                            }
                            if (candidates != null)
                            {
                                strModels.addAll(candidates);
                            }
                        }
                    }
                }

                boolean foundModel = false;
                for (String m : strModels)
                {
                    if (m != null && m.equalsIgnoreCase(getDataType()))
                    {
                        foundModel = true;
                        break;
                    }
                }

                if (!foundModel)
                {
                    StringBuilder message = new StringBuilder("This dataset does not contain the provided data type ")
                                                              .append(dataType)
                                                              .append("! ");
                    message.append("Available models are: ");
                    for (String m : strModels)
                    {
                        message.append(m).append(" ");
                    }
                    throw new IllegalArgumentException(message.toString());
                }
            }
        }
    }

    /**
     * Validates that {@link #datasetType} is supported and available for the selected dataset.
     *
     * @throws  IllegalArgumentException
     *          when the dataset type is invalid or unavailable
     */
    public void validateDatasetType()
    {
        String datasetType = getDatasetType();
        DatasetType dst = AbstractDatasetData.resolveDatasetTypeFromString(datasetType);
        if (dst == null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("The dataset type \"%s\" is invalid!", datasetType));
            sb.append(" Must either be: ");
            sb.append(AbstractDatasetData.DATASET_TYPES);
            sb.append(".");
            throw new IllegalArgumentException(sb.toString());
        }

        // all dataset data instances are lazily instantiated, but because there are already existing datasets this check will
        // not work, skip it for those cases
        boolean isDefaultDataset = AbstractDatasetData.isDefaultDataset(getDatasetName());
        if (!AbstractDatasetRequest.isRequestType(this, DatasetRequestType.CREATE) && !isDefaultDataset)
        {
            Set<DatasetType> dTypes = DatasetService.getDatasetTypes(getDatasetName());
            if (!dTypes.contains(dst))
            {
                String message = String.format("Tried to read/update/delete from dataset type \"%s\" that was not created! ",
                                               dst.name());
                StringBuilder available = new StringBuilder();
                if (dTypes.isEmpty())
                {
                    available.append("There are no dataset types declared for this dataset.");
                }
                else
                {
                    available = new StringBuilder("Available types for this dataset are: ");
                    for (DatasetType type : dTypes)
                    {
                        available.append(type).append(" ");
                    }
                }
                message = message + available;
                throw new IllegalArgumentException(message);
            }
        }
    }

    /**
     * Validates that {@link #sortDirection} is supported for this request.
     *
     * @throws  IllegalArgumentException
     *          when the sort direction is invalid
     */
    public void validateSortType()
    {
        String sortDirection = getSortDirection();
        if (sortDirection == null || sortDirection.isEmpty())
        {
            return;
        }
        SortDirection sd = AbstractDatasetData.resolveSortDirectionFromString(sortDirection);
        if (sd == null && !isMetadataRequest())
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("The sort direction %s is invalid.", sortDirection));
            sb.append(" Must either be: ");
            sb.append(AbstractDatasetData.SORT_DIRECTIONS);
            sb.append(" but was ");
            sb.append(sortDirection);
            sb.append("!");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Utility helper to compare a request's operation against a {@link DatasetRequestType}.
     *
     * @param   adr
     *          request to inspect
     * @param   drt
     *          expected dataset request type
     *
     * @return  {@code true} when request type matches the expected value
     */
    public static boolean isRequestType(AbstractDatasetRequest adr, DatasetRequestType drt)
    {
        return AbstractDatasetData.resolveRequestType(adr.getRequestType()) == drt;
    }

    /**
     * Returns the request type.
     *
     * @return  request type string
     */
    public String getRequestType()
    {
        return requestType;
    }

    /**
     * Sets the request type.
     *
     * @param   requestType
     *          request type string
     */
    public void setRequestType(String requestType)
    {
        this.requestType = requestType;
    }

    /**
     * Returns the requested sort direction.
     *
     * @return  sort direction string
     */
    public String getSortDirection()
    {
        return sortDirection;
    }

    /**
     * Sets the requested sort direction.
     *
     * @param   sortDirection
     *          sort direction string
     */
    public void setSortDirection(String sortDirection)
    {
        this.sortDirection = sortDirection;
    }

    /**
     * Returns the dataset name.
     *
     * @return  dataset name
     */
    public String getDatasetName()
    {
        return datasetName;
    }

    /**
     * Sets the dataset name.
     *
     * @param   datasetName
     *          dataset name
     */
    public void setDatasetName(String datasetName)
    {
        this.datasetName = datasetName;
    }

    /**
     * Returns the data type.
     *
     * @return  data type string
     */
    public String getDataType()
    {
        return dataType;
    }

    /**
     * Sets the data type.
     *
     * @param   dataType
     *          data type string
     */
    public void setDataType(String dataType)
    {
        this.dataType = dataType;
    }

    /**
     * Returns the dataset type.
     *
     * @return  dataset type string
     */
    public String getDatasetType()
    {
        return datasetType;
    }

    /**
     * Sets the dataset type.
     *
     * @param   datasetType
     *          dataset type string
     */
    public void setDatasetType(String datasetType)
    {
        this.datasetType = datasetType;
    }

    /**
     * Indicates whether this request is a metadata request.
     *
     * @return  {@code true} if metadata request, otherwise {@code false}
     */
    public boolean isMetadataRequest()
    {
        return false;
    }

    /**
     * Indicates whether this request is a similarity request.
     *
     * @return  {@code true} if similarity request, otherwise {@code false}
     */
    public boolean isSimilarityRequest()
    {
        return false;
    }

    /**
     * Indicates whether this request is a SPARQL query request.
     *
     * @return  {@code true} if SPARQL request, otherwise {@code false}
     */
    public boolean isSparqlQueryRequest()
    {
        return false;
    }

    /**
     * Indicates whether this request is a clusters request.
     *
     * @return  {@code true} if clusters request, otherwise {@code false}
     */
    public boolean isClustersRequest()
    {
        return false;
    }

    /**
     * Indicates whether this request is an export request.
     *
     * @return  {@code true} if export request, otherwise {@code false}
     */
    public boolean isExportRequest()
    {
        return false;
    }

    /**
     * Indicates whether this request targets the dataset-metadata persistence endpoint.
     *
     * @return  {@code true} if dataset-metadata request, otherwise {@code false}
     */
    public boolean isDatasetMetadataRequest()
    {
        return false;
    }

    /**
     * Returns the requested range size for paginated operations.
     *
     * @return  range size
     */
    public Integer getRange()
    {
        return range;
    }

    /**
     * Sets the requested range size for paginated operations.
     *
     * @param   range
     *          range size
     */
    public void setRange(Integer range)
    {
        this.range = range;
    }

    /**
     * Returns the requested zero-based range start for paginated operations.
     *
     * @return  range start offset
     */
    public Integer getRangeStart()
    {
        return rangeStart;
    }

    /**
     * Sets the requested zero-based range start for paginated operations.
     *
     * @param   rangeStart
     *          range start offset
     */
    public void setRangeStart(Integer rangeStart)
    {
        this.rangeStart = rangeStart;
    }
}

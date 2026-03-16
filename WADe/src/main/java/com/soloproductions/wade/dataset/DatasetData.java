package com.soloproductions.wade.dataset;

import com.soloproductions.wade.datatype.DataTypeController;
import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.ExportRequest;
import com.soloproductions.wade.metadata.DatasetMetadata;

import java.io.IOException;

/**
 * Interface for all dataset model types. Implementations provide access to
 * the underlying data, define their dataset category, and delegate query
 * execution to the appropriate {@link DataTypeController}.
 */
public interface DatasetData
{
    /**
     * Returns dataset type (cluster/heatmap).
     *
     * @return  dataset type
     */
    public DatasetType getDatasetType();

    /**
     * Executes the given request against the dataset and returns the result.
     *
     * @param   adr
     *          request to execute
     *
     * @return  result produced by the data type controller
     */
    public Object executeRequest(AbstractDatasetRequest adr);

    /**
     * Applies filter and pagination values from the request onto this dataset instance.
     *
     * @param   adr
     *          request whose values should be applied
     */
    public void applyFilters(AbstractDatasetRequest adr);

    /**
     * Returns the controller responsible for the dataset's current data type.
     *
     * @return  active data type controller
     */
    public DataTypeController getDataTypeController();

    /**
     * Loads and returns metadata for this dataset.
     *
     * @return  dataset metadata
     *
     * @throws  IOException
     *          when metadata sources cannot be read
     */
    public DatasetMetadata getMetadata() throws IOException;

    /**
     * Returns the name that identifies this dataset.
     *
     * @return  dataset name
     */
    public String getDatasetName();

    /**
     * Returns the requested number of items to return in a paginated response.
     * Note: all requests are paginated, but if not provided the range is defaulted
     * to a specific value. See {@link AbstractDatasetData#DEFAULT_RANGE} for details.
     *
     * @return  range size
     */
    public Integer getRange();

    /**
     * Returns the starting offset for paginated access.
     * Note: all requests are paginated, but if not provided the range start is defaulted
     * to a specific value. See {@link AbstractDatasetData#DEFAULT_RANGE_START} for details.
     *
     * @return  range start offset
     */
    public Integer getRangeStart();

    /**
     * Sets the requested number of items to return in a paginated response.
     *
     * @param   range
     *          range size to store
     */
    public void setRange(Integer range);

    /**
     * Sets the starting offset for paginated access.
     *
     * @param   rangeStart
     *          starting offset to store
     */
    public void setRangeStart(Integer rangeStart);

    /**
     * Returns the persistence or data-source type used by this dataset.
     *
     * @return  current data type
     */
    public DataType getDataType();

    /**
     * Returns a pending SPARQL query associated with this dataset.
     *
     * @return  pending SPARQL query text
     */
    public String getPendingQuery();

    /**
     * Sets a pending SPARQL query on this dataset.
     *
     * @param   query
     *          SPARQL query text to store
     */
    public void setPendingQuery(String query);

    /**
     * Returns whether the last SPARQL query returned raw results without mapping.
     *
     * @return  {@code true} when raw results should be preserved
     */
    public boolean isRawResults();

    /**
     * Exports dataset data using the resolved data type controller.
     *
     * @param   exportRequest
     *          export configuration
     *
     * @return  exported data payload
     */
    public Object exportData(ExportRequest exportRequest);
}

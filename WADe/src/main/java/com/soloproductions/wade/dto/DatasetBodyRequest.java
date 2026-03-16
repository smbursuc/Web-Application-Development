package com.soloproductions.wade.dto;

import com.soloproductions.wade.pojo.DataEntry;

/**
 * Base DTO for requests that carry a request body payload. Used for 
 * create/update/delete operations where the payload is represented by
 * {@link DataEntry}.
 */
public abstract class DatasetBodyRequest extends AbstractDatasetRequest
{
    /**
     * Request payload wrapper containing operation-specific data.
     */
    private DataEntry entry;

    /**
     * Returns the request payload entry.
     *
     * @return  payload wrapper
     */
    public DataEntry getEntry()
    {
        return entry;
    }

    /**
     * Sets the request payload entry.
     *
     * @param   entry
     *          payload wrapper
     */
    public void setEntry(DataEntry entry)
    {
        this.entry = entry;
    }

    /**
     * Validates common parameters for body-based dataset requests.
     *
     * @return  {@code true} when validation succeeds
     */
    public boolean validateParams()
    {
        validateDataType();
        validateDatasetType();
        return true;
    }

    /**
     * Marks this DTO as a data-definition-like body request.
     *
     * @return  always {@code true}
     */
    public boolean isDdlRequest()
    {
        return true;
    }
}

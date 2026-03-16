package com.soloproductions.wade.dto;

/**
 * DTO for HTTP POST dataset operations. POST is used both for creating a brand-new 
 * dataset and for inserting entries into an existing dataset.
 */
public class DatasetPostRequest extends DatasetBodyRequest
{
    /**
     * Indicates whether this POST request creates a new dataset definition
     * ({@code true}) or inserts data into an existing dataset ({@code false}).
     */
    private boolean isCreation;

    /**
     * Returns whether this request is a dataset-creation request.
     *
     * @return  {@code true} for dataset creation, {@code false} for data insertion
     */
    public boolean isCreation()
    {
        return isCreation;
    }

    /**
     * Sets whether this request targets dataset creation.
     *
     * @param   creation
     *          {@code true} for dataset creation, {@code false} for data insertion
     */
    public void setCreation(boolean creation)
    {
        isCreation = creation;
    }
}

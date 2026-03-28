package com.soloproductions.wade.dto;

/**
 * DTO for the {@code GET /api/dataset-metadata/{datasetName}} and
 * {@code PUT /api/dataset-metadata/{datasetName}} endpoints.
 *
 * <p>Only {@code datasetName} and {@code requestType} are required.  The two
 * optional payload fields ({@code summary}, {@code source})
 * are populated for PUT requests.</p>
 */
public class DatasetMetadataRequest extends AbstractDatasetRequest
{
    /** Free-text description / summary for the dataset (nullable). */
    private String summary;

    /** Source reference for the dataset (nullable). */
    private String source;

    /**
     * Indicates whether this request targets the dataset-metadata persistence endpoint.
     *
     * @return {@code true} — this is always a dataset-metadata request
     */
    @Override
    public boolean isDatasetMetadataRequest()
    {
        return true;
    }

    /**
     * Validates only the dataset name; other standard fields (dataType,
     * datasetType, sortType, ranges) are not applicable for this request.
     *
     * @return  {@code true} when validation passes
     *
     * @throws  IllegalArgumentException when the dataset name is invalid
     */
    @Override
    public boolean validateParams()
    {
        validateDatasetName();
        return true;
    }

    /**
     * Returns a free-text summary of the dataset.
     *
     * @return   dataset summary (nullable)
     */
    public String getSummary()
    {
        return summary;
    }

    /**
     * Sets the dataset summary.
     *
     * @param   summary 
     *          dataset summary text; may be null
     */
    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    /**
     * Returns the dataset source reference.
     *
     * @return  source reference (nullable)
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Sets the dataset source reference.
     *
     * @param   source 
     *          source reference text; may be null
     */
    public void setSource(String source)
    {
        this.source = source;
    }
}

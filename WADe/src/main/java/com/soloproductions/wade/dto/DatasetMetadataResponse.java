package com.soloproductions.wade.dto;

/**
 * DTO for the response of {@code GET /api/dataset-metadata/{datasetName}}
 * and {@code PUT /api/dataset-metadata/{datasetName}} endpoints.
 *
 * <p>Contains the persisted metadata fields for a dynamically-created dataset.</p>
 */
public class DatasetMetadataResponse
{
    /** Free-text description / summary for the dataset. */
    private String summary;

    /** Source reference for the dataset. */
    private String source;

    /**
     * Default constructor.
     */
    public DatasetMetadataResponse()
    {
    }

    /**
     * Constructor with all fields.
     *
     * @param   summary        
     *          description
     * @param   source         
     *          source reference
     */
    public DatasetMetadataResponse(String summary, String source)
    {
        this.summary = summary != null ? summary : "";
        this.source = source != null ? source : "";
    }

    /**
     * Returns the persisted dataset summary.
     *
     * @return  dataset summary (never null)
     */
    public String getSummary()
    {
        return summary;
    }

    /**
     * Sets the persisted dataset summary.
     *
     * @param   summary 
     *          dataset summary text; null is converted to empty string
     */
    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    /**
     * Returns the persisted dataset source reference.
     *
     * @return  source reference (never null)
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Sets the persisted dataset source reference.
     *
     * @param   source 
     *          source reference text; null is converted to empty string
     */
    public void setSource(String source)
    {
        this.source = source;
    }
}

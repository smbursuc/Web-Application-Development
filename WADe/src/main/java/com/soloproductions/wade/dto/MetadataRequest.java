package com.soloproductions.wade.dto;

/**
 * DTO for metadata fetch requests.
 */
public class MetadataRequest extends AbstractDatasetRequest
{
    /**
     * If {@code true}, returns general/static metadata definitions.
     * If {@code false}, returns current dataset instance specific data, such as its size.
     */
    private boolean generalInfo;

    /**
     * Marks this DTO as a metadata request.
     *
     * @return  always {@code true}
     */
    @Override
    public boolean isMetadataRequest()
    {
        return true;
    }

    /**
     * Returns whether general metadata is requested.
     *
     * @return  general metadata flag
     */
    public boolean isGeneralInfo()
    {
        return generalInfo;
    }

    /**
     * Sets whether general metadata is requested.
     *
     * @param   generalInfo
     *          general metadata flag
     */
    public void setGeneralInfo(boolean generalInfo)
    {
        this.generalInfo = generalInfo;
    }
}

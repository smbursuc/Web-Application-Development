package com.soloproductions.wade.dto;

/**
 * DTO for dataset export requests. Requests are routed to export handlers via the 
 * URL query parameter {@code export=true}. The {@code honorFilters} flag determines 
 * whether active query filters/pagination are applied to exported content.
 */
public class ExportRequest extends AbstractDatasetRequest
{
    /**
     * Flag used to distinguish read requests from export requests (as they are very similar). 
     * This parameter is a URL query parameter (not part of the JSON body) and is used to 
     * route requests to export handlers. Functionally after mapping it is of no use.
     */
    private boolean export;

    /**
     * Whether export should apply request filters (range, sort, criteria) or return full raw data.
     */
    private boolean honorFilters;

    /**
     * Returns whether this request carries the export flag.
     *
     * @return  export flag value
     */
    public boolean isExport()
    {
        return export;
    }

    /**
     * Sets the export flag value.
     *
     * @param   export
     *          export flag value
     */
    public void setExport(boolean export)
    {
        this.export = export;
    }

    /**
     * Returns whether export should honor active filters.
     *
     * @return  {@code true} to export filtered data, {@code false} for full/raw export
     */
    public boolean isHonorFilters()
    {
        return honorFilters;
    }

    /**
     * Sets whether export should honor active filters.
     *
     * @param   honorFilters
     *          {@code true} to export filtered data, {@code false} for full/raw export
     */
    public void setHonorFilters(boolean honorFilters)
    {
        this.honorFilters = honorFilters;
    }

    /**
     * Marks this DTO as an export request type.
     *
     * @return  always {@code true}
     */
    @Override
    public boolean isExportRequest()
    {
        return true;
    }
}

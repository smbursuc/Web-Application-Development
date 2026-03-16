package com.soloproductions.wade.dto;

/**
 * Request DTO for reading cluster datasets. Extends shared request fields from 
 * {@link AbstractDatasetRequest} and adds an optional {@code clusterName} filter 
 * used to target a specific cluster.
 */
public class ClusterDatasetRequest extends AbstractDatasetRequest
{
    /**
     * Optional cluster name filter. When provided, read operations can return a 
     * specific cluster subtree instead of the full root-level cluster list.
     */
    private String clusterName;

    /**
     * Validates this request using base dataset request validation.
     *
     * @return  {@code true} when validation succeeds
     */
    public boolean validateParams()
    {
        // do nothing extra?
        return super.validateParams();
    }

    /**
     * Marks this request as a clusters request.
     *
     * @return  always {@code true}
     */
    @Override
    public boolean isClustersRequest()
    {
        return true;
    }

    /**
     * Returns the optional cluster name filter.
     *
     * @return  cluster name, or {@code null} when no specific cluster is requested
     */
    public String getClusterName()
    {
        return clusterName;
    }

    /**
     * Sets the optional cluster name filter.
     *
     * @param   clusterName
     *          cluster name to target for read operations
     */
    public void setClusterName(String clusterName)
    {
        this.clusterName = clusterName;
    }
}

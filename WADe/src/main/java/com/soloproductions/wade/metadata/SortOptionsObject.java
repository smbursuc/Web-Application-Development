package com.soloproductions.wade.metadata;

import java.util.List;

/**
 * Configuration object for available sort options in metadata responses.
 */
public class SortOptionsObject
{
    /** Common sort options shared across dataset representations. */
    private List<FeatureTuple> common;

    /** Heatmap-specific sort options. */
    private List<FeatureTuple> heatmaps;

    /**
     * Returns common sort options.
     *
     * @return  list of common sort options
     */
    public List<FeatureTuple> getCommon()
    {
        return common;
    }

    /**
     * Sets common sort options.
     *
     * @param   common 
     *          list of common sort options
     */
    public void setCommon(List<FeatureTuple> common)
    {
        this.common = common;
    }

    /**
     * Returns heatmap-specific sort options.
     *
     * @return   list of heatmap sort options
     */
    public List<FeatureTuple> getHeatmaps()
    {
        return heatmaps;
    }

    /**
     * Sets heatmap-specific sort options.
     *
     * @param   heatmaps 
     *          list of heatmap sort options
     */
    public void setHeatmaps(List<FeatureTuple> heatmaps)
    {
        this.heatmaps = heatmaps;
    }
}

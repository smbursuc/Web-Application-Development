package com.soloproductions.wade.pojo;

import com.soloproductions.wade.dataset.DataEntity;

/**
 * POJO representation of a heatmap payload.
 */
public class HeatmapData implements DataEntity
{
    /** Ordered object labels used for matrix rows and columns. */
    private String[] objects;

    /** Pairwise similarity matrix aligned with {@link #objects}. */
    private double[][] matrix;

    /**
     * Returns object labels used by this heatmap.
     *
     * @return  object labels
     */
    public String[] getObjects()
    {
        return objects;
    }

    /**
     * Sets object labels used by this heatmap.
     *
     * @param   objects
     *          object labels
     */
    public void setObjects(String[] objects)
    {
        this.objects = objects;
    }

    /**
     * Returns the pairwise similarity matrix.
     *
     * @return  similarity matrix
     */
    public double[][] getMatrix()
    {
        return matrix;
    }

    /**
     * Sets the pairwise similarity matrix.
     *
     * @param   matrix
     *          similarity matrix
     */
    public void setMatrix(double[][] matrix)
    {
        this.matrix = matrix;
    }
}

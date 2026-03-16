package com.soloproductions.wade.dto;

import com.soloproductions.wade.dataset.DataEntity;
import java.util.List;

/**
 * DTO for representing a heatmap dataset, which includes a list of objects and a corresponding similarity matrix.
 */
public class SimilarityDatasetDTO implements DataEntity
{
    /** List of objects. */
    private List<String> objects;

    /** Similarity matrix. */
    private List<List<Double>> matrix;

    /**
     * Gets the list of objects.
     * 
     * @return   List of objects.
     */
    public List<String> getObjects()
    {
        return objects;
    }

    /**
     * Sets the list of objects.
     *
     * @param   objects
     *          List of objects to set.
     */
    public void setObjects(List<String> objects)
    {
        this.objects = objects;
    }

    /**
     * Gets the similarity matrix.
     *
     * @return   Similarity matrix.
     */
    public List<List<Double>> getMatrix()
    {
        return matrix;
    }

    /**
     * Sets the similarity matrix.
     *
     * @param   matrix
     *          Similarity matrix to set.
     */
    public void setMatrix(List<List<Double>> matrix)
    {
        this.matrix = matrix;
    }
}

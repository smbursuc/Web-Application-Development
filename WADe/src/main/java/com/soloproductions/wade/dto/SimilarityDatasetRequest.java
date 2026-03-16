package com.soloproductions.wade.dto;

import com.soloproductions.wade.dataset.AbstractDatasetData;
import com.soloproductions.wade.dataset.SimilaritySortCriteria;

/**
 * Represents a read request for a heatmap dataset.
 */
public class SimilarityDatasetRequest extends AbstractDatasetRequest
{
    /** Similarity sort criteria. See {@link SimilaritySortCriteria}. */
    private String similaritySortCriteria;

    /**
     * Validates the parameters extracted from the request.
     * 
     * @return   true if the parameters are valid, false otherwise
     * 
     * @throws   IllegalArgumentException if the similarity sort criteria is invalid
     */
    public boolean validateParams()
    {
        String similaritySortCriteria = getSimilaritySortCriteria();
        SimilaritySortCriteria ssc = AbstractDatasetData.resolveSimilaritySortCriteriaFromString(similaritySortCriteria);
        if (ssc == null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("The sort criteria %s is invalid", similaritySortCriteria));
            sb.append(" Must either be: ");
            sb.append(AbstractDatasetData.SIMILARITY_SORT_CRITERIAS);
            sb.append(".");
            throw new IllegalArgumentException("This sort criteria does not exist. Choose highest_probability or strongest_pair.");
        }
        return super.validateParams();
    }

    /**
     * Gets the similarity sort criteria.
     * 
     * @return   similarity sort criteria as a string
     */
    public String getSimilaritySortCriteria()
    {
        return similaritySortCriteria;
    }

    /**
     * Sets the similarity sort criteria.
     *
     * @param   similaritySortCriteria
     *          Similarity sort criteria to set.
     */
    public void setSimilaritySortCriteria(String similaritySortCriteria)
    {
        this.similaritySortCriteria = similaritySortCriteria;
    }
}

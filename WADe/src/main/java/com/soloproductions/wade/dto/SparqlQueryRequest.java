package com.soloproductions.wade.dto;

import com.soloproductions.wade.dataset.SimilarityDataset;
import com.soloproductions.wade.dataset.ClusterNode;

/**
 * Represents a request for executing a SPARQL query. This is related to the "Playgroud" mode, 
 * where the user inputs a string and the system executes it as a SPARQL query.
 */
public class SparqlQueryRequest extends AbstractDatasetRequest
{
    /** The unfiltered raw query received from the request. */
    private String query;

    /** 
     * Indicates whether the raw results should be returned. This removes any mapping of the result set
     * to corresponding objects such as {@link ClusterNode} and {@link SimilarityDataset}.
     */
    private boolean rawResults;

    /**
     * Gets the SPARQL query string.
     * 
     * @return   the SPARQL query string
     */
    public String getQuery()
    {
        return query;
    }

    /**
     * Sets the SPARQL query string.
     * 
     * @param query   the SPARQL query string to set
     */
    public void setQuery(String query)
    {
        this.query = query;
    }

    /**
     * Checks if raw results should be returned. See {@link #rawResults}.
     * 
     * @return   true if raw results should be returned, false otherwise
     */
    public boolean isRawResults()
    {
        return rawResults;
    }

    /**
     * Sets whether raw results should be returned. See {@link #rawResults}.
     * 
     * @param rawResults   true if raw results should be returned, false otherwise
     */
    public void setRawResults(boolean rawResults)
    {
        this.rawResults = rawResults;
    }

    /**
     * Indicates that this request is a SPARQL query request.
     * 
     * @return   true if this is a SPARQL query request, false otherwise
     */
    @Override
    public boolean isSparqlQueryRequest()
    {
        return true;
    }

    /**
     * Validates the parameters for this request. For now, DDL operation are not supported.
     * 
     * @return   true if the parameters are valid, false otherwise
     */
    @Override
    public boolean validateParams()
    {
        validateDatasetName();
        if (isDmlDdlQuery())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("DDL operations are not allowed! Detected the usage of UPDATE/DELETE keywords!.");
            throw new IllegalArgumentException(sb.toString());
        }

        return true;
    }

    /**
     * Checks if the query contains DML/DDL keywords.
     * 
     * @return   true if the query contains DML/DDL keywords, false otherwise
     */
    public boolean isDmlDdlQuery()
    {
        String query = getQuery();
        if (query == null || query.isBlank())
        {
            return false;
        }

        String[] ddlKeywords = {"update", "delete", "insert", "create"};
        String normalized = " " + query.toLowerCase();

        for (String keyword : ddlKeywords)
        {
            if (normalized.contains(" " + keyword + " "))
            {
                return true;
            }
        }
        return false;
    }

}

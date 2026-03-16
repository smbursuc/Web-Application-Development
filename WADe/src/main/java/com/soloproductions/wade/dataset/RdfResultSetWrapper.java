package com.soloproductions.wade.dataset;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around a raw SPARQL result set returned by an RDF query.
 * Each entry in the list corresponds to one solution row, where the key is
 * the variable name and the value is its bound string representation.
 */
public class RdfResultSetWrapper implements DataEntity
{
    /** Raw SPARQL result rows; each map entry is a variable-name-to-string-value binding. */
    private List<Map<String, String>> resultSet;

    /**
     * Returns the raw SPARQL result set.
     *
     * @return  list of solution rows
     */
    public List<Map<String, String>> getResultSet()
    {
        return resultSet;
    }

    /**
     * Sets the raw SPARQL result set.
     *
     * @param   resultSet
     *          list of solution rows to store
     */
    public void setResultSet(List<Map<String, String>> resultSet)
    {
        this.resultSet = resultSet;
    }
}

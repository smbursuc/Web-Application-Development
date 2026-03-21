package com.soloproductions.wade.metadata;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Metadata structure describing which data types are available for each dataset type.
 */
public class DatasetInfo
{
    /**
     * Mapping from dataset type names (for example {@code heatmap}, {@code clusters})
     * to available backing data types (for example {@code RDF}, {@code JSON}, {@code SQL}).
     */
    private Map<String, Set<String>> datasetTypeAndDataTypes = new LinkedHashMap<>();

    /**
     * Returns the dataset type to data type mapping.
     *
     * @return mapping of dataset type to available data types
     */
    public Map<String, Set<String>> getDatasetTypeAndDataTypes()
    {
        return datasetTypeAndDataTypes;
    }

    /**
     * Sets the dataset type to data type mapping.
     *
     * @param   datasetTypeAndDataTypes 
     *          mapping of dataset type to available data types
     */
    public void setDatasetTypeAndDataTypes(Map<String, Set<String>> datasetTypeAndDataTypes)
    {
        this.datasetTypeAndDataTypes = datasetTypeAndDataTypes;
    }

    /**
     * Captures dynamic JSON properties during deserialization and merges them into
     * {@link #datasetTypeAndDataTypes}.
     *
     * @param   name 
     *          dataset type key from JSON
     * @param   value 
     *          collection of supported data types for the dataset type
     */
    @JsonAnySetter
    public void addDatasetType(String name, Object value)
    {
        if (value == null) 
        {
            return;
        }
        
        if (value instanceof Collection)
        {
            Set<String> set = new LinkedHashSet<>();
            for (Object o : (Collection<?>) value)
            {
                if (o != null) set.add(o.toString());
            }
            datasetTypeAndDataTypes.put(name, set);
        }
    }
}

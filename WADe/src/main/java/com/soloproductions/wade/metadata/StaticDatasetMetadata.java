package com.soloproductions.wade.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Static metadata block for one dataset as loaded from the metadata configuration file.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StaticDatasetMetadata
{
    /** Informational features shown for the dataset. */
    private List<FeatureObject> features;

    /** Readable dataset name. */
    private String displayValue;

    /** Short dataset summary text. */
    private String summary;

    /** Dataset source URL. */
    private String source;

    /** Supported data model options. */
    private List<FeatureTuple> dataModels;

    /** Supported dataset representation types. */
    private List<FeatureTuple> datasetTypes;

    /** Sort options available for this dataset. */
    private SortOptionsObject sortOptions;

    /** Mapping from representation to available data types. */
    private DatasetInfo datasetInfo;

    /**
     * Returns dataset feature entries.
     *
     * @return  list of feature objects
     */
    public List<FeatureObject> getFeatures()
    {
        return features;
    }

    /**
     * Sets dataset feature entries.
     *
     * @param   features 
     *          list of feature objects
     */
    public void setFeatures(List<FeatureObject> features)
    {
        this.features = features;
    }

    /**
     * Returns the human-readable dataset display value.
     *
     * @return  dataset display value
     */
    public String getDisplayValue()
    {
        return displayValue;
    }

    /**
     * Sets the human-readable dataset display value.
     *
     * @param   displayValue 
     *          dataset display value
     */
    public void setDisplayValue(String displayValue)
    {
        this.displayValue = displayValue;
    }

    /**
     * Returns the dataset summary.
     *
     * @return  summary text
     */
    public String getSummary()
    {
        return summary;
    }

    /**
     * Sets the dataset summary.
     *
     * @param   summary 
     *          summary text
     */
    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    /**
     * Returns the dataset source URL.
     *
     * @return  source URL
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Sets the dataset source URL.
     *
     * @param   source 
     *          source URL
     */
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * Returns supported data model options.
     *
     * @return  data model options
     */
    public List<FeatureTuple> getDataModels()
    {
        return dataModels;
    }

    /**
     * Sets supported data model options.
     *
     * @param   dataModels 
     *          data model options
     */
    public void setDataModels(List<FeatureTuple> dataModels)
    {
        this.dataModels = dataModels;
    }

    /**
     * Returns sort options for this dataset.
     *
     * @return  sort options
     */
    public SortOptionsObject getSortOptions()
    {
        return sortOptions;
    }

    /**
     * Sets sort options for this dataset.
     *
     * @param   sortOptions 
     *          sort options
     */
    public void setSortOptions(SortOptionsObject sortOptions)
    {
        this.sortOptions = sortOptions;
    }

    /**
     * Returns dataset-type to data-type mapping information.
     *
     * @return  dataset info mapping
     */
    public DatasetInfo getDatasetInfo()
    {
        return datasetInfo;
    }

    /**
     * Sets dataset-type to data-type mapping information.
     *
     * @param   datasetInfo 
     *          dataset info mapping
     */
    public void setDatasetInfo(DatasetInfo datasetInfo)
    {
        this.datasetInfo = datasetInfo;
    }

    /**
     * Returns supported dataset representation types.
     *
     * @return  dataset representation types
     */
    public List<FeatureTuple> getDatasetTypes()
    {
        return datasetTypes;
    }

    /**
     * Sets supported dataset representation types.
     *
     * @param   datasetTypes 
     *          dataset representation types
     */
    public void setDatasetTypes(List<FeatureTuple> datasetTypes)
    {
        this.datasetTypes = datasetTypes;
    }
}

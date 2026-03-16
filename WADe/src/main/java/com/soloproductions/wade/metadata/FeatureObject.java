package com.soloproductions.wade.metadata;

/**
 * Metadata entry containing a title and a descriptive text block.
 */
public class FeatureObject
{
    /** Feature title shown in the dataset information panel. */
    private String title;

    /** Feature description text. */
    private String description;

    /**
     * Returns the feature title.
     *
     * @return  feature title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Sets the feature title.
     *
        * @param   title
     *          feature title
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Returns the feature description.
     *
        * @return  feature description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the feature description.
     *
        * @param   description
     *          feature description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
}

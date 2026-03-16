package com.soloproductions.wade.metadata;

/**
 * Metadata tuple with internal name and user-facing display value.
 */
public class FeatureTuple
{
    /** Internal identifier used in requests/configuration. */
    private String name;

    /** User-seen label shown in UI controls. */
    private String displayValue;

    /**
     * Returns the internal name.
     *
     * @return  internal feature name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the internal name.
     *
        * @param   name
     *          internal feature name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the display value.
     *
     * @return  user-seen display value
     */
    public String getDisplayValue()
    {
        return displayValue;
    }

    /**
     * Sets the display value.
     *
        * @param   displayValue
     *          user-seen display value
     */
    public void setDisplayValue(String displayValue)
    {
        this.displayValue = displayValue;
    }
}

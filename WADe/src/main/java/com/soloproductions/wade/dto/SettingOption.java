package com.soloproductions.wade.dto;

/**
 * Represents a single setting option descriptor consumed by the Settings page UI.
 */
public class SettingOption
{
    private String key;
    private String label;
    private String type;
    private String description;
    private String placeholder;
    private Boolean sensitive;

    /**
     * Gets the unique settings key.
     *
     * @return the option key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Sets the unique settings key.
     *
     * @param key the option key
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * Gets the display label shown in the UI.
     *
     * @return the display label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Sets the display label shown in the UI.
     *
     * @param label the display label
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * Gets the option type (for example, boolean or string).
     *
     * @return the option type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the option type (for example, boolean or string).
     *
     * @param type the option type
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Gets the descriptive text for this option.
     *
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the descriptive text for this option.
     *
     * @param description the description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Gets the optional placeholder text for string inputs.
     *
     * @return the placeholder text
     */
    public String getPlaceholder()
    {
        return placeholder;
    }

    /**
     * Sets the optional placeholder text for string inputs.
     *
     * @param placeholder the placeholder text
     */
    public void setPlaceholder(String placeholder)
    {
        this.placeholder = placeholder;
    }

    /**
     * Gets whether the value should be treated as sensitive in the UI.
     *
     * @return true if sensitive, false otherwise
     */
    public Boolean getSensitive()
    {
        return sensitive;
    }

    /**
     * Sets whether the value should be treated as sensitive in the UI.
     *
     * @param sensitive true if sensitive, false otherwise
     */
    public void setSensitive(Boolean sensitive)
    {
        this.sensitive = sensitive;
    }
}

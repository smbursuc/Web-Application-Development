package com.soloproductions.wade.dto;

import java.util.List;

/**
 * Represents settings page payload returned to the frontend.
 */
public class SettingsPageData
{
    private List<SettingOption> options;
    private SettingsValues values;

    /**
     * Gets available setting options.
     *
     * @return list of setting options
     */
    public List<SettingOption> getOptions()
    {
        return options;
    }

    /**
     * Sets available setting options.
     *
     * @param options list of setting options
     */
    public void setOptions(List<SettingOption> options)
    {
        this.options = options;
    }

    /**
     * Gets current settings values.
     *
     * @return current settings values
     */
    public SettingsValues getValues()
    {
        return values;
    }

    /**
     * Sets current settings values.
     *
     * @param values current settings values
     */
    public void setValues(SettingsValues values)
    {
        this.values = values;
    }
}

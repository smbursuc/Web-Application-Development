package com.soloproductions.wade.dto;

import java.util.List;

/**
 * Represents the static settings page configuration loaded from JSON.
 */
public class SettingsPageConfig
{
    private SettingsValues defaults;
    private List<SettingOption> options;

    /**
     * Gets default values for settings.
     *
     * @return default settings values
     */
    public SettingsValues getDefaults()
    {
        return defaults;
    }

    /**
     * Sets default values for settings.
     *
     * @param defaults default settings values
     */
    public void setDefaults(SettingsValues defaults)
    {
        this.defaults = defaults;
    }

    /**
     * Gets available settings option descriptors.
     *
     * @return list of setting options
     */
    public List<SettingOption> getOptions()
    {
        return options;
    }

    /**
     * Sets available settings option descriptors.
     *
     * @param options list of setting options
     */
    public void setOptions(List<SettingOption> options)
    {
        this.options = options;
    }
}

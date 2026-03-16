package com.soloproductions.wade.dto;

/**
 * Holds persisted values for current settings fields.
 */
public class SettingsValues
{
    private Boolean notifications;
    private Boolean darkMode;
    private String apiKey;

    /**
     * Gets the notification preference.
     *
     * @return true if notifications are enabled, false otherwise
     */
    public Boolean getNotifications()
    {
        return notifications;
    }

    /**
     * Sets the notification preference.
     *
     * @param notifications true to enable notifications, false to disable
     */
    public void setNotifications(Boolean notifications)
    {
        this.notifications = notifications;
    }

    /**
     * Gets the dark mode preference.
     *
     * @return true if dark mode is preferred, false otherwise
     */
    public Boolean getDarkMode()
    {
        return darkMode;
    }

    /**
     * Sets the dark mode preference.
     *
     * @param darkMode true if dark mode is preferred, false otherwise
     */
    public void setDarkMode(Boolean darkMode)
    {
        this.darkMode = darkMode;
    }

    /**
     * Gets the external API key value.
     *
     * @return the API key
     */
    public String getApiKey()
    {
        return apiKey;
    }

    /**
     * Sets the external API key value.
     *
     * @param apiKey the API key
     */
    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }
}

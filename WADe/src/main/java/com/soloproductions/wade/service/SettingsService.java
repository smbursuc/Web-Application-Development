package com.soloproductions.wade.service;

import com.soloproductions.wade.dto.SettingsPageConfig;
import com.soloproductions.wade.dto.SettingsPageData;
import com.soloproductions.wade.dto.SettingsValues;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides settings metadata and per-user settings persistence for the Settings page.
 * 
 * <p> Currently a stub. </p>
 */
@Service
public class SettingsService
{
    /** Loaded settings-page configuration metadata. */
    private SettingsPageConfig config;

    /** In-memory per-user settings store. */
    private final ConcurrentHashMap<String, SettingsValues> userSettings = new ConcurrentHashMap<>();

    /**
     * Retrieves settings page payload for a user (options + current values).
     *
     * @param   username
     *          user identifier
    *
     * @return  settings page data for the user
     */
    public SettingsPageData getSettingsPageData(String username)
    {
        SettingsValues defaults = config != null ? config.getDefaults() : null;
        SettingsValues values = userSettings.computeIfAbsent(username, key -> copyValues(defaults));
        SettingsPageData data = new SettingsPageData();
        data.setOptions(config != null ? config.getOptions() : java.util.Collections.emptyList());
        data.setValues(copyValues(values));
        return data;
    }

    /**
     * Saves supported settings fields for a user in server memory.
     *
     * @param   username
     *          user identifier
     * @param   incoming
     *          new values from client
    *
     * @return  saved settings values snapshot
     */
    public SettingsValues saveSettings(String username, SettingsValues incoming)
    {
        SettingsValues defaults = config != null ? config.getDefaults() : null;
        SettingsValues current = userSettings.computeIfAbsent(username, key -> copyValues(defaults));
        if (incoming.getNotifications() != null)
        {
            current.setNotifications(incoming.getNotifications());
        }
        if (incoming.getDarkMode() != null)
        {
            current.setDarkMode(incoming.getDarkMode());
        }
        if (incoming.getApiKey() != null)
        {
            current.setApiKey(incoming.getApiKey());
        }
        return copyValues(current);
    }

    /**
     * Creates a shallow copy of settings values to avoid leaking mutable references.
     *
     * @param   source
     *          source settings values
    *
     * @return  copied settings values
     */
    private SettingsValues copyValues(SettingsValues source)
    {
        SettingsValues target = new SettingsValues();
        if (source == null)
        {
            return target;
        }
        target.setNotifications(source.getNotifications());
        target.setDarkMode(source.getDarkMode());
        target.setApiKey(source.getApiKey());
        return target;
    }
}

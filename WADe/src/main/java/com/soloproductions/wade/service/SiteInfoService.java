package com.soloproductions.wade.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service exposing static informational site messages and health status.
 */
@Service
public class SiteInfoService
{

    /** In-memory map of message keys to content values. */
    private final Map<String, String> siteMessages;

    /**
     * Creates the site info service and initializes default messages.
     */
    public SiteInfoService()
    {
        this.siteMessages = new HashMap<>();
        initializeMessages();
    }

    /**
     * Populates the default message catalog for informational endpoints.
     */
    private void initializeMessages()
    {
        siteMessages.put("disclaimer", "DISCLAIMER: This application is for demonstration and development purposes only. Please do NOT upload, enter, or process any sensitive, personal, or confidential data (PII). The developers assume no liability for data security.");
        siteMessages.put("about", "IMPR - IMage PRocessing Dataset Creator.\n\nBuilt by Solo Productions.\nUsing Spring Boot, React, and Ollama.");
        siteMessages.put("version", "1.0.0-BETA-ALPHA-GIGA-OMEGA Latest Stable Release 2026");
    }

    /**
     * Retrieves a site message by key.
     *
     * @param   key
     *          message key
     *
     * @return  matching message or default fallback text
     */
    public String getMessage(String key)
    {
        return siteMessages.getOrDefault(key.toLowerCase(), "No information available for: " + key);
    }

    /**
     * Health probe for this service.
     *
     * @return  always {@code true}
     */
    public boolean isAlive()
    {
        return true;
    }
}

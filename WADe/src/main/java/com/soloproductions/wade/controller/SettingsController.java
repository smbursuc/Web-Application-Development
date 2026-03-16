package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.SettingsPageData;
import com.soloproductions.wade.dto.SettingsValues;
import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
/**
 * Controller responsible for handling user settings related requests. 
 * It provides endpoints to retrieve and save user-specific settings.
 */
public class SettingsController 
{
    /** Logger for the SettingsController class. */
    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);

    /** Service for handling settings-related operations. */
    private final SettingsService settingsService;

    /**
     * Constructor for SettingsController.
     * 
     * @param   settingsService
     *          the service for handling settings-related operations
     */
    @Autowired
    public SettingsController(SettingsService settingsService)
    {
        this.settingsService = settingsService;
    }

    /**
     * Returns settings page data (options + current values) for the active user.
     *
     * @return settings page payload
     */
    @GetMapping
    public ResponseEntity<SettingsPageData> getSettings()
    {
        String username = resolveUsername();
        SettingsPageData data = settingsService.getSettingsPageData(username);
        return ResponseEntity.ok(data);
    }

    /**
     * Persists settings values for the active user.
     *
     * @param settings settings values payload
     * @return status message
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> saveSettings(@RequestBody SettingsValues settings)
    {
        String username = resolveUsername();
        LOG.info("Received settings update request for user {}", username);
        settingsService.saveSettings(username, settings);
        return ResponseEntity.ok(Collections.singletonMap("status", "Settings saved"));
    }

    /**
     * Resolves a username from Spring Security authentication context.
     *
     * @return resolved username or "anonymous" when unauthenticated
     */
    private String resolveUsername()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Spring's AnonymousAuthenticationFilter (enabled by default) guarantees auth is never null
        // and always marks it as authenticated, so we only need to check for the anonymous token type.
        if (auth instanceof AnonymousAuthenticationToken)
        {
            return "anonymous";
        }

        // The JwtAuthenticationFilter always sets a User entity as the principal,
        // so this is the only concrete type we need to handle.
        if (auth.getPrincipal() instanceof User user)
        {
            return user.getUsername();
        }

        return "anonymous";
    }
}

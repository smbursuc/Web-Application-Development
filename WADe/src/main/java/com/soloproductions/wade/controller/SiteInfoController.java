package com.soloproductions.wade.controller;

import com.soloproductions.wade.service.SiteInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Controller responsible for providing site information such as disclaimers and about page content. 
 * It also includes a heartbeat endpoint for checking server status.
 */
@RestController
@RequestMapping("/api/site-info")
public class SiteInfoController 
{
    /** Service for retrieving site information content. */
    @Autowired
    private SiteInfoService siteInfoService;

    /**
     * Endpoint for retrieving site messages based on a key. Used for extracting information related text.
     * 
     * @param   key
     *          the key identifying the specific site message to retrieve (e.g., "disclaimer", "about")
     * 
     * @return  a response entity containing the requested site message content or an error message if the key is invalid
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getSiteMessage(@PathVariable String key) 
    {
        String message = siteInfoService.getMessage(key);
        return ResponseEntity.ok(Collections.singletonMap("content", message));
    }

    /**
     * Endpoint for checking the server status. This is used by the frontend to verify that the backend is responsive.
     * 
     * @return  a response entity containing the string "OK" if the server is running
     */
    @GetMapping("/heartbeat")
    public ResponseEntity<String> getHeartbeat() 
    {
        return ResponseEntity.ok("OK");
    }
}

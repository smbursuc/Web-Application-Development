package com.soloproductions.wade.controller;

import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.entity.UserAction;
import com.soloproductions.wade.service.UserActionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsible for exposing authenticated user history and usage statistics.
 */
@RestController
@RequestMapping("/api/history")
public class UserActionController 
{

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(UserActionController.class);

    /** Service that handles user action data. */
    @Autowired
    private UserActionService actionService;

    /**
     * Resolves the authenticated username from the Spring Security context.
     *
     * @return  the authenticated username, or {@code null} when the current request is anonymous
     */
    private String getAuthenticatedUsername() 
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Spring's AnonymousAuthenticationFilter (enabled by default) guarantees auth is never null
        // and always marks it as authenticated, so we only need to check for the anonymous token type.
        if (auth instanceof AnonymousAuthenticationToken)
        {
            return null;
        }

        // The JwtAuthenticationFilter always sets a User entity as the principal,
        // so this is the only concrete type we need to handle.
        if (auth.getPrincipal() instanceof User user)
        {
            return user.getUsername();
        }

        return null;
    }

    /**
     * Returns recent history entries for the authenticated user.
     *
     * @param   limit 
     *          maximum number of history entries to return
     * 
     * @return  a response containing the user's recent actions, or {@code 401} when unauthenticated
     */
    @GetMapping
    public ResponseEntity<List<UserAction>> getHistory(@RequestParam(defaultValue = "10") int limit) 
    {
        String username = getAuthenticatedUsername();
        if (username == null) 
            {
            LOG.warn("History request unauthorized");
            return ResponseEntity.status(401).build();
        }
        List<UserAction> actions = actionService.getUserHistory(username, limit);
        LOG.info("History request resolved username='{}', limit={}, actions={}", username, limit, actions.size());
        return ResponseEntity.ok(actions);
    }

    /**
     * Returns usage statistics for the authenticated user.
     *
     * @return a response containing aggregated user statistics, or {@code 401} when unauthenticated
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() 
    {
        String username = getAuthenticatedUsername();
        if (username == null) 
        {
            LOG.warn("Stats request unauthorized");
            return ResponseEntity.status(401).build();
        }
        
        Map<String, Object> stats = actionService.getUserStats(username);
        LOG.info("Stats request resolved username='{}', payload={}", username, stats);
        return ResponseEntity.ok(stats);
    }
}

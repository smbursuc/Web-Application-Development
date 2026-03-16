package com.soloproductions.wade.service;

import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.entity.UserAction;
import com.soloproductions.wade.repository.UserActionRepository;
import com.soloproductions.wade.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for recording and querying user activity history and aggregate stats.
 */
@Service
public class UserActionService 
{

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(UserActionService.class);

    /** Repository for user action persistence and queries. */
    @Autowired
    private UserActionRepository actionRepository;

    /** Repository for resolving users from usernames/emails. */
    @Autowired
    private UserRepository userRepository;

    /**
     * Resolves a user from an identity string (username first, then email).
     *
     * @param   identity
     *          username or email identity
     *
     * @return  resolved user entity or {@code null} if not found
     */
    private User resolveUser(String identity) 
    {
        if (identity == null || identity.isBlank()) 
        {
            LOG.warn("resolveUser called with empty identity");
            return null;
        }

        User user = userRepository.findByUsername(identity);
        if (user == null) 
        {
            LOG.warn("User {} could not be found!", identity);
        }
        else
        {
            LOG.info("resolveUser identity='{}' -> {}", identity, user != null ? user.getUsername() : "null");
        }
        return user;
    }

    /**
     * Persists a user action record when the user can be resolved.
     *
     * @param   username
     *          username or email used to resolve the user
     * @param   actionType
     *          action type label (for example CREATE/VIEW/UPDATE/DELETE)
     * @param   datasetId
     *          dataset identifier associated with the action
     * @param   datasetType
     *          dataset type associated with the action
     * @param   description
     *          free-text description for audit context
     */
    public void logAction(String username, String actionType, String datasetId, String datasetType, String description) 
    {
        User user = resolveUser(username);
        if (user != null) 
        {
            UserAction action = new UserAction(user, actionType, datasetId, datasetType, description);
            UserAction saved = actionRepository.save(action);
            LOG.info("Saved action id={}, userId={}, actionType={}, datasetId={}, datasetType={}",
                     saved.getId(), 
                     user.getId(), 
                     actionType, 
                     datasetId, 
                     datasetType);
        } 
        else 
        {
            LOG.warn("Skipping action log because user could not be resolved. identity='{}'", username);
        }
    }

    /**
     * Returns recent user history ordered by latest timestamp.
     *
     * @param   username
     *          username or email used to resolve the user
     * @param   limit
     *          maximum number of entries to return
     *
     * @return  list of recent user actions (empty when user is unresolved)
     */
    public List<UserAction> getUserHistory(String username, int limit) 
    {
        User user = resolveUser(username);
        if (user == null) 
        {
            return List.of();
        }

        List<UserAction> actions = actionRepository.findByUserIdOrderByTimestampDesc(user.getId(), PageRequest.of(0, limit));
        LOG.info("getUserHistory userId={}, limit={}, returned={}", user.getId(), limit, actions.size());
        return actions;
    }

    /**
     * Computes basic user activity statistics for dashboard display.
     *
     * @param   username
     *          username or email used to resolve the user
     *
     * @return  map containing totals and common action counts
     */
    public Map<String, Object> getUserStats(String username) 
    {
        User user = resolveUser(username);
        if (user == null) 
        {
            return Map.of();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActions", actionRepository.countByUserId(user.getId()));
        stats.put("datasetsCreated", actionRepository.countByUserIdAndActionType(user.getId(), "CREATE"));
        stats.put("datasetsViewed", actionRepository.countByUserIdAndActionType(user.getId(), "VIEW"));
        LOG.info("getUserStats userId={}, stats={}", user.getId(), stats);
        
        // Add more complex stats logic here (e.g., datasets currently owned) if needed
        return stats;
    }
}

package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.PredictionRequest;
import com.soloproductions.wade.service.PredictionService;
import com.soloproductions.wade.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller responsible for handling prediction requests. Provides endpoints for similarity, clustering, and node data predictions.
 * For cluster datasets, the similarity and the fitting cluster are guessed.
 * For heatmap datasets, the similarity between two objects is guessed.
 * An LLM service is used for all predictions.
 * A per-client rate limiter (configured via application.properties) restricts the number of AI calls
 * allowed within a rolling time window.
 */
@RestController
@RequestMapping("/api/prediction")
public class PredictionController
{
    /** The prediction service, handling the logic for the endpoints. */
    private final PredictionService predictionService;

    /** Handles the rate limiting logic for AI guesses. */
    private final RateLimitService rateLimitService;

    /**
     * Constructor.
     *
     * @param   predictionService
     *          The prediction service.
     * @param   rateLimitService
     *          The rate limiter service.
     */
    @Autowired
    public PredictionController(PredictionService predictionService, RateLimitService rateLimitService)
    {
        this.predictionService = predictionService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Returns the current rate-limit status for the calling client.
     * @param   request
     *          The HTTP request.
     *
     * @return  A response entity with key-value pairs that indicate how much quota is left.
     */
    @GetMapping("/rate-limit/status")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(HttpServletRequest request)
    {
        RateLimitService.RateLimitResult status = rateLimitService.getStatus(clientId(request));
        return ResponseEntity.ok(Map.of("remaining", status.remaining,
                                        "resetInSeconds", status.resetInSeconds));
    }

    /**
     * Predicts the best matching cluster and a confidence score.
     *
     * @param   request
     *          A prediction request mapped to its respective DTO.
     * @param   httpRequest
     *          The HTTP servlet request.
     *
     * @return  The prediction result or an error response if the quota is passed or an error occurred.
     */
    @PostMapping("/cluster/select-with-confidence")
    public ResponseEntity<String> predictClusterWithConfidence(@RequestBody PredictionRequest request,
                                                               HttpServletRequest httpRequest)
    {
        if (request.getObject1() == null || request.getCandidates() == null    ||
                                            request.getCandidates().isEmpty())
        {
            return ResponseEntity.badRequest().body("object1 and a list of candidates are required.");
        }

        RateLimitService.RateLimitResult rl = rateLimitService.checkAndDecrement(clientId(httpRequest));
        if (!rl.allowed)
        {
            return ResponseEntity
                   .status(429)
                   .header("X-RateLimit-Remaining", "0")
                   .body("Rate limit exceeded. You have used all your AI guesses for this period. " +
                         "Quota resets in " + rl.resetInSeconds + " seconds.");
        }

        try
        {
            String result = predictionService.guessClusterWithConfidence(request);
            return ResponseEntity
                   .ok()
                   .header("X-RateLimit-Remaining", String.valueOf(rl.remaining))
                   .body(result);
        }
        catch (Exception e)
        {
            return ResponseEntity
                   .status(500)
                   .header("X-RateLimit-Remaining", String.valueOf(rl.remaining))
                   .body("Error processing cluster prediction: " + e.getMessage());
        }
    }
    
    /**
     * Guesses node data (object label + confidence) from an image URI.
     *
     * @param   request
     *          The prediction request, mapped to its respective DTO.
     *          httpRequest
     *          The HTTP servlet request.
     *
     * @return  A response entity containing the guess result  or an error response if the quota is passed or
     *          an error occurred.
     */
    @PostMapping("/node/guess")
    public ResponseEntity<String> guessNodeData(@RequestBody PredictionRequest request,
                                                HttpServletRequest httpRequest) 
    {
        if (request.getUri() == null || request.getUri().isEmpty()) 
        {
            return ResponseEntity.badRequest().body("URL (uri) is required.");
        }

        RateLimitService.RateLimitResult rl = rateLimitService.checkAndDecrement(clientId(httpRequest));
        if (!rl.allowed)
        {
            return ResponseEntity
                   .status(429)
                   .header("X-RateLimit-Remaining", "0")
                   .body("Rate limit exceeded. You have used all your AI guesses for this period. " +
                         "Quota resets in " + rl.resetInSeconds + " seconds.");
        }

        try 
        {
            String result = predictionService.guessNodeData(request.getUri());
            return ResponseEntity
                   .ok()
                   .header("X-RateLimit-Remaining", String.valueOf(rl.remaining))
                   .body(result);
        } 
        catch (Exception e) 
        {
            return ResponseEntity
                   .status(500)
                   .header("X-RateLimit-Remaining", String.valueOf(rl.remaining))
                   .body("Error processing image: " + e.getMessage());
        }
    }
    
    /**
     * Predicts similarity probability between two objects in JSON format.
     *
     * @param   request
     *          The prediction request mapped to its respective DTO.
     * @param   httpRequest
     *          The HTTP servlet request.
     *
     * @return  A response entity with the guess result  or an error response if the quota is passed or an
     *          error occurred.
     */
    @PostMapping("/similarity/score")
    public ResponseEntity<String> guessSimilarity(@RequestBody PredictionRequest request,
                                                  HttpServletRequest httpRequest) 
    {
        if (request.getObject1() == null || request.getObject2() == null) 
        {
            return ResponseEntity.badRequest().body("Both object1 and object2 are required.");
        }

        RateLimitService.RateLimitResult rl = rateLimitService.checkAndDecrement(clientId(httpRequest));
        if (!rl.allowed) {
            return ResponseEntity
                   .status(429)
                   .header("X-RateLimit-Remaining", "0")
                   .body("Rate limit exceeded. You have used all your AI guesses for this period. " +
                         "Quota resets in " + rl.resetInSeconds + " seconds.");
        }

        try 
        {
            String result = predictionService.guessSimilarityProbability(request.getObject1(),
                                                                         request.getObject2());
            return ResponseEntity
                   .ok()
                   .header("X-RateLimit-Remaining", String.valueOf(rl.remaining))
                   .body(result);
        } 
        catch (Exception e) 
        {
             return ResponseEntity
                    .status(500)
                    .header("X-RateLimit-Remaining", String.valueOf(rl.remaining))
                    .body("Error processing prediction: " + e.getMessage());
        }
    }

    /**
     * Extracts the client ID from the request.
     *
     * @param   request
     *          The HTTP request.
     *
     * @return  A unique client ID.
     */
    private static String clientId(HttpServletRequest request)
    {
        // Prefer X-Forwarded-For when behind a proxy/load-balancer
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
        {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


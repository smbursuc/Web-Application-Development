package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.PredictionRequest;
import com.soloproductions.wade.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible for handling prediction requests. Provides endpoints for similarity, clustering, and node data predictions.
 * For cluster datasets, the similarity and the fitting cluster are guessed.
 * For heatmap datasets, the similarity between two objects is guessed.
 * An LLM service is used for all predictions.
 */
@RestController
@RequestMapping("/api/prediction")
public class PredictionController
{

    private final PredictionService predictionService;

    @Autowired
    public PredictionController(PredictionService predictionService)
    {
        this.predictionService = predictionService;
    }

    /**
     * Predicts the best matching cluster and a confidence score.
     */
    @PostMapping("/cluster/select-with-confidence")
    public ResponseEntity<String> predictClusterWithConfidence(@RequestBody PredictionRequest request)
    {
        if (request.getObject1() == null || request.getCandidates() == null || request.getCandidates().isEmpty())
        {
            return ResponseEntity.badRequest().body("object1 and a list of candidates are required.");
        }
        try
        {
            String result = predictionService.guessClusterWithConfidence(request);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(500).body("Error processing cluster prediction: " + e.getMessage());
        }
    }
    
    /**
     * Guesses node data (object label + confidence) from an image URI.
     */
    @PostMapping("/node/guess")
    public ResponseEntity<String> guessNodeData(@RequestBody PredictionRequest request) 
    {
        if (request.getUri() == null || request.getUri().isEmpty()) 
        {
            return ResponseEntity.badRequest().body("URL (uri) is required.");
        }
        try 
        {
            String result = predictionService.guessNodeData(request.getUri());
            return ResponseEntity.ok(result);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.status(500).body("Error processing image: " + e.getMessage());
        }
    }
    
    /**
     * Predicts similarity probability between two objects in JSON format.
     */
    @PostMapping("/similarity/score")
    public ResponseEntity<String> guessSimilarity(@RequestBody PredictionRequest request) 
    {
        if (request.getObject1() == null || request.getObject2() == null) 
        {
            return ResponseEntity.badRequest().body("Both object1 and object2 are required.");
        }
        try 
        {
            String result = predictionService.guessSimilarityProbability(request.getObject1(), request.getObject2());
            return ResponseEntity.ok(result);
        } 
        catch (Exception e) 
        {
             return ResponseEntity.status(500).body("Error processing prediction: " + e.getMessage());
        }
    }
}

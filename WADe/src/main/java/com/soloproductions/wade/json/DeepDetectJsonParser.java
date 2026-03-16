package com.soloproductions.wade.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser implementation for DeepDetect JSON prediction responses. The parser 
 * extracts prediction classes and flattens them into string rows in the format: 
 * {@code <class-label> <probability> <uri>}.
 */
public class DeepDetectJsonParser implements ResponseParser
{
    /**
     * Parses a DeepDetect response payload.
     *
     * @param   jsonInput 
     *          raw JSON response from DeepDetect
     * 
     * @return  flattened prediction rows
     */
    public List<String> parseResponse(String jsonInput)
    {
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonResponse response = objectMapper.readValue(jsonInput, JsonResponse.class);

            // extract all classes
            List<String> allClasses = new ArrayList<>();
            if (response.getBody() != null && response.getBody().getPredictions() != null)
            {
                for (Prediction prediction : response.getBody().getPredictions())
                {
                    if (prediction.getClasses() != null)
                    {
                        for (PredictionClass predictionClass : prediction.getClasses())
                        {
                            String cat = predictionClass.getCat();
                            double prob = predictionClass.getProb();
                            String uri = prediction.getUri();
                            int spaceIdx = cat.indexOf(" ");
                            String object = cat.substring(spaceIdx + 1) + " " + prob + " " + uri;
                            allClasses.add(object);
                        }
                    }
                }
            }

            return allClasses;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Error parsing JSON response");
        }

    }
}

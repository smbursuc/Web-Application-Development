package com.soloproductions.wade.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class DeepDetectJsonParser implements ResponseParser
{
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

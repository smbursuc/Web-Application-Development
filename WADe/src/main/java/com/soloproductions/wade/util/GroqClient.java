package com.soloproductions.wade.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Lightweight helper for calling the Groq cloud API.
 *
 * <p>Groq exposes an OpenAI-compatible {@code /chat/completions} endpoint, so this
 * client builds standard chat-completion requests and parses the response.
 * It is designed to be instantiated once by {@code PredictionService} when the
 * configured provider is {@code groq}.
 *
 * <p>Usage:
 * <pre>
 *     GroqClient groq = new GroqClient(apiKey, model, baseUrl);
 *     String answer = groq.chatCompletion(prompt);
 * </pre>
 */
public class GroqClient
{
    private static final Logger LOG = LoggerFactory.getLogger(GroqClient.class);

    /** Groq API base URL (OpenAI-compatible). */
    private final String baseUrl;

    /** Bearer API key. */
    private final String apiKey;

    /** Default model identifier for text completions. */
    private final String model;

    /** Shared HTTP client. */
    private final RestTemplate restTemplate;

    /**
     * @param   apiKey      
     *          Groq API key (starts with {@code gsk_})
     * @param   model       
     *          model to use for text completions (e.g. {@code llama-3.1-70b-versatile})
     * @param   baseUrl     
     *          API base URL, usually {@code https://api.groq.com/openai/v1}
     */
    public GroqClient(String apiKey, String model, String baseUrl)
    {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends a text prompt as a single-turn chat completion and returns the
     * assistant's reply.
     *
     * @param   prompt  
     *          user message
     *
     * @return  model response text, or an {@code "Error: ..."} string on failure
     */
    public String chatCompletion(String prompt)
    {
        return chatCompletion(prompt, this.model);
    }

    /**
     * Sends a text prompt using a specific model override.
     *
     * @param   prompt      
     *          user message
     * @param   modelOverride   
     *          model identifier to use instead of the default
     *
     * @return  model response text
     */
    public String chatCompletion(String prompt, String modelOverride)
    {
        try
        {
            HttpHeaders headers = buildHeaders();

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelOverride);
            body.put("messages", Collections.singletonList(message));
            body.put("temperature", 0.0);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = baseUrl + "chat/completions";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return extractContent(response.getBody());
        }
        catch (Exception e)
        {
            LOG.error("Groq chat completion failed", e);
            throw new RuntimeException("Groq chat completion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a vision request with an image encoded as Base64 JPEG.
     *
     * @param   prompt          
     *          text prompt accompanying the image
     * @param   base64Image     
     *          Base64-encoded JPEG image data
     * @param   visionModel     
     *          vision-capable model identifier
     *
     * @return  model response text
     */
    public String visionCompletion(String prompt, String base64Image, String visionModel)
    {
        try
        {
            HttpHeaders headers = buildHeaders();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", prompt);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> imageUrlMap = new HashMap<>();
            imageUrlMap.put("url", "data:image/jpeg;base64," + base64Image);
            imagePart.put("image_url", imageUrlMap);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", Arrays.asList(textPart, imagePart));

            Map<String, Object> body = new HashMap<>();
            body.put("model", visionModel);
            body.put("messages", Collections.singletonList(message));
            body.put("max_tokens", 300);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = baseUrl + "chat/completions";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return extractContent(response.getBody());
        }
        catch (Exception e)
        {
            LOG.error("Groq vision completion failed", e);
            throw new RuntimeException("Groq vision completion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds authenticated JSON headers for Groq API requests.
     *
     * @return  HTTP headers with bearer auth and JSON content type
     */
    private HttpHeaders buildHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    /**
     * Extracts the assistant message content from an OpenAI-compatible response body.
     *
     * @param   body    
     *          deserialized response map
     *
     * @return  extracted text, or a fallback string
     */
    private String extractContent(Map body)
    {
        if (body == null)
        {
            return "";
        }

        try
        {
            if (body.containsKey("choices"))
            {
                List<Map> choices = (List<Map>) body.get("choices");
                if (!choices.isEmpty())
                {
                    Map choice = choices.get(0);
                    Object msgObj = choice.get("message");
                    if (msgObj instanceof Map)
                    {
                        Object content = ((Map) msgObj).get("content");
                        if (content instanceof String)
                        {
                            return ((String) content).trim();
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOG.debug("Failed to extract content from Groq response", e);
        }

        return body.toString();
    }
}

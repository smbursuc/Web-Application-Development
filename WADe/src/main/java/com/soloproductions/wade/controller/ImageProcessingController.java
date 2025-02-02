package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soloproductions.wade.dto.ProcessImageRequest;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.json.DeepDetectJsonParser;
import com.soloproductions.wade.json.ResponseParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ImageProcessingController
{

    private static final String DEEPDETECT_URL = "http://localhost:8080/predict";

    @PostMapping("/process-image")
    @Operation(summary = "Process Image",
            description = "Processes images by sending paths to a deep learning model server and returns the processed data.",
            tags = {"Image Processing"})
    @ApiResponse(responseCode = "200",
            description = "Image processed successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StandardResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = StandardResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = StandardResponse.class)))
    public ResponseEntity<StandardResponse<?>> processImage(
            @Parameter(description = "Process image request")
            ProcessImageRequest piRequest)
    {
        try
        {
            List<String> imagePaths = piRequest.getImagePaths();
            if (imagePaths == null || imagePaths.isEmpty())
            {
                StandardResponse<String> errorResponse = new StandardResponse<>(
                        "error",
                        "Processing failed! Image path is missing in the request body.",
                        null
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Prepare the DeepDetect API request
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPDETECT_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(constructRequestPayload(imagePaths)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            String responseType = piRequest.getResponseType();
            if (responseType != null && responseType.equalsIgnoreCase("raw"))
            {
                // Return the DeepDetect response
                StandardResponse<String> successResponse = new StandardResponse<>(
                        "success",
                        "Processing completed!",
                        responseBody
                );

                return ResponseEntity.ok(successResponse);
            }

            ResponseParser rp = new DeepDetectJsonParser();

            // should contain all classes
            List<String> responseParserResult = rp.parseResponse(responseBody);

            StandardResponse<List<String>> successResponse = new StandardResponse<>(
                    "success",
                    "Processing completed!",
                    responseParserResult
            );

            return ResponseEntity.ok(successResponse);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            StandardResponse<String> errorResponse = new StandardResponse<>(
                    "error",
                    "Processing failed! " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    private String constructRequestPayload(List<String> imagePaths)
    {
        try
        {
            // Use the special hostname to refer to the host IP address
            List<String> sanitizedPaths = imagePaths.stream().map(path -> path.replace("localhost", "host.docker.internal")).toList();

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("service", "imageserv");
            ObjectNode parametersNode = rootNode.putObject("parameters");

            ObjectNode inputNode = parametersNode.putObject("input");
            inputNode.put("width", 224);
            inputNode.put("height", 224);

            ObjectNode outputNode = parametersNode.putObject("output");
            outputNode.put("best", 1);

            ObjectNode mllibNode = parametersNode.putObject("mllib");
            mllibNode.put("gpu", true);

            ArrayNode dataNode = rootNode.putArray("data");
            sanitizedPaths.forEach(dataNode::add);

            return objectMapper.writeValueAsString(rootNode);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

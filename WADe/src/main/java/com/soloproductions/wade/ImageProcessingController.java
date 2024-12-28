package com.soloproductions.wade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImageProcessingController
{

    private static final String DEEPDETECT_URL = "http://localhost:8080/predict"; // Update with DeepDetect endpoint

    @PostMapping("/process-image")
    public ResponseEntity<String> processImage(@RequestBody Map<String, String> requestBody)
    {
        try
        {
            // Extract the image path from the request body
            String imagePath = requestBody.get("imagePath");
            if (imagePath == null || imagePath.isEmpty())
            {
                return ResponseEntity.badRequest().body("Image path is missing in the request body.");
            }

            // Read the image file
//            File imageFile = new File(imagePath);
//            if (!imageFile.exists())
//            {
//                return ResponseEntity.badRequest().body("Image file not found at: " + imagePath);
//            }

            // Prepare the DeepDetect API request
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPDETECT_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(constructRequestPayload(imagePath)))
                    .build();

            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Return the DeepDetect response
            return ResponseEntity.ok(response.body());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing image: " + e.getMessage());
        }
    }

    private String constructRequestPayload(String imagePath) throws IOException
    {
        // Escape backslashes in the image path
//        String sanitizedPath = imagePath.replace("\\", "\\\\");

        // Use the special hostname to refer to the host IP address
        String sanitizedPath = imagePath.replace("localhost", "host.docker.internal");

        // Prepare the DeepDetect JSON payload
        return "{ \"service\":\"imageserv\", \"parameters\":{ \"input\":{ \"width\":224, \"height\":224 }, \"output\":{ \"best\":3 }, \"mllib\":{ \"gpu\":true } }, \"data\":[ \"%s\" ] }".formatted(sanitizedPath);

    }
}

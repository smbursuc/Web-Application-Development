package com.soloproductions.wade.controller;

import com.soloproductions.wade.dataset.AbstractDatasetData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Component used in the dataset buildup process. Used for storing images found in the dataset.
 * This component only has a use case if creating datasets locally and not through the SqlController API.
 * Default datasets such as CIFAR-10 and BSDS300 have been created using these endpoints.
 */
@RestController
@RequestMapping("/api")
public class ImageUploadController
{
    /** Directory where uploaded images are stored. This is hardcoded but can be configured if needed. */
    private static final String UPLOAD_DIR = "cifar10";

    /** Logger for the ImageUploadController class. */
    private static final Logger LOG = LogManager.getLogger(ImageUploadController.class);

    /**
    * Endpoint responsible for handling image file uploads. Accepts a multipart file upload request and saves the file to the server.
    * Returns the URL to access the uploaded file.
    *
    * @param file  the multipart file being uploaded
    * @return      a response entity containing the URL to access the uploaded file, or an error status if the upload fails
    */
    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(
            @Parameter(description = "The file being uploaded")
            @RequestParam("image") MultipartFile file
    )
    {
        try
        {
            // Create the upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath))
            {
                Files.createDirectories(uploadPath);
            }

            // Save the file with its original filename
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL to access the file
            String fileUrl = "/api/files/" + fileName;
            return ResponseEntity.ok(fileUrl);

        }
        catch (IOException e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading image: " + e.getMessage());
        }
    }

    /**
     * Endpoint responsible for serving uploaded image files. Used when building a dataset through the IMPR scripts.
     * For SqlController, this is not used, as in the creation phase, uploaded files are not supported, an existing 
     * URI must be provided.
     * 
     * @param fileName   the name of the file to retrieve, as returned by the upload endpoint
     * @return           the response entity containing the image resource, or an error status if the file is not found or cannot be served 
     */
    @GetMapping("/files/{fileName}")
    public ResponseEntity<Resource> serveFile(
            @Parameter(description = "The name of the file to retrieve", required = true)
            @PathVariable String fileName
    )
    {
        List<String> datasets = AbstractDatasetData.DATASETS;
        for (String dataset : datasets)
        {
            try
            {
                Path filePath = Paths.get(dataset).resolve(fileName).normalize();
                Resource resource = new UrlResource(filePath.toUri());

                if (resource.exists())
                {
                    // content-type header needs to be set to image so that the browser won't display binary data
                    String contentType = Files.probeContentType(filePath);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(resource);
                }
            }
            catch (Exception e)
            {
                LOG.error("Encountered exception while uploading file {}. Error: {}", fileName, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
        return ResponseEntity.notFound().build();
    }

}


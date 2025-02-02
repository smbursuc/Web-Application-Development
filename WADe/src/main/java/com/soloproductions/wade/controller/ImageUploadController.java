package com.soloproductions.wade.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ImageUploadController
{

    private static final String UPLOAD_DIR = "bsds300";

    @PostMapping("/upload-image")
    @Operation(summary = "Upload Image",
            description = "Uploads an image file to the server and returns the URL to access the uploaded image.",
            tags = {"Image Upload"})
    @ApiResponse(responseCode = "200",
            description = "The URL of the uploaded image",
            content = @Content(mediaType = "text/plain",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(type = "string")))
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

    @GetMapping("/files/{fileName}")
    @Operation(summary = "Serve File",
            description = "Returns the requested file if it exists within the server's upload directory.",
            tags = {"File Serving"})
    @ApiResponse(responseCode = "200",
            description = "File retrieved successfully",
            content = @Content(mediaType = "application/octet-stream",
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "File not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<Resource> serveFile(
            @Parameter(description = "The name of the file to retrieve", required = true)
            @PathVariable String fileName
    )
    {
        try
        {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName).normalize();
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
            else
            {
                filePath = Paths.get("uploaded-images").resolve(fileName).normalize();
                resource = new UrlResource(filePath.toUri());

                if (resource.exists())
                {
                    // content-type header needs to be set to image so that the browser won't display binary data
                    String contentType = Files.probeContentType(filePath);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(resource);
                }
                else
                {
                    return ResponseEntity.notFound().build();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}


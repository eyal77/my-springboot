package com.example.eyal.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/multipart")
@Tag(name = "Multipart Upload", description = "Endpoint for uploading files via multipart requests")
public class MultipartController {

    @Value("${app.upload.dir:${user.home}/multipart}")
    private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file",
            description = "Receives a multipart file, saves it to the configured directory, and returns details about the saved file."
    )
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(
                    description = "The file to upload",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("error", "Bad Request");
            response.put("message", "Uploaded file is empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Resolve upload directory
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Get clean filename
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.contains("..")) {
                response.put("error", "Bad Request");
                response.put("message", "Invalid file name");
                return ResponseEntity.badRequest().body(response);
            }

            // Save file
            Path targetLocation = uploadPath.resolve(originalFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            response.put("status", "success");
            response.put("fileName", originalFileName);
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("savedPath", targetLocation.toString());

            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            response.put("error", "Internal Server Error");
            response.put("message", "Could not save the file: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

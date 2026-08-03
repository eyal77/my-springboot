package com.example.eyal.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/multipart")
@Tag(name = "Multipart Upload", description = "Endpoint for uploading files via multipart requests")
public class MultipartController {

    @Value("${app.upload.dir:${user.home}/multipart}")
    private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload files",
            description = "Receives a multipart request containing one or more files, saves them to the configured directory, and returns details about all saved files."
    )
    public ResponseEntity<Map<String, Object>> uploadFiles(MultipartHttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        MultiValueMap<String, MultipartFile> multiFileMap = request.getMultiFileMap();
        if (multiFileMap.isEmpty()) {
            response.put("error", "Bad Request");
            response.put("message", "No files found in the multipart request");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Resolve upload directory
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            List<Map<String, Object>> uploadedFiles = new ArrayList<>();

            for (Map.Entry<String, List<MultipartFile>> entry : multiFileMap.entrySet()) {
                String parameterName = entry.getKey();
                List<MultipartFile> files = entry.getValue();
                
                if (files == null) continue;

                for (MultipartFile file : files) {
                    if (file.isEmpty()) {
                        continue; // Skip empty files
                    }

                    // Get clean filename
                    String originalFileName = file.getOriginalFilename();
                    if (originalFileName == null || originalFileName.contains("..")) {
                        response.put("error", "Bad Request");
                        response.put("message", "Invalid file name: " + originalFileName);
                        return ResponseEntity.badRequest().body(response);
                    }

                    // Save file
                    Path targetLocation = uploadPath.resolve(originalFileName);
                    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                    Map<String, Object> fileDetails = new HashMap<>();
                    fileDetails.put("parameterName", parameterName);
                    fileDetails.put("fileName", originalFileName);
                    fileDetails.put("fileSize", file.getSize());
                    fileDetails.put("contentType", file.getContentType());
                    fileDetails.put("savedPath", targetLocation.toString());

                    uploadedFiles.add(fileDetails);
                }
            }

            if (uploadedFiles.isEmpty()) {
                response.put("error", "Bad Request");
                response.put("message", "All uploaded files were empty");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("status", "success");
            response.put("uploadedFiles", uploadedFiles);

            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            response.put("error", "Internal Server Error");
            response.put("message", "Could not save the files: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

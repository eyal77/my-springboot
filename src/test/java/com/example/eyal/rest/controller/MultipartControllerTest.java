package com.example.eyal.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MultipartControllerTest {

    private MockMvc mockMvc;
    private MultipartController controller;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        controller = new MultipartController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadFile_ShouldSaveFileAndReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "customFileKey",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        mockMvc.perform(multipart("/api/multipart").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.uploadedFiles[0].fileName").value("test.txt"))
                .andExpect(jsonPath("$.uploadedFiles[0].fileSize").value(13))
                .andExpect(jsonPath("$.uploadedFiles[0].contentType").value("text/plain"))
                .andExpect(jsonPath("$.uploadedFiles[0].savedPath").exists());
    }

    @Test
    void uploadMultipleFiles_ShouldSaveAllFilesAndReturnSuccess() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1.txt",
                "text/plain",
                "Hello, First!".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file2",
                "test2.txt",
                "text/plain",
                "Hello, Second!!".getBytes()
        );

        mockMvc.perform(multipart("/api/multipart").file(file1).file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.uploadedFiles.length()").value(2))
                .andExpect(jsonPath("$.uploadedFiles[?(@.fileName == 'test1.txt')].fileSize").value(13))
                .andExpect(jsonPath("$.uploadedFiles[?(@.fileName == 'test2.txt')].fileSize").value(15));
    }

    @Test
    void uploadMultipleFilesSameKey_ShouldSaveAllFilesAndReturnSuccess() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "test1.txt",
                "text/plain",
                "Hello, First!".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "test2.txt",
                "text/plain",
                "Hello, Second!!".getBytes()
        );

        mockMvc.perform(multipart("/api/multipart").file(file1).file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.uploadedFiles.length()").value(2))
                .andExpect(jsonPath("$.uploadedFiles[?(@.fileName == 'test1.txt')].parameterName").value("file"))
                .andExpect(jsonPath("$.uploadedFiles[?(@.fileName == 'test2.txt')].parameterName").value("file"));
    }

    @Test
    void uploadTextParts_ShouldSaveAsPropertiesFile() throws Exception {
        MockMultipartFile textPart1 = new MockMultipartFile(
                "username",
                "blob",
                "text/plain",
                "jane_doe".getBytes()
        );
        MockMultipartFile textPart2 = new MockMultipartFile(
                "role",
                "",
                "text/plain",
                "ADMIN".getBytes()
        );

        mockMvc.perform(multipart("/api/multipart").file(textPart1).file(textPart2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.uploadedFiles[0].fileName").value("upload.properties"))
                .andExpect(jsonPath("$.uploadedFiles[0].parameterName").value("properties"));

        // Verify properties content
        Path propFile = tempDir.resolve("upload.properties");
        List<String> lines = Files.readAllLines(propFile);
        org.junit.jupiter.api.Assertions.assertTrue(lines.contains("username=jane_doe"));
        org.junit.jupiter.api.Assertions.assertTrue(lines.contains("role=ADMIN"));
    }

    @Test
    void uploadFile_EmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "anotherCustomKey",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/multipart").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("All uploaded parts were empty"));
    }

    @Test
    void uploadFile_NoFiles_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/multipart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("No parts found in the multipart request"));
    }
}

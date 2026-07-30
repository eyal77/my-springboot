package com.example.eyal.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

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
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        mockMvc.perform(multipart("/api/multipart").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.fileSize").value(13))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.savedPath").exists());
    }

    @Test
    void uploadFile_EmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/multipart").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));
    }
}

package com.example.eyal.rest.controller;

import com.example.eyal.rest.service.SystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SystemControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SystemService systemService = new SystemService();
        SystemController controller = new SystemController(systemService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSystemInfo_ShouldReturnSystemInfoPayload() throws Exception {
        mockMvc.perform(get("/api/system-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").exists())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.time").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.freeSpaceBytes").isNumber())
                .andExpect(jsonPath("$.freeSpaceFormatted").exists())
                .andExpect(jsonPath("$.diskDrives").isArray());
    }
}

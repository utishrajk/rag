package com.example.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "app.load-data-on-startup=false"
})
class RagControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testHealthEndpoint() throws Exception {
        setUp();
        
        mockMvc.perform(get("/api/rag/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("RAG Application with Redis Vector Store"));
    }

    @Test
    void testLoadDataEndpoint() throws Exception {
        setUp();
        
        // Note: This test will fail if Redis is not running or OpenAI key is invalid
        // In a real test environment, you would mock these dependencies
        mockMvc.perform(post("/api/rag/load-data"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testSearchEndpointWithValidQuery() throws Exception {
        setUp();
        
        Map<String, String> searchRequest = Map.of(
            "query", "government expenditure",
            "topK", "3",
            "similarityThreshold", "0.7"
        );

        String requestJson = objectMapper.writeValueAsString(searchRequest);

        // Note: This test requires data to be loaded first and valid OpenAI API key
        mockMvc.perform(post("/api/rag/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.query").value("government expenditure"));
    }

    @Test
    void testSearchEndpointWithEmptyQuery() throws Exception {
        setUp();
        
        Map<String, String> searchRequest = Map.of("query", "");
        String requestJson = objectMapper.writeValueAsString(searchRequest);

        mockMvc.perform(post("/api/rag/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Query cannot be empty"));
    }

    @Test
    void testSearchByYearEndpointWithValidData() throws Exception {
        setUp();
        
        Map<String, String> searchRequest = Map.of(
            "query", "revenue",
            "year", "2007/08",
            "topK", "3"
        );

        String requestJson = objectMapper.writeValueAsString(searchRequest);

        mockMvc.perform(post("/api/rag/search-by-year")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.query").value("revenue"))
                .andExpect(jsonPath("$.year").value("2007/08"));
    }

    @Test
    void testSearchByYearEndpointWithMissingYear() throws Exception {
        setUp();
        
        Map<String, String> searchRequest = Map.of("query", "revenue");
        String requestJson = objectMapper.writeValueAsString(searchRequest);

        mockMvc.perform(post("/api/rag/search-by-year")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Year cannot be empty"));
    }

    @Test
    void testSearchByYearEndpointWithMissingQuery() throws Exception {
        setUp();
        
        Map<String, String> searchRequest = Map.of("year", "2007/08");
        String requestJson = objectMapper.writeValueAsString(searchRequest);

        mockMvc.perform(post("/api/rag/search-by-year")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Query cannot be empty"));
    }
}
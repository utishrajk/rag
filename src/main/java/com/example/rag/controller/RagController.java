package com.example.rag.controller;

import com.example.rag.service.RagService;
import com.example.rag.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {
    
    private static final Logger logger = LoggerFactory.getLogger(RagController.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private RagService ragService;
    
    @PostMapping("/load-data")
    public ResponseEntity<Map<String, String>> loadData() {
        try {
            logger.info("Loading CSV data into vector store");
            vectorStoreService.loadAndStoreDocuments("macroeconimic-indicator-2007-2017-by-monetary-sector.csv");
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Data successfully loaded into Redis vector store"
            ));
        } catch (Exception e) {
            logger.error("Error loading data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to load data: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Query cannot be empty"
                ));
            }
            
            logger.info("Searching for: {}", query);
            
            // Get top K from request or default to 5
            String topKStr = request.get("topK");
            int topK = topKStr != null ? Integer.parseInt(topKStr) : 5;
            
            // Get similarity threshold from request or default to 0.75
            String thresholdStr = request.get("similarityThreshold");
            double threshold = thresholdStr != null ? Double.parseDouble(thresholdStr) : 0.75;
            
            List<Document> results = vectorStoreService.searchSimilarDocuments(query, topK, threshold);
            
            // Format response
            List<Map<String, Object>> formattedResults = results.stream()
                    .map(doc -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", doc.getText());
                        result.put("metadata", doc.getMetadata());
                        return result;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "query", query,
                "totalResults", results.size(),
                "results", formattedResults
            ));
            
        } catch (Exception e) {
            logger.error("Error performing search: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Search failed: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/search-by-year")
    public ResponseEntity<Map<String, Object>> searchByYear(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            String year = request.get("year");
            
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Query cannot be empty"
                ));
            }
            
            if (year == null || year.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Year cannot be empty"
                ));
            }
            
            logger.info("Searching for: {} in year: {}", query, year);
            
            String topKStr = request.get("topK");
            int topK = topKStr != null ? Integer.parseInt(topKStr) : 5;
            
            List<Document> results = vectorStoreService.searchWithMetadataFilter(query, topK, year);
            
            // Format response
            List<Map<String, Object>> formattedResults = results.stream()
                    .map(doc -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", doc.getText());
                        result.put("metadata", doc.getMetadata());
                        return result;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "query", query,
                "year", year,
                "totalResults", results.size(),
                "results", formattedResults
            ));
            
        } catch (Exception e) {
            logger.error("Error performing year-filtered search: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Search failed: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Prompt cannot be empty"
                ));
            }
            
            logger.info("Processing RAG request: {}", prompt);
            
            String response = ragService.generateResponse(prompt);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "prompt", prompt,
                "response", response
            ));
            
        } catch (Exception e) {
            logger.error("Error processing RAG request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to process request: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/ask-external")
    public ResponseEntity<Map<String, Object>> askExternal(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String externalUrl = request.get("url");
            
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Prompt cannot be empty"
                ));
            }
            
            if (externalUrl == null || externalUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "External LLM URL cannot be empty"
                ));
            }
            
            logger.info("Processing external LLM request: {} to URL: {}", prompt, externalUrl);
            
            String response = ragService.generateExternalResponse(prompt, externalUrl);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "prompt", prompt,
                "externalUrl", externalUrl,
                "response", response
            ));
            
        } catch (Exception e) {
            logger.error("Error processing external LLM request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to process external request: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "RAG Application with Redis Vector Store"
        ));
    }
}
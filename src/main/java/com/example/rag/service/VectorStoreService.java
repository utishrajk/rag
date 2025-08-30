package com.example.rag.service;

import com.example.rag.model.MacroeconomicIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorStoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private CsvProcessingService csvProcessingService;
    
    public void loadAndStoreDocuments(String csvFileName) {
        logger.info("Loading and storing documents from CSV: {}", csvFileName);
        
        // Load CSV data
        List<MacroeconomicIndicator> indicators = csvProcessingService.loadDataFromCsv(csvFileName);
        List<MacroeconomicIndicator> validIndicators = csvProcessingService.filterValidData(indicators);
        
        logger.info("Processing {} valid indicators", validIndicators.size());
        
        // Convert to Document objects
        List<Document> documents = validIndicators.stream()
                .map(this::convertToDocument)
                .toList();
        
        // Store in vector store
        vectorStore.add(documents);
        logger.info("Successfully stored {} documents in Redis vector store", documents.size());
    }
    
    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold) {
        logger.info("Searching for documents similar to: {}", query);
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        logger.info("Found {} similar documents", results.size());
        
        return results;
    }
    
    public List<Document> searchWithMetadataFilter(String query, int topK, String year) {
        logger.info("Searching for documents similar to: {} for year: {}", query, year);
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.7)
                .filterExpression("year == '" + year + "'")
                .build();
        
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        logger.info("Found {} similar documents for year {}", results.size(), year);
        
        return results;
    }
    
    private Document convertToDocument(MacroeconomicIndicator indicator) {
        // Create document content
        String content = indicator.toDocumentText();
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("indicator", indicator.getIndicators());
        metadata.put("units", indicator.getUnits());
        metadata.put("year", indicator.getYear());
        metadata.put("value", indicator.getValue());
        metadata.put("source", "macroeconomic-indicator-2007-2017-by-monetary-sector.csv");
        
        return new Document(content, metadata);
    }
}
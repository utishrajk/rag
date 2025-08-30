package com.example.rag;

import com.example.rag.model.MacroeconomicIndicator;
import com.example.rag.service.CsvProcessingService;
import com.example.rag.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorStoreServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private CsvProcessingService csvProcessingService;

    @InjectMocks
    private VectorStoreService vectorStoreService;

    @Test
    void testLoadAndStoreDocuments() {
        // Arrange
        String csvFileName = "test.csv";
        List<MacroeconomicIndicator> mockIndicators = Arrays.asList(
            new MacroeconomicIndicator("Revenues", "Annual % Change", "2007/08", "22.7"),
            new MacroeconomicIndicator("Total Government Expenditures", "Annual % Change", "2007/08", "20.8")
        );

        when(csvProcessingService.loadDataFromCsv(csvFileName)).thenReturn(mockIndicators);
        when(csvProcessingService.filterValidData(mockIndicators)).thenReturn(mockIndicators);
        doNothing().when(vectorStore).add(any());

        // Act
        vectorStoreService.loadAndStoreDocuments(csvFileName);

        // Assert
        verify(csvProcessingService).loadDataFromCsv(csvFileName);
        verify(csvProcessingService).filterValidData(mockIndicators);
        verify(vectorStore).add(argThat(documents -> 
            documents.size() == 2 && 
            ((List<Document>) documents).get(0).getText().contains("Revenues")
        ));
    }

    @Test
    void testSearchSimilarDocuments() {
        // Arrange
        String query = "government revenue";
        int topK = 3;
        double threshold = 0.7;

        Document mockDoc1 = new Document("In 2007/08, Revenues was 22.7 Annual % Change", 
            Map.of("indicator", "Revenues", "year", "2007/08"));
        Document mockDoc2 = new Document("In 2007/08, Total Government Expenditures was 20.8 Annual % Change", 
            Map.of("indicator", "Total Government Expenditures", "year", "2007/08"));
        
        List<Document> mockResults = Arrays.asList(mockDoc1, mockDoc2);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockResults);

        // Act
        List<Document> results = vectorStoreService.searchSimilarDocuments(query, topK, threshold);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("In 2007/08, Revenues was 22.7 Annual % Change", results.get(0).getText());
        
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testSearchWithMetadataFilter() {
        // Arrange
        String query = "expenditure";
        String year = "2007/08";
        int topK = 5;

        Document mockDoc = new Document("In 2007/08, Total Government Expenditures was 20.8 Annual % Change", 
            Map.of("indicator", "Total Government Expenditures", "year", "2007/08"));
        
        List<Document> mockResults = Arrays.asList(mockDoc);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockResults);

        // Act
        List<Document> results = vectorStoreService.searchWithMetadataFilter(query, topK, year);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("2007/08", results.get(0).getMetadata().get("year"));
        
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }
}
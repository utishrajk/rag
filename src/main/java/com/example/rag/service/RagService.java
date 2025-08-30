package com.example.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String SYSTEM_PROMPT = """
            You are an AI assistant specialized in analyzing macroeconomic data.
            You will be provided with relevant economic indicators and data points to answer user questions.
            
            Instructions:
            1. Use only the provided context to answer questions
            2. If the context doesn't contain enough information, say so clearly
            3. Provide specific numbers, years, and indicators when available
            4. Format your response in a clear, professional manner
            5. If asked about trends, compare multiple data points from the context
            
            Context Information:
            {context}
            """;
    
    public String generateResponse(String userQuery) {
        logger.info("Processing RAG query: {}", userQuery);
        
        // Step 1: Retrieve relevant documents from vector store
        List<Document> relevantDocs = retrieveRelevantDocuments(userQuery);
        
        if (relevantDocs.isEmpty()) {
            logger.warn("No relevant documents found for query: {}", userQuery);
            return "I couldn't find any relevant macroeconomic data for your query. Please try rephrasing your question or check if the data has been loaded.";
        }
        
        logger.info("Found {} relevant documents", relevantDocs.size());
        
        // Step 2: Prepare context from retrieved documents
        String context = prepareContext(relevantDocs);
        logger.debug("Prepared context for OpenAI: {}", context);
        
        // Step 3: Create prompt with system message and user query
        String systemPromptWithContext = SYSTEM_PROMPT.replace("{context}", context);
        
        List<Message> messages = List.of(
            new SystemMessage(systemPromptWithContext),
            new UserMessage(userQuery)
        );
        
        Prompt prompt = new Prompt(messages);
        
        // Step 4: Generate response using ChatClient
        try {
            logger.info("Sending request to OpenAI Chat API");
            logger.debug("OpenAI Request - System Message: {}", systemPromptWithContext);
            logger.debug("OpenAI Request - User Message: {}", userQuery);
            logger.debug("OpenAI Request - Full Prompt: {}", prompt.toString());
            
            long startTime = System.currentTimeMillis();
            String generatedResponse = chatClient.prompt(prompt).call().content();
            long endTime = System.currentTimeMillis();
            
            logger.info("OpenAI API call completed in {} ms", endTime - startTime);
            logger.debug("OpenAI Response: {}", generatedResponse);
            logger.info("Successfully generated response");
            return generatedResponse;
            
        } catch (Exception e) {
            logger.error("Error generating response from OpenAI: {}", e.getMessage(), e);
            return "I encountered an error while processing your request. Please try again later.";
        }
    }
    
    private List<Document> retrieveRelevantDocuments(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)  // Retrieve top 5 most relevant documents
                .similarityThreshold(0.6)  // Lower threshold for broader context
                .build();
        
        return vectorStore.similaritySearch(searchRequest);
    }
    
    private String prepareContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    String content = doc.getText();
                    String metadata = formatMetadata(doc);
                    return content + " " + metadata;
                })
                .collect(Collectors.joining("\n\n"));
    }
    
    public String generateExternalResponse(String userQuery, String externalUrl) {
        logger.info("Processing external LLM query: {} to URL: {}", userQuery, externalUrl);
        
        // Step 1: Retrieve relevant documents from vector store
        List<Document> relevantDocs = retrieveRelevantDocuments(userQuery);
        
        if (relevantDocs.isEmpty()) {
            logger.warn("No relevant documents found for external query: {}", userQuery);
            return "I couldn't find any relevant macroeconomic data for your query. Please try rephrasing your question or check if the data has been loaded.";
        }
        
        logger.info("Found {} relevant documents for external query", relevantDocs.size());
        
        // Step 2: Prepare context from retrieved documents
        String context = prepareContext(relevantDocs);
        logger.debug("Prepared context for external LLM: {}", context);
        
        // Step 3: Create request payload for external LLM
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("prompt", userQuery);
        requestPayload.put("context", context);
        requestPayload.put("system_message", "You are an AI assistant specialized in analyzing macroeconomic data. Use the provided context to answer the user's question.");
        
        // Step 4: Call external LLM API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);
            
            logger.info("Sending request to external LLM at: {}", externalUrl);
            logger.debug("External LLM Request payload: {}", requestPayload);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.exchange(
                externalUrl, 
                HttpMethod.POST, 
                requestEntity, 
                Map.class
            );
            long endTime = System.currentTimeMillis();
            
            logger.info("External LLM API call completed in {} ms", endTime - startTime);
            logger.debug("External LLM Response: {}", response.getBody());
            
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                String generatedResponse = response.getBody().get("response").toString();
                logger.info("Successfully generated response from external LLM");
                return generatedResponse;
            } else {
                logger.warn("External LLM response format unexpected: {}", response.getBody());
                return "External LLM returned an unexpected response format.";
            }
            
        } catch (Exception e) {
            logger.error("Error calling external LLM at {}: {}", externalUrl, e.getMessage(), e);
            return "I encountered an error while calling the external LLM. Please check the URL and try again.";
        }
    }
    
    private String formatMetadata(Document doc) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("(");
        
        if (doc.getMetadata().containsKey("indicator")) {
            metadata.append("Indicator: ").append(doc.getMetadata().get("indicator")).append(", ");
        }
        if (doc.getMetadata().containsKey("units")) {
            metadata.append("Units: ").append(doc.getMetadata().get("units")).append(", ");
        }
        if (doc.getMetadata().containsKey("year")) {
            metadata.append("Year: ").append(doc.getMetadata().get("year"));
        }
        
        metadata.append(")");
        return metadata.toString();
    }
}
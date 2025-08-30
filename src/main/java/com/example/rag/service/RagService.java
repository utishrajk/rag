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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private ChatClient chatClient;
    
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
        
        // Step 3: Create prompt with system message and user query
        String systemPromptWithContext = SYSTEM_PROMPT.replace("{context}", context);
        
        List<Message> messages = List.of(
            new SystemMessage(systemPromptWithContext),
            new UserMessage(userQuery)
        );
        
        Prompt prompt = new Prompt(messages);
        
        // Step 4: Generate response using ChatClient
        try {
            logger.info("Generating response using OpenAI Chat");
            String generatedResponse = chatClient.prompt(prompt).call().content();
            
            logger.info("Successfully generated response");
            return generatedResponse;
            
        } catch (Exception e) {
            logger.error("Error generating response: {}", e.getMessage(), e);
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
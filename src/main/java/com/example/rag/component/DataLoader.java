package com.example.rag.component;

import com.example.rag.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Value("${app.load-data-on-startup:false}")
    private boolean loadDataOnStartup;
    
    @Override
    public void run(String... args) throws Exception {
        if (loadDataOnStartup) {
            logger.info("Loading CSV data into vector store on startup");
            try {
                vectorStoreService.loadAndStoreDocuments("macroeconimic-indicator-2007-2017-by-monetary-sector.csv");
                logger.info("Data loaded successfully on startup");
            } catch (Exception e) {
                logger.error("Failed to load data on startup: {}", e.getMessage(), e);
            }
        } else {
            logger.info("Data loading on startup is disabled. Use POST /api/rag/load-data to load data manually.");
        }
    }
}
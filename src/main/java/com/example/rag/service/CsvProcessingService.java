package com.example.rag.service;

import com.example.rag.model.MacroeconomicIndicator;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

@Service
public class CsvProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvProcessingService.class);
    
    public List<MacroeconomicIndicator> loadDataFromCsv(String csvFileName) {
        try {
            ClassPathResource resource = new ClassPathResource(csvFileName);
            Reader reader = new InputStreamReader(resource.getInputStream());
            
            List<MacroeconomicIndicator> indicators = new CsvToBeanBuilder<MacroeconomicIndicator>(reader)
                    .withType(MacroeconomicIndicator.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
            
            logger.info("Successfully loaded {} macroeconomic indicators from CSV", indicators.size());
            return indicators;
            
        } catch (Exception e) {
            logger.error("Error loading CSV data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load CSV data", e);
        }
    }
    
    public List<MacroeconomicIndicator> filterValidData(List<MacroeconomicIndicator> indicators) {
        return indicators.stream()
                .filter(indicator -> 
                    indicator.getIndicators() != null && !indicator.getIndicators().trim().isEmpty() &&
                    indicator.getValue() != null && !indicator.getValue().trim().isEmpty() &&
                    !indicator.getValue().equals("-"))
                .toList();
    }
}
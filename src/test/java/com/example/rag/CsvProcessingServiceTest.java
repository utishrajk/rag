package com.example.rag;

import com.example.rag.model.MacroeconomicIndicator;
import com.example.rag.service.CsvProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CsvProcessingServiceTest {

    @InjectMocks
    private CsvProcessingService csvProcessingService;

    @Test
    void testFilterValidData() {
        // Arrange
        List<MacroeconomicIndicator> indicators = Arrays.asList(
            new MacroeconomicIndicator("Revenues", "Annual % Change", "2007/08", "22.7"),
            new MacroeconomicIndicator("", "Annual % Change", "2007/08", "20.8"), // Empty indicator
            new MacroeconomicIndicator("Capital Expenditure", "Annual % Change", "2007/08", "-"), // Dash value
            new MacroeconomicIndicator("Valid Indicator", "Units", "2008/09", "15.5")
        );

        // Act
        List<MacroeconomicIndicator> validData = csvProcessingService.filterValidData(indicators);

        // Assert
        assertEquals(2, validData.size());
        assertEquals("Revenues", validData.get(0).getIndicators());
        assertEquals("Valid Indicator", validData.get(1).getIndicators());
    }

    @Test
    void testLoadDataFromCsv() {
        // Test loading actual CSV file
        assertDoesNotThrow(() -> {
            List<MacroeconomicIndicator> data = csvProcessingService.loadDataFromCsv("macroeconimic-indicator-2007-2017-by-monetary-sector.csv");
            assertNotNull(data);
            assertFalse(data.isEmpty());
        });
    }
}
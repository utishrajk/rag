package com.example.rag.model;

import com.opencsv.bean.CsvBindByName;

public class MacroeconomicIndicator {
    
    @CsvBindByName(column = "Indicators")
    private String indicators;
    
    @CsvBindByName(column = "Units")
    private String units;
    
    @CsvBindByName(column = "Year")
    private String year;
    
    @CsvBindByName(column = "Value")
    private String value;
    
    public MacroeconomicIndicator() {}
    
    public MacroeconomicIndicator(String indicators, String units, String year, String value) {
        this.indicators = indicators;
        this.units = units;
        this.year = year;
        this.value = value;
    }
    
    public String getIndicators() {
        return indicators;
    }
    
    public void setIndicators(String indicators) {
        this.indicators = indicators;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String toDocumentText() {
        return String.format("In %s, %s was %s %s", 
                year, indicators, value, units);
    }
    
    @Override
    public String toString() {
        return "MacroeconomicIndicator{" +
                "indicators='" + indicators + '\'' +
                ", units='" + units + '\'' +
                ", year='" + year + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
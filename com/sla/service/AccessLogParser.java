package com.sla.service;

import com.sla.model.LogRecord;
import com.sla.util.AnsiColors;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccessLogParser {
    
    public List<LogRecord> parseLogs(String filePath) {
        List<LogRecord> logRecords = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split(",", 7);
                    if (parts.length >= 7) {
                        LogRecord record = new LogRecord(
                            LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_DATE_TIME),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            parts[4].trim(),
                            Integer.parseInt(parts[5].trim()),
                            parts[6].trim()
                        );
                        logRecords.add(record);
                    }
                } catch (Exception e) {
                    System.err.println(AnsiColors.YELLOW + "Warning: Failed to parse log line: " + line + AnsiColors.RESET);
                }
            }
        } catch (IOException e) {
            System.err.println(AnsiColors.RED_BOLD + "FATAL: Could not read log file: " + filePath + AnsiColors.RESET);
            System.err.println(AnsiColors.RED + "Error: " + e.getMessage() + AnsiColors.RESET);
            return Collections.emptyList();
        }
        
        return logRecords;
    }
}

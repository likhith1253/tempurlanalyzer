package com.sla.app;

import com.sla.util.AnsiColors;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class AppConfiguration {
    private final Properties properties = new Properties();
    
    public AppConfiguration(String configFilePath) {
        try (FileReader reader = new FileReader(configFilePath)) {
            properties.load(reader);
        } catch (IOException e) {
            System.err.println(AnsiColors.RED_BOLD + "FATAL: config.properties not found. Exiting." + AnsiColors.RESET);
            System.err.println(AnsiColors.RED + "Error: " + e.getMessage() + AnsiColors.RESET);
            System.exit(1);
        }
    }
    
    public String getUsersFile() {
        return properties.getProperty("users.file");
    }
    
    public String getLogFile() {
        return properties.getProperty("log.file");
    }
    
    public String getGeoDataFile() {
        return properties.getProperty("geo.data.file");
    }
    
    public String getBlacklistFile() {
        return properties.getProperty("blacklist.file");
    }
    
    public String getSuspiciousFile() {
        return properties.getProperty("suspicious.file");
    }
    
    public String getConfidentialPatternsFile() {
        return properties.getProperty("confidential.patterns.file");
    }
    
    public String getReportFile() {
        return properties.getProperty("report.file");
    }
    
    public String getQueryLogFile() {
        return properties.getProperty("query.log.file");
    }
    
    public double getThreatScoreThreshold() {
        return Double.parseDouble(properties.getProperty("threat.score.threshold"));
    }
}

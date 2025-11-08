package com.sla.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogRecord implements Comparable<LogRecord> {
    public final LocalDateTime timestamp;
    public final String ipSourceAddress;
    public final String username;
    public final String httpMethod;
    public final String requestPath;
    public final int httpStatusCode;
    public final String messageDetails;
    
    public LogRecord(LocalDateTime timestamp, String ipSourceAddress, String username, 
                     String httpMethod, String requestPath, int httpStatusCode, String messageDetails) {
        this.timestamp = timestamp;
        this.ipSourceAddress = ipSourceAddress;
        this.username = username;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.httpStatusCode = httpStatusCode;
        this.messageDetails = messageDetails;
    }
    
    @Override
    public int compareTo(LogRecord other) {
        // Order records by timestamp only so that lookups using a key
        // constructed with just the timestamp (as used by the demo) work
        // correctly for search and delete operations in the RBT.
        return this.timestamp.compareTo(other.timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LogRecord logRecord = (LogRecord) obj;
        return timestamp.equals(logRecord.timestamp) &&
               ipSourceAddress.equals(logRecord.ipSourceAddress) &&
               username.equals(logRecord.username) &&
               httpMethod.equals(logRecord.httpMethod) &&
               requestPath.equals(logRecord.requestPath) &&
               httpStatusCode == logRecord.httpStatusCode &&
               messageDetails.equals(logRecord.messageDetails);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(timestamp, ipSourceAddress, username, httpMethod, requestPath, httpStatusCode, messageDetails);
    }
    
    @Override
    public String toString() {
        // --- MODIFIED SECTION ---
        // If the username is a hyphen or empty, display a more descriptive placeholder.
        String displayUser = (username == null || username.trim().isEmpty() || username.equals("-"))
            ? "System/VPN"
            : username;

        return String.format("[%s] IP: %-15s | User: %-12s | Method: %-6s | Path: %-30s | Status: %d | Details: %s",
            timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            ipSourceAddress, displayUser, httpMethod, requestPath, httpStatusCode, messageDetails);
    }
}
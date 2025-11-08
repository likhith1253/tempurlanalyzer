package com.sla.util;

import com.sla.model.User;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class UserQueryLogger {
    private final Path logPath;
    
    public UserQueryLogger(String filePath) {
        this.logPath = Paths.get(filePath);
    }
    
    public void log(User user, String query) {
        String logEntry = String.format("%s | UserID: %s | Query: %s%n",
            LocalDateTime.now(ZoneOffset.UTC), user.userId, query);
        try {
            Files.writeString(logPath, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Error writing to query log: " + e.getMessage() + AnsiColors.RESET);
        }
    }
}

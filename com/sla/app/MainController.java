package com.sla.app;

import com.sla.dsa.SelfBalancingSearchTree;
import com.sla.engine.ReportGenerator;
import com.sla.engine.ThreatAnalysisEngine;
import com.sla.model.Geolocation;
import com.sla.model.LogRecord;
import com.sla.model.ScanReport;
import com.sla.model.User;
import com.sla.service.AccessLogParser;
import com.sla.service.BlacklistManager;
import com.sla.service.UserAccessControl;
import com.sla.service.UserAuthService;
import com.sla.util.AnsiColors;
import com.sla.util.ConsoleUIHelper;
import com.sla.util.SampleDataGenerator;
import com.sla.util.UserQueryLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;


public class MainController {
    private final AppConfiguration appConfig;
    private User currentUser;
    private final Scanner scanner = new Scanner(System.in);
    private final String SECRET_KEY_FOR_CIPHER = "SECRET_KEY_FOR_FLA_v11";

    public MainController() {
        this.appConfig = new AppConfiguration("config.properties");
    }

    public void run() {
        ConsoleUIHelper.printToolHeader("🕵️ Security Log Analyzer (SLA) v12.0");
        System.out.println(AnsiColors.CYAN + "This tool performs log analysis to detect and report suspicious activities." + AnsiColors.RESET);
        System.out.println(AnsiColors.YELLOW + "Threat Score Blacklist Threshold: " + appConfig.getThreatScoreThreshold() + AnsiColors.RESET);

        SampleDataGenerator.generateSampleFiles(appConfig);

        UserAuthService authService = new UserAuthService(scanner, appConfig.getUsersFile());
        this.currentUser = authService.login();

        if (currentUser == null) {
            System.out.println(AnsiColors.RED_BOLD + "Authentication failed. Exiting." + AnsiColors.RESET);
            return;
        }

        UserAccessControl accessControl = new UserAccessControl(currentUser);
        UserQueryLogger queryLogger = new UserQueryLogger(appConfig.getQueryLogFile());

        mainMenu(accessControl, queryLogger);
    }

    private void mainMenu(UserAccessControl accessControl, UserQueryLogger queryLogger) {
        while (true) {
            System.out.println("\n" + AnsiColors.CYAN_BOLD + "--- Main Menu ---" + AnsiColors.RESET);
            System.out.println("1. Run Full Forensic Analysis");
            System.out.println("2. Trace IP Activity");
            System.out.println("3. Query Logs by Time Range");
            System.out.println("4. Red-Black Tree Operations Demo");
            System.out.println("5. Exit");
            System.out.print(AnsiColors.YELLOW + "Select an option: " + AnsiColors.RESET);

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    runFullAnalysis(accessControl, queryLogger);
                    break;
                case "2":
                    traceIpPath(accessControl, queryLogger);
                    break;
                case "3":
                    queryLogsByTimeRange(accessControl, queryLogger);
                    break;
                case "4":
                    demonstrateRBTOperations(queryLogger);
                    break;
                case "5":
                    System.out.println(AnsiColors.CYAN + "Exiting Security Log Analyzer." + AnsiColors.RESET);
                    scanner.close();
                    return;
                default:
                    System.out.println(AnsiColors.RED + "Invalid option." + AnsiColors.RESET);
            }
        }
    }

    private void runFullAnalysis(UserAccessControl accessControl, UserQueryLogger queryLogger) {
        queryLogger.log(currentUser, "Ran Full Forensic Analysis");
        long startTime = System.currentTimeMillis();

        ConsoleUIHelper.printSectionTitle("PHASE 1: Initializing System");
        File blacklistFile = new File(appConfig.getBlacklistFile());
        if (blacklistFile.exists()) {
            blacklistFile.delete();
        }
        BlacklistManager blacklistManager = new BlacklistManager(appConfig.getBlacklistFile(), SECRET_KEY_FOR_CIPHER);
        Map<String, Geolocation> geoData = loadGeoData();
        List<Pattern> confidentialPatterns = loadConfidentialPatterns();
        System.out.println(AnsiColors.GREEN + "✔ Data sources loaded." + AnsiColors.RESET);

        ConsoleUIHelper.printSectionTitle("PHASE 2: Log Processing");
        AccessLogParser logParser = new AccessLogParser();
        List<LogRecord> logRecords = logParser.parseLogs(appConfig.getLogFile());

        SelfBalancingSearchTree<LogRecord> logTree = new SelfBalancingSearchTree<>();
        System.out.print("Inserting " + logRecords.size() + " logs into self-balancing tree... ");
        for (LogRecord record : logRecords) {
            logTree.insert(record);
        }
        System.out.println(AnsiColors.GREEN + "✔ Done." + AnsiColors.RESET);

        ConsoleUIHelper.printSectionTitle("PHASE 3: Running Analysis Engine");
        ThreatAnalysisEngine analysisEngine = new ThreatAnalysisEngine(blacklistManager, geoData, confidentialPatterns, appConfig);
        Set<String> alertedIps = new HashSet<>();

        analysisEngine.setLiveAlertCallback((ipAddr, score, ts) -> {
            if (alertedIps.add(ipAddr)) {
                System.out.println(AnsiColors.RED_BOLD + "🚨 LIVE ALERT: IP " +
                    ConsoleUIHelper.formatIPForDisplay(ipAddr, accessControl, true) +
                    " crossed threat threshold!" + AnsiColors.RESET);
            }
        });

        ScanReport report = analysisEngine.analyze(logTree);
        System.out.println(AnsiColors.GREEN + "✔ Core analysis complete." + AnsiColors.RESET);

        long endTime = System.currentTimeMillis();
        report.performanceMetrics.put("Total Analysis Time", String.format("%.2f seconds", (endTime - startTime) / 1000.0));
        report.performanceMetrics.put("Logs Processed", String.valueOf(logRecords.size()));

        ConsoleUIHelper.printSectionTitle("PHASE 4: Analysis Results");

        // Display blacklist summary
        System.out.println(AnsiColors.RED_BOLD + "\n=== BLACKLIST SUMMARY ===" + AnsiColors.RESET);
        System.out.println(AnsiColors.YELLOW + "Total Blacklisted IPs: " + report.finalBlacklist.size() + AnsiColors.RESET);
        System.out.println(AnsiColors.YELLOW + "Newly Blocked IPs: " + report.newlyBlockedIPs.size() + AnsiColors.RESET);

        if (!report.newlyBlockedIPs.isEmpty()) {
            System.out.println(AnsiColors.RED + "\nNewly Blacklisted IPs:" + AnsiColors.RESET);
            for (Map.Entry<String, LocalDateTime> entry : report.newlyBlockedIPs.entrySet()) {
                Geolocation geo = report.ipToGeolocationMap.get(entry.getKey());
                double score = report.ipRiskScores.getOrDefault(entry.getKey(), 0.0);
                System.out.println(String.format("  • %s (%s) - Blocked at %s with score %.2f",
                    ConsoleUIHelper.formatIPForDisplay(entry.getKey(), accessControl, false),
                    geo != null ? geo.city + ", " + geo.country : "Unknown",
                    entry.getValue().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    score
                ));
            }
        }

        System.out.println(AnsiColors.RED_BOLD + "\n=== MOST DANGEROUS IP ===" + AnsiColors.RESET);
        if (!report.ipRiskScores.isEmpty()) {
            String mostDangerousIP = null;
            double highestScore = -1.0;
            for (Map.Entry<String, Double> entry : report.ipRiskScores.entrySet()) {
                if (entry.getValue() > highestScore) {
                    highestScore = entry.getValue();
                    mostDangerousIP = entry.getKey();
                }
            }
            
            if (mostDangerousIP != null) {
                final String targetIP = mostDangerousIP;
                Geolocation geo = report.ipToGeolocationMap.get(mostDangerousIP);
                long intrusionCount = logRecords.stream()
                    .filter(log -> log.ipSourceAddress.equals(targetIP))
                    .count();
                
                System.out.println(String.format("  IP Address: %s", 
                    ConsoleUIHelper.formatIPForDisplay(mostDangerousIP, accessControl, false)));
                System.out.println(String.format("  Location: %s", 
                    geo != null ? geo.city + ", " + geo.country : "Unknown"));
                System.out.println(String.format("  Threat Score: %.2f", highestScore));
                System.out.println(String.format("  Total Intrusions: %d", intrusionCount));
            }
        }

        ConsoleUIHelper.printSectionTitle("PHASE 5: Generating Detailed Report File");
        ReportGenerator reportGenerator = new ReportGenerator(appConfig.getReportFile(), accessControl, confidentialPatterns);
        reportGenerator.generate(report, analysisEngine);
        System.out.println(AnsiColors.GREEN_BOLD + "\n✔ Analysis complete. Detailed report saved to '" + appConfig.getReportFile() + "'." + AnsiColors.RESET);

        System.out.println(AnsiColors.CYAN_BOLD + "\n=== ALL BLACKLISTED IPs ===" + AnsiColors.RESET);
        if (!report.finalBlacklist.isEmpty()) {
            for (String ip : report.finalBlacklist) {
                Geolocation geo = report.ipToGeolocationMap.get(ip);
                double score = report.ipRiskScores.getOrDefault(ip, 0.0);
                final String targetIP = ip;
                long requestCount = logRecords.stream()
                    .filter(log -> log.ipSourceAddress.equals(targetIP))
                    .count();
                
                System.out.println(String.format("  • %s (%s) - Threat Score: %.2f | Requests: %d",
                    ConsoleUIHelper.formatIPForDisplay(ip, accessControl, false),
                    geo != null ? geo.city + ", " + geo.country : "Unknown",
                    score,
                    requestCount
                ));
            }
        } else {
            System.out.println(AnsiColors.GREEN + "  No IPs were blacklisted." + AnsiColors.RESET);
        }

        System.out.println(AnsiColors.CYAN_BOLD + "\n=== IP WITH MOST REQUESTS ===" + AnsiColors.RESET);
        Map<String, Long> requestCounts = new HashMap<>();
        for (LogRecord log : logRecords) {
            requestCounts.merge(log.ipSourceAddress, 1L, Long::sum);
        }
        
        if (!requestCounts.isEmpty()) {
            String mostActiveIP = null;
            long maxRequests = 0;
            for (Map.Entry<String, Long> entry : requestCounts.entrySet()) {
                if (entry.getValue() > maxRequests) {
                    maxRequests = entry.getValue();
                    mostActiveIP = entry.getKey();
                }
            }
            
            if (mostActiveIP != null) {
                Geolocation geo = report.ipToGeolocationMap.get(mostActiveIP);
                double score = report.ipRiskScores.getOrDefault(mostActiveIP, 0.0);
                
                System.out.println(String.format("  IP Address: %s", 
                    ConsoleUIHelper.formatIPForDisplay(mostActiveIP, accessControl, false)));
                System.out.println(String.format("  Location: %s", 
                    geo != null ? geo.city + ", " + geo.country : "Unknown"));
                System.out.println(String.format("  Threat Score: %.2f", score));
                System.out.println(String.format("  Total Requests: %d", maxRequests));
            }
        }

        blacklistManager.saveBlacklist();
        saveSuspiciousList(report.finalSuspiciousIps);
    }

    private void traceIpPath(UserAccessControl accessControl, UserQueryLogger queryLogger) {
        System.out.print(AnsiColors.CYAN + "Enter IP address to trace: " + AnsiColors.RESET);
        String ipToTrace = scanner.nextLine().trim();
        queryLogger.log(currentUser, "Traced IP: " + ipToTrace);

        AccessLogParser logParser = new AccessLogParser();
        List<LogRecord> allLogs = logParser.parseLogs(appConfig.getLogFile());
        Map<String, Geolocation> geoData = loadGeoData();

        List<LogRecord> ipActivity = allLogs.stream()
            .filter(log -> log.ipSourceAddress.equals(ipToTrace))
            .sorted()
            .collect(java.util.stream.Collectors.toList());

        if (ipActivity.isEmpty()) {
            System.out.println(AnsiColors.YELLOW + "No activity found for IP: " + ipToTrace + AnsiColors.RESET);
            return;
        }

        Geolocation geo = geoData.get(BlacklistManager.getIpPrefix(ipToTrace));
        ConsoleUIHelper.printSectionTitle("Activity Trace for " + ipToTrace + " (" + (geo != null ? geo : "Unknown Location") + ")");

        List<String> headers = Arrays.asList("Timestamp", "Method", "Resource", "Status Code", "Details");
        List<List<String>> rows = ipActivity.stream()
            .map(log -> Arrays.asList(
                log.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                log.httpMethod, log.requestPath, String.valueOf(log.httpStatusCode), log.messageDetails
            )).collect(java.util.stream.Collectors.toList());
        ConsoleUIHelper.displayTable("Chronological Event History", headers, rows);
    }

    private void queryLogsByTimeRange(UserAccessControl accessControl, UserQueryLogger queryLogger) {
        queryLogger.log(currentUser, "Queried Logs by Time Range");

        System.out.print(AnsiColors.CYAN + "Enter start timestamp (yyyy-MM-ddTHH:mm:ss): " + AnsiColors.RESET);
        String startInput = scanner.nextLine().trim();
        System.out.print(AnsiColors.CYAN + "Enter end timestamp (yyyy-MM-ddTHH:mm:ss): " + AnsiColors.RESET);
        String endInput = scanner.nextLine().trim();

        LocalDateTime startTime;
        LocalDateTime endTime;

        try {
            startTime = LocalDateTime.parse(startInput, DateTimeFormatter.ISO_DATE_TIME);
            endTime = LocalDateTime.parse(endInput, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.out.println(AnsiColors.RED + "Invalid timestamp format. Please use yyyy-MM-ddTHH:mm:ss" + AnsiColors.RESET);
            return;
        }

        if (startTime.isAfter(endTime)) {
            System.out.println(AnsiColors.RED + "Start time must be before end time." + AnsiColors.RESET);
            return;
        }

        ConsoleUIHelper.printSectionTitle("Loading Logs");
        AccessLogParser logParser = new AccessLogParser();
        List<LogRecord> allLogs = logParser.parseLogs(appConfig.getLogFile());

        SelfBalancingSearchTree<LogRecord> logTree = new SelfBalancingSearchTree<>();
        for (LogRecord record : allLogs) {
            logTree.insert(record);
        }

        // Use efficient range query O(log N + k)
        LogRecord startKey = new LogRecord(startTime, "", "", "", "", 0, "");
        LogRecord endKey = new LogRecord(endTime, "", "", "", "", 0, "");
        List<LogRecord> results = logTree.rangeQuery(startKey, endKey);

        ConsoleUIHelper.printSectionTitle("Time Range Query Results: " + startInput + " to " + endInput);
        System.out.println(AnsiColors.YELLOW + "Found " + results.size() + " log(s) in the specified time range." + AnsiColors.RESET);

        if (!results.isEmpty()) {
            List<String> headers = Arrays.asList("Timestamp", "IP Address", "User", "Method", "Path", "Status", "Details");
            List<List<String>> rows = results.stream()
                .map(log -> Arrays.asList(
                    log.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ConsoleUIHelper.formatIPForDisplay(log.ipSourceAddress, accessControl, false),
                    log.username,
                    log.httpMethod,
                    log.requestPath,
                    String.valueOf(log.httpStatusCode),
                    log.messageDetails
                )).collect(java.util.stream.Collectors.toList());
            ConsoleUIHelper.displayTable("Logs in Time Range", headers, rows);
        }
    }

    private Map<String, Geolocation> loadGeoData() {
        Map<String, Geolocation> data = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(appConfig.getGeoDataFile()))) {
            reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length >= 3) {
                    data.put(parts[0], new Geolocation(parts[0], parts[1], parts[2]));
                }
            }
        } catch (IOException e) {
            System.err.println(AnsiColors.YELLOW + "Warning: Could not load geo_data.csv: " + e.getMessage() + AnsiColors.RESET);
        }
        return data;
    }

    private List<Pattern> loadConfidentialPatterns() {
        List<Pattern> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(appConfig.getConfidentialPatternsFile()))) {
            reader.lines()
                .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                .forEach(line -> patterns.add(Pattern.compile(line.trim(), Pattern.CASE_INSENSITIVE)));
        } catch (IOException e) {
            System.err.println(AnsiColors.YELLOW + "Warning: Could not load confidential_data_requests.txt: " + e.getMessage() + AnsiColors.RESET);
        }
        return patterns;
    }

    private void saveSuspiciousList(Set<String> suspiciousIps) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(appConfig.getSuspiciousFile(), false))) {
            for (String ip : suspiciousIps) {
                writer.write(ip);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Error saving suspicious.txt file: " + e.getMessage() + AnsiColors.RESET);
        }
    }

    private void demonstrateRBTOperations(UserQueryLogger queryLogger) {
        queryLogger.log(currentUser, "Demonstrated Red-Black Tree Operations");

        ConsoleUIHelper.printSectionTitle("RED-BLACK TREE OPERATIONS DEMONSTRATION");
        System.out.println(AnsiColors.CYAN + "Loading logs into Red-Black Tree..." + AnsiColors.RESET);

        AccessLogParser logParser = new AccessLogParser();
        List<LogRecord> allLogs = logParser.parseLogs(appConfig.getLogFile());

        SelfBalancingSearchTree<LogRecord> logTree = new SelfBalancingSearchTree<>();
        for (LogRecord record : allLogs) {
            logTree.insert(record);
        }

        System.out.println(AnsiColors.GREEN + "✔ Tree built successfully!\n" + AnsiColors.RESET);
        
        // --- INTERACTIVE SUB-MENU LOOP ---
        while (true) {
            System.out.println(AnsiColors.YELLOW_BOLD + "\n=== TREE STATISTICS ===" + AnsiColors.RESET);
            System.out.println(String.format("  Total Nodes: %d", logTree.size()));
            System.out.println(String.format("  Tree Height: %d", logTree.getHeight()));
            System.out.println(String.format("  Black Height: %d", logTree.getBlackHeight()));
            
            System.out.println(AnsiColors.CYAN + "\nHere are some sample log records you can use for operations:" + AnsiColors.RESET);
            if(allLogs.size() > 10) {
                 System.out.println(AnsiColors.GRAY + "  (A) " + allLogs.get(allLogs.size() / 3) + AnsiColors.RESET);
                 System.out.println(AnsiColors.GRAY + "  (B) " + allLogs.get(allLogs.size() / 2) + AnsiColors.RESET);
                 System.out.println(AnsiColors.GRAY + "  (C) " + allLogs.get(allLogs.size() * 2 / 3) + AnsiColors.RESET);
            }
            
            System.out.println(AnsiColors.CYAN_BOLD + "\n--- RBT Operations Sub-Menu ---" + AnsiColors.RESET);
            System.out.println("1. Search for a Log Record");
            System.out.println("2. Insert a new Log Record");
            System.out.println("3. Delete a Log Record");
            System.out.println("4. Find Predecessor of a Log Record");
            System.out.println("5. Find Successor of a Log Record");
            System.out.println("6. Validate Tree Integrity");
            System.out.println("7. Back to Main Menu");
            System.out.print(AnsiColors.YELLOW + "Select an option: " + AnsiColors.RESET);

            String choice = scanner.nextLine();

            switch (choice) {
                case "1": // Search
                    handleSearch(logTree);
                    break;
                case "2": // Insert
                    handleInsert(logTree, allLogs);
                    break;
                case "3": // Delete
                    handleDelete(logTree, allLogs);
                    break;
                case "4": // Predecessor
                    handlePredecessor(logTree);
                    break;
                case "5": // Successor
                    handleSuccessor(logTree);
                    break;
                case "6": // Validate
                    handleValidation(logTree);
                    break;
                case "7":
                    System.out.println(AnsiColors.CYAN + "Returning to Main Menu." + AnsiColors.RESET);
                    return;
                default:
                    System.out.println(AnsiColors.RED + "Invalid option." + AnsiColors.RESET);
            }
        }
    }
    
    // --- HELPER METHODS FOR INTERACTIVE DEMO ---

    private LogRecord getLogRecordFromUserInput() {
        System.out.print(AnsiColors.CYAN + "Enter timestamp (yyyy-MM-ddTHH:mm:ss): " + AnsiColors.RESET);
        String tsInput = scanner.nextLine().trim();
        try {
            LocalDateTime timestamp = LocalDateTime.parse(tsInput, DateTimeFormatter.ISO_DATE_TIME);
            // For ADT operations, only the timestamp (key) is needed for comparison.
            // Create a dummy record with only the key.
            return new LogRecord(timestamp, "", "", "", "", 0, "");
        } catch (DateTimeParseException e) {
            System.out.println(AnsiColors.RED + "Invalid timestamp format." + AnsiColors.RESET);
            return null;
        }
    }

    private void handleSearch(SelfBalancingSearchTree<LogRecord> logTree) {
        System.out.println("\n--- Search Operation ---");
        LogRecord key = getLogRecordFromUserInput();
        if (key == null) return;
        
        LogRecord found = logTree.search(key);
        if (found != null) {
            System.out.println(AnsiColors.GREEN_BOLD + "✔ Success: Found log record!" + AnsiColors.RESET);
            System.out.println("  " + found);
        } else {
            System.out.println(AnsiColors.YELLOW + "✗ Result: No log record found with that exact timestamp." + AnsiColors.RESET);
        }
    }

    private void handleInsert(SelfBalancingSearchTree<LogRecord> logTree, List<LogRecord> allLogs) {
        System.out.println("\n--- Insert Operation ---");
        System.out.println(AnsiColors.CYAN + "Creating a new log record to insert..." + AnsiColors.RESET);
        // Ensure the new timestamp is unique to avoid conflicts.
        LogRecord newRecord = new LogRecord(LocalDateTime.now().withNano(0), "8.8.8.8", "demo_user", "PUT", "/api/demo", 201, "Demo Insert");
        System.out.println("  Inserting: " + newRecord);
        
        System.out.println("\n--- Tree State BEFORE Insert ---");
        System.out.println(String.format("  Size: %d, Height: %d", logTree.size(), logTree.getHeight()));

        logTree.insert(newRecord);
        allLogs.add(newRecord); 

        System.out.println("\n--- Tree State AFTER Insert ---");
        System.out.println(String.format("  Size: %d, Height: %d", logTree.size(), logTree.getHeight()));
        
        boolean isValid = logTree.validateTree();
        System.out.println(isValid ? AnsiColors.GREEN_BOLD + "  ✔ Tree remains valid and balanced." : AnsiColors.RED_BOLD + "  ✗ Tree is now invalid!");
    }

    private void handleDelete(SelfBalancingSearchTree<LogRecord> logTree, List<LogRecord> allLogs) {
         System.out.println("\n--- Delete Operation ---");
        LogRecord key = getLogRecordFromUserInput();
        if (key == null) return;

        System.out.println("\n--- Tree State BEFORE Delete ---");
        System.out.println(String.format("  Size: %d, Height: %d", logTree.size(), logTree.getHeight()));

        // Find the full log record to remove from the list later
        LogRecord recordToDelete = logTree.search(key);
        boolean deleted = logTree.delete(key);

        if (deleted) {
            if (recordToDelete != null) {
                 allLogs.remove(recordToDelete);
            }
            System.out.println(AnsiColors.GREEN_BOLD + "✔ Delete successful!" + AnsiColors.RESET);
            System.out.println("\n--- Tree State AFTER Delete ---");
            System.out.println(String.format("  Size: %d, Height: %d", logTree.size(), logTree.getHeight()));
            boolean isValid = logTree.validateTree();
            System.out.println(isValid ? AnsiColors.GREEN_BOLD + "  ✔ Tree remains valid and balanced." : AnsiColors.RED_BOLD + "  ✗ Tree is now invalid!");
        } else {
            System.out.println(AnsiColors.YELLOW + "✗ Result: No log record found to delete." + AnsiColors.RESET);
        }
    }

    private void handlePredecessor(SelfBalancingSearchTree<LogRecord> logTree) {
        System.out.println("\n--- Find Predecessor Operation ---");
        LogRecord key = getLogRecordFromUserInput();
        if (key == null) return;

        LogRecord predecessor = logTree.findPredecessor(key);
        if (predecessor != null) {
            System.out.println(AnsiColors.GREEN + "✔ Predecessor found:" + AnsiColors.RESET);
            System.out.println("  " + predecessor);
        } else {
            System.out.println(AnsiColors.YELLOW + "✗ Result: No predecessor found (it might be the minimum element)." + AnsiColors.RESET);
        }
    }

    private void handleSuccessor(SelfBalancingSearchTree<LogRecord> logTree) {
        System.out.println("\n--- Find Successor Operation ---");
        LogRecord key = getLogRecordFromUserInput();
        if (key == null) return;

        LogRecord successor = logTree.findSuccessor(key);
        if (successor != null) {
            System.out.println(AnsiColors.GREEN + "✔ Successor found:" + AnsiColors.RESET);
            System.out.println("  " + successor);
        } else {
            System.out.println(AnsiColors.YELLOW + "✗ Result: No successor found (it might be the maximum element)." + AnsiColors.RESET);
        }
    }

    // --- MODIFIED METHOD ---
    private void handleValidation(SelfBalancingSearchTree<LogRecord> logTree) {
        System.out.println(AnsiColors.YELLOW_BOLD + "\n=== TREE VALIDATION ===" + AnsiColors.RESET);
        boolean isValid = logTree.validateTree();
        if (isValid) {
            System.out.println(AnsiColors.GREEN_BOLD + "  ✓ Tree satisfies all Red-Black Tree properties!" + AnsiColors.RESET);
            System.out.println(AnsiColors.GREEN + "    1. Every node is either Red or Black." + AnsiColors.RESET);
            System.out.println(AnsiColors.GREEN + "    2. The root node is Black." + AnsiColors.RESET);
            System.out.println(AnsiColors.GREEN + "    3. All leaves (NIL) are considered Black." + AnsiColors.RESET);
            System.out.println(AnsiColors.GREEN + "    4. If a node is Red, then both of its children are Black." + AnsiColors.RESET);
            System.out.println(AnsiColors.GREEN + "    5. Every path from a node to its descendant leaves contains the same number of Black nodes." + AnsiColors.RESET);
        } else {
            System.out.println(AnsiColors.RED_BOLD + "  ✗ Tree validation failed! The structure is compromised." + AnsiColors.RESET);
        }
    }
}
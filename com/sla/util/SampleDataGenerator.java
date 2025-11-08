package com.sla.util;

import com.sla.app.AppConfiguration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SampleDataGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static void generateSampleFiles(AppConfiguration config) {
        generateUsersFile(config.getUsersFile());
        generateGeoDataFile(config.getGeoDataFile());
        generateConfidentialPatternsFile(config.getConfidentialPatternsFile());
        generateLogsFile(config.getLogFile());
    }

    private static void generateUsersFile(String filePath) {
        if (new File(filePath).exists()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("username,password,role,userid\n");
            writer.write("admin,admin123,ADMIN,ADM001\n");
            writer.write("investigator,inv123,INVESTIGATOR,INV001\n");
            writer.write("auditor,audit123,AUDITOR,AUD001\n");
        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Warning: Could not generate " + filePath + ": " + e.getMessage() + AnsiColors.RESET);
        }
    }

    private static void generateGeoDataFile(String filePath) {
        if (new File(filePath).exists()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ip_prefix,country,city\n");

            // 50+ diverse geolocations from around the world
            String[][] geoData = {
                {"45.142.120", "RU", "Moscow"},
                {"185.220.101", "DE", "Frankfurt"},
                {"91.219.236", "RU", "St Petersburg"},
                {"89.248.165", "RU", "Novosibirsk"},
                {"193.239.147", "NL", "Amsterdam"},
                {"104.244.42", "US", "Los Angeles"},
                {"167.94.138", "US", "New York"},
                {"198.98.54", "US", "Chicago"},
                {"51.89.115", "FR", "Paris"},
                {"178.62.193", "UK", "London"},
                {"195.123.220", "PL", "Warsaw"},
                {"46.101.126", "DE", "Berlin"},
                {"159.89.214", "US", "San Francisco"},
                {"209.141.45", "US", "Dallas"},
                {"142.93.125", "CA", "Toronto"},
                {"188.166.89", "SG", "Singapore"},
                {"139.59.10", "IN", "Bangalore"},
                {"103.253.145", "CN", "Beijing"},
                {"221.194.47", "CN", "Shanghai"},
                {"125.214.87", "KR", "Seoul"},
                {"210.15.142", "JP", "Tokyo"},
                {"114.119.136", "CN", "Shenzhen"},
                {"180.163.220", "CN", "Hangzhou"},
                {"203.113.162", "TH", "Bangkok"},
                {"118.107.243", "VN", "Hanoi"},
                {"49.156.23", "IN", "Mumbai"},
                {"122.161.93", "IN", "Delhi"},
                {"182.73.214", "IN", "Hyderabad"},
                {"202.47.38", "AU", "Sydney"},
                {"103.28.121", "AU", "Melbourne"},
                {"41.60.234", "ZA", "Johannesburg"},
                {"105.112.33", "ZA", "Cape Town"},
                {"197.234.221", "NG", "Lagos"},
                {"196.201.214", "EG", "Cairo"},
                {"200.98.133", "BR", "Sao Paulo"},
                {"186.148.172", "BR", "Rio de Janeiro"},
                {"190.14.36", "AR", "Buenos Aires"},
                {"187.188.165", "MX", "Mexico City"},
                {"177.54.148", "BR", "Brasilia"},
                {"201.216.243", "CL", "Santiago"},
                {"85.132.6", "ES", "Madrid"},
                {"95.216.145", "FI", "Helsinki"},
                {"217.12.210", "SE", "Stockholm"},
                {"80.94.92", "NO", "Oslo"},
                {"213.108.105", "IT", "Rome"},
                {"151.236.219", "IT", "Milan"},
                {"77.37.160", "CH", "Zurich"},
                {"87.230.14", "AT", "Vienna"},
                {"78.142.19", "RO", "Bucharest"},
                {"212.58.244", "TR", "Istanbul"},
                {"31.145.191", "UA", "Kyiv"},
                {"93.170.122", "GR", "Athens"},
                {"192.168.1", "US", "Private Network"},
                {"10.0.0", "US", "Private Network"},
                {"172.16.5", "US", "Private Network"}
            };

            for (String[] geo : geoData) {
                writer.write(String.format("%s,%s,%s\n", geo[0], geo[1], geo[2]));
            }
        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Warning: Could not generate " + filePath + ": " + e.getMessage() + AnsiColors.RESET);
        }
    }

    private static void generateConfidentialPatternsFile(String filePath) {
        if (new File(filePath).exists()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("/admin\n");
            writer.write("/config\n");
            writer.write("/secret\n");
            writer.write("/private\n");
            writer.write("\\.env\n");
            writer.write("/credentials\n");
            writer.write("/api/keys\n");
            writer.write("/database\n");
            writer.write("/backup\n");
            writer.write("/internal\n");
        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Warning: Could not generate " + filePath + ": " + e.getMessage() + AnsiColors.RESET);
        }
    }

    private static void generateLogsFile(String filePath) {
        File logFile = new File(filePath);
        if (logFile.exists()) {
            logFile.delete();
        }

        // Generate random IP addresses for different threat profiles
        String[] ipPrefixes = {
            "45.142.120", "91.219.236", "89.248.165", "103.253.145",
            "221.194.47", "125.214.87", "185.220.101", "193.239.147",
            "104.244.42", "178.62.193", "195.123.220", "139.59.10",
            "167.94.138", "198.98.54", "51.89.115", "159.89.214",
            "142.93.125", "188.166.89", "192.168.1", "10.0.0", "172.16.5"
        };
        
        // Generate unique random IPs for this run
        String[] highThreatIPs = new String[20];
        for (int i = 0; i < highThreatIPs.length; i++) {
            String prefix = ipPrefixes[random.nextInt(ipPrefixes.length)];
            highThreatIPs[i] = prefix + "." + (random.nextInt(200) + 1);
        }

        String[] mediumThreatIPs = new String[25];
        for (int i = 0; i < mediumThreatIPs.length; i++) {
            String prefix = ipPrefixes[random.nextInt(ipPrefixes.length)];
            mediumThreatIPs[i] = prefix + "." + (random.nextInt(200) + 1);
        }

        String[] lowThreatIPs = new String[30];
        for (int i = 0; i < lowThreatIPs.length; i++) {
            String prefix = ipPrefixes[random.nextInt(ipPrefixes.length)];
            lowThreatIPs[i] = prefix + "." + (random.nextInt(200) + 1);
        }

        String[] normalUsers = {"alice", "bob", "charlie", "david", "emma"};
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

        String[] normalPaths = {
            "/api/users", "/api/products", "/api/orders", "/dashboard",
            "/profile", "/settings", "/help", "/about", "/contact"
        };

        String[] sensitivePaths = {
            "/admin/users", "/admin/logs", "/config/database", "/secret/keys",
            "/private/data", "/credentials/list", "/api/keys", "/backup/restore",
            "/.env", "/internal/metrics"
        };

        String[] attackPaths = {
            "/admin/../../../etc/passwd", "/api/users?id=1' OR '1'='1",
            "/config/settings?file=../../../../etc/shadow",
            "/api/exec?cmd=rm -rf /", "/.git/config"
        };

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            LocalDateTime baseTime = LocalDateTime.now().withNano(0).minusHours(24 * 30); // A month of logs, truncated to seconds
            int logCount = 0;
            
            // --- MODIFIED LINE ---
            // Increased total logs to 3 million to slow down the analysis.
            int totalLogs = 3000000;

            // Proportional distribution
            int highThreatCount = (int) (totalLogs * 0.33);
            int mediumThreatCount = (int) (totalLogs * 0.27);
            int lowThreatCount = totalLogs - highThreatCount - mediumThreatCount;


            // Generate high-threat activity
            for (int i = 0; i < highThreatCount; i++) {
                String ip = highThreatIPs[random.nextInt(highThreatIPs.length)];
                LocalDateTime timestamp = baseTime.plusSeconds(logCount++);

                if (i % 5 == 0) {
                    // Confidential resource access
                    String path = sensitivePaths[random.nextInt(sensitivePaths.length)];
                    writer.write(String.format("%s,%s,-,%s,%s,%d,Confidential resource access\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, methods[random.nextInt(methods.length)], path, 403));
                } else if (i % 4 == 0) {
                    // SQL injection attempts
                    String path = attackPaths[random.nextInt(attackPaths.length)];
                    writer.write(String.format("%s,%s,-,POST,%s,%d,SQL injection attempt detected\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, path, 400));
                } else if (i % 3 == 0) {
                    // Failed login attempts
                    writer.write(String.format("%s,%s,-,POST,/api/login,%d,Failed login attempt\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, 401));
                } else {
                    // Server errors
                    String path = normalPaths[random.nextInt(normalPaths.length)];
                    writer.write(String.format("%s,%s,-,%s,%s,%d,Internal server error\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, methods[random.nextInt(methods.length)], path, 500));
                }
            }

            // Generate medium-threat activity
            for (int i = 0; i < mediumThreatCount; i++) {
                String ip = mediumThreatIPs[random.nextInt(mediumThreatIPs.length)];
                LocalDateTime timestamp = baseTime.plusSeconds(logCount++);

                if (i % 3 == 0) {
                    // Failed login
                    writer.write(String.format("%s,%s,-,POST,/api/login,%d,Failed login attempt\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, 401));
                } else if (i % 5 == 0) {
                    // Unauthorized access
                    String path = sensitivePaths[random.nextInt(sensitivePaths.length)];
                    writer.write(String.format("%s,%s,-,%s,%s,%d,Unauthorized access\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, methods[random.nextInt(methods.length)], path, 403));
                } else {
                    // 404 errors
                    String path = normalPaths[random.nextInt(normalPaths.length)];
                    writer.write(String.format("%s,%s,%s,%s,%s,%d,Not found\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, normalUsers[random.nextInt(normalUsers.length)],
                        methods[random.nextInt(methods.length)], path, 404));
                }
            }

            // Generate low-threat/normal activity
            for (int i = 0; i < lowThreatCount; i++) {
                String ip = lowThreatIPs[random.nextInt(lowThreatIPs.length)];
                LocalDateTime timestamp = baseTime.plusSeconds(logCount++);

                if (i % 10 == 0) {
                    // Occasional 404
                    String path = normalPaths[random.nextInt(normalPaths.length)];
                    writer.write(String.format("%s,%s,%s,GET,%s,%d,Not found\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, normalUsers[random.nextInt(normalUsers.length)], path, 404));
                } else {
                    // Normal successful requests
                    String path = normalPaths[random.nextInt(normalPaths.length)];
                    String user = normalUsers[random.nextInt(normalUsers.length)];
                    int status = (i % 2 == 0) ? 200 : 201;
                    writer.write(String.format("%s,%s,%s,%s,%s,%d,Success\n",
                        timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                        ip, user, methods[random.nextInt(methods.length)], path, status));
                }
            }

        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Warning: Could not generate " + filePath + ": " + e.getMessage() + AnsiColors.RESET);
        }
    }
}
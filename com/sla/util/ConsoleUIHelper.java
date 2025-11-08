package com.sla.util;

import com.sla.service.UserAccessControl;
import java.util.ArrayList;
import java.util.List;

public class ConsoleUIHelper {
    
    public static void printToolHeader(String title) {
        String border = "=".repeat(title.length() + 4);
        System.out.println(AnsiColors.YELLOW_BOLD + border + "\n= " + title + " =\n" + border + AnsiColors.RESET);
    }
    
    public static void printSectionTitle(String title) {
        System.out.println("\n" + AnsiColors.CYAN_BOLD + "--- " + title + " ---" + AnsiColors.RESET);
    }
    
    public static void displayTable(String tableTitle, List<String> headers, List<List<String>> tableRows) {
        System.out.println(AnsiColors.WHITE_BOLD + "\n" + tableTitle + AnsiColors.RESET);
        
        if (tableRows.isEmpty()) {
            System.out.println("  No data to display.");
            return;
        }
        
        // Calculate column widths
        List<Integer> columnWidths = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            int maxWidth = headers.get(i).length();
            for (List<String> row : tableRows) {
                if (i < row.size() && row.get(i) != null) {
                    maxWidth = Math.max(maxWidth, row.get(i).length());
                }
            }
            columnWidths.add(maxWidth);
        }
        
        // Print header line
        StringBuilder headerLine = new StringBuilder("  ");
        for (int i = 0; i < headers.size(); i++) {
            headerLine.append(String.format("%-" + (columnWidths.get(i) + 2) + "s", headers.get(i)));
        }
        System.out.println(AnsiColors.YELLOW + headerLine.toString() + AnsiColors.RESET);
        
        // Print data rows
        for (List<String> row : tableRows) {
            StringBuilder rowLine = new StringBuilder("  ");
            for (int i = 0; i < row.size(); i++) {
                rowLine.append(String.format("%-" + (columnWidths.get(i) + 2) + "s", row.get(i)));
            }
            System.out.println(rowLine.toString());
        }
    }
    
    public static String formatIPForDisplay(String ipAddress, UserAccessControl accessControl, boolean padded) {
        if (ipAddress == null) {
            return "N/A";
        }
        String redactedIP = accessControl.redactIPAddress(ipAddress);
        return padded ? String.format("%-15s", redactedIP) : redactedIP;
    }
}

package com.sla.service;

import com.sla.dsa.SelfBalancingSearchTree;
import com.sla.model.BlockedIPAddress;
import com.sla.util.AnsiColors;
import com.sla.util.SimpleXorCipher;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class BlacklistManager {
    private final SelfBalancingSearchTree<BlockedIPAddress> blacklistTree = new SelfBalancingSearchTree<>();
    private final String filePath;
    private final String encryptionKey;
    
    public BlacklistManager(String filePath, String encryptionKey) {
        this.filePath = filePath;
        this.encryptionKey = encryptionKey;
        loadBlacklist();
    }
    
    public void add(String ipAddress) {
        blacklistTree.insert(new BlockedIPAddress(ipAddress));
    }
    
    public boolean isBlacklisted(String ipAddress) {
        return blacklistTree.inOrderTraversal().stream()
            .anyMatch(blockedIP -> blockedIP.ipAddress.equals(ipAddress));
    }
    
    public Set<String> getActiveBlacklist() {
        return blacklistTree.inOrderTraversal().stream()
            .map(blockedIP -> blockedIP.ipAddress)
            .collect(Collectors.toSet());
    }
    
    public static String getIpPrefix(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        int lastDot = ipAddress.lastIndexOf('.');
        return (lastDot != -1) ? ipAddress.substring(0, lastDot) : null;
    }
    
    private void loadBlacklist() {
        if (!new File(filePath).exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.lines()
                .map(line -> SimpleXorCipher.decrypt(line, encryptionKey))
                .forEach(this::add);
        } catch (IOException e) {
            System.err.println(AnsiColors.YELLOW + "Warning: Could not load blacklist from " + filePath + ": " + e.getMessage() + AnsiColors.RESET);
        }
    }
    
    public void saveBlacklist() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (BlockedIPAddress blockedIP : blacklistTree.inOrderTraversal()) {
                writer.write(SimpleXorCipher.encrypt(blockedIP.ipAddress, encryptionKey));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println(AnsiColors.RED + "Error saving blacklist to " + filePath + ": " + e.getMessage() + AnsiColors.RESET);
        }
    }
}

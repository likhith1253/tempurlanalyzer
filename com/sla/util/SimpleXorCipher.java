package com.sla.util;

public class SimpleXorCipher {
    
    public static String encrypt(String plainText, String key) {
        StringBuilder encryptedText = new StringBuilder();
        for (int i = 0; i < plainText.length(); i++) {
            char encryptedChar = (char) (plainText.charAt(i) ^ key.charAt(i % key.length()));
            encryptedText.append(encryptedChar);
        }
        return encryptedText.toString();
    }
    
    public static String decrypt(String encryptedText, String key) {
        // XOR is symmetric, so encryption and decryption are the same operation
        return encrypt(encryptedText, key);
    }
}

package com.sentinelai.guard.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.Message;
import com.sentinelai.guard.model.Alert;
import com.sentinelai.guard.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseService {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseMessaging firebaseMessaging;

    // User Management
    public UserRecord createUser(String email, String password, String displayName, UserRole role) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName)
                .setDisabled(false);

        UserRecord userRecord = firebaseAuth.createUser(request);
        
        // Set custom claims for role-based access
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);

        return userRecord;
    }

    public UserRecord getUser(String uid) throws FirebaseAuthException {
        return firebaseAuth.getUser(uid);
    }

    public void deleteUser(String uid) throws FirebaseAuthException {
        firebaseAuth.deleteUser(uid);
    }

    // Cloud Messaging
    public void sendNotificationToUser(String userId, String title, String body, Map<String, String> data) {
        try {
            // In a real app, you would get the FCM token from your users collection
            // For now, this is a placeholder
            String registrationToken = getFcmTokenForUser(userId);
            
            if (registrationToken == null) {
                log.warn("No FCM token found for user: {}", userId);
                return;
            }

            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(registrationToken)
                    .setNotification(notification)
                    .putAllData(data != null ? data : new HashMap<>())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    public void sendAlertNotification(Alert alert) {
        String title = "Security Alert: " + alert.getType();
        String body = alert.getDescription(); // Using description instead of getMessage()
        
        Map<String, String> data = new HashMap<>();
        data.put("alertId", alert.getId());
        data.put("type", alert.getType().name());
        data.put("severity", alert.getSeverity().name());
        data.put("timestamp", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : new Date().toString());
        
        // Send to all admins or specific users based on alert configuration
        getUserIdsToNotify(alert).forEach(userId -> 
            sendNotificationToUser(userId, title, body, data)
        );
    }

    // Helper methods
    private String getFcmTokenForUser(String userId) {
        // In a real app, you would fetch this from your users collection
        // This is a placeholder implementation
        return null;
    }

    private List<String> getUserIdsToNotify(Alert alert) {
        // In a real app, you would query your database for users who should receive this alert
        // For example, all admins or users subscribed to this domain
        // This is a simplified example that would notify all admin users
        List<String> adminUserIds = new ArrayList<>();
        try {
            // Get all users (paginated)
            ListUsersPage page = firebaseAuth.listUsers(null);
            for (UserRecord user : page.iterateAll()) {
                Map<String, Object> claims = user.getCustomClaims();
                if (claims != null && 
                    claims.containsKey("role") && 
                    UserRole.ADMIN.name().equals(claims.get("role").toString())) {
                    adminUserIds.add(user.getUid());
                }
            }
            
            // Handle pagination if there are more users
            while (page.getNextPageToken() != null && !page.getNextPageToken().isEmpty()) {
                page = firebaseAuth.listUsers(page.getNextPageToken());
                for (UserRecord user : page.iterateAll()) {
                    Map<String, Object> claims = user.getCustomClaims();
                    if (claims != null && 
                        claims.containsKey("role") && 
                        UserRole.ADMIN.name().equals(claims.get("role").toString())) {
                        adminUserIds.add(user.getUid());
                    }
                }
            }
            
            return adminUserIds;
        } catch (FirebaseAuthException e) {
            log.error("Error fetching admin users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}

package com.sentinelai.guard.controller;

import com.sentinelai.guard.model.Alert;
import com.sentinelai.guard.service.FirebaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final FirebaseService firebaseService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendNotification(
            @RequestParam String userId,
            @RequestParam String title,
            @RequestParam String message) {
        try {
            firebaseService.sendNotificationToUser(userId, title, message, null);
            return ResponseEntity.ok().body("Notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to send notification: " + e.getMessage());
        }
    }

    @PostMapping("/alert")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendAlertNotification(@RequestBody Alert alert) {
        try {
            firebaseService.sendAlertNotification(alert);
            return ResponseEntity.ok().body("Alert notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send alert notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to send alert notification: " + e.getMessage());
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> saveFcmToken(
            @RequestParam String userId,
            @RequestParam String fcmToken) {
        try {
            // In a real app, you would save this token to your database
            // associated with the user
            log.info("Received FCM token {} for user {}", fcmToken, userId);
            return ResponseEntity.ok().body("FCM token received successfully");
        } catch (Exception e) {
            log.error("Failed to save FCM token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to save FCM token: " + e.getMessage());
        }
    }
}

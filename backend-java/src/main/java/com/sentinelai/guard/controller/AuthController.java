package com.sentinelai.guard.controller;

import com.sentinelai.guard.model.UserRole;
import com.sentinelai.guard.service.FirebaseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final FirebaseService firebaseService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            // Default role is USER unless specified otherwise
            UserRole role = request.getRole() != null ? request.getRole() : UserRole.USER;
            
            var userRecord = firebaseService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                role
            );

            return ResponseEntity.ok(new AuthResponse(
                userRecord.getUid(),
                userRecord.getEmail(),
                userRecord.getDisplayName(),
                role
            ));
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to create user: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        // Firebase Admin SDK doesn't handle login directly
        // This endpoint is a placeholder - the actual login will be handled by Firebase Client SDK
        return ResponseEntity.ok().body(new MessageResponse("Please use Firebase Client SDK for login"));
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 6, max = 100)
        private String password;

        @NotBlank
        private String displayName;

        private UserRole role;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    @RequiredArgsConstructor
    public static class AuthResponse {
        private final String uid;
        private final String email;
        private final String displayName;
        private final UserRole role;
    }

    @Data
    @RequiredArgsConstructor
    public static class MessageResponse {
        private final String message;
    }

    @Data
    @RequiredArgsConstructor
    public static class ErrorResponse {
        private final String error;
    }
}

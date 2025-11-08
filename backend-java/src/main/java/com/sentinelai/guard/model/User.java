package com.sentinelai.guard.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseFirestoreModel {
    public enum UserRole {
        ADMIN, ANALYST, VIEWER, API_CLIENT
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_ACTIVATION
    }

    @DocumentId
    private String id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Set<UserRole> roles;
    private UserStatus status;
    private Map<String, Object> preferences;
    private String authProvider; // e.g., "firebase", "google", "github"
    private String authUid; // UID from the auth provider
    private Date lastLoginAt;
    private List<String> teams; // Team IDs the user belongs to
    private Map<String, Object> metadata; // Additional user metadata

    public static User fromMap(Map<String, Object> map) {
        User user = new User();
        user.setId((String) map.get("id"));
        user.setEmail((String) map.get("email"));
        user.setDisplayName((String) map.get("displayName"));
        user.setAvatarUrl((String) map.get("avatarUrl"));
        
        // Convert roles from List<String> to Set<UserRole>
        Set<UserRole> roles = new HashSet<>();
        if (map.get("roles") instanceof List) {
            for (Object role : (List<?>) map.get("roles")) {
                if (role instanceof String) {
                    try {
                        roles.add(UserRole.valueOf(((String) role).toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid roles
                    }
                }
            }
        }
        user.setRoles(roles);
        
        // Set status with null check
        if (map.get("status") != null) {
            try {
                user.setStatus(UserStatus.valueOf(((String) map.get("status")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                user.setStatus(UserStatus.PENDING_ACTIVATION);
            }
        } else {
            user.setStatus(UserStatus.PENDING_ACTIVATION);
        }
        
        // Handle preferences with type safety
        Object preferences = map.get("preferences");
        if (preferences instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> prefsMap = (Map<String, Object>) preferences;
            user.setPreferences(prefsMap);
        } else {
            user.setPreferences(new HashMap<>());
        }
        
        user.setAuthProvider((String) map.get("authProvider"));
        user.setAuthUid((String) map.get("authUid"));
        
        // Handle timestamps
        if (map.get("lastLoginAt") != null) {
            user.setLastLoginAt(((Timestamp) map.get("lastLoginAt")).toDate());
        }
        
        // Handle teams list with type safety
        Object teams = map.get("teams");
        if (teams instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> teamsList = (List<String>) teams;
            user.setTeams(teamsList);
        } else {
            user.setTeams(new ArrayList<>());
        }
        
        // Handle metadata with type safety
        Object metadata = map.get("metadata");
        if (metadata instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = (Map<String, Object>) metadata;
            user.setMetadata(metadataMap);
        } else {
            user.setMetadata(new HashMap<>());
        }
        
        // Set creation and update timestamps
        if (map.get("createdAt") != null) {
            user.setCreatedAt(((Timestamp) map.get("createdAt")).toDate());
        } else {
            user.setCreatedAt(new Date());
        }
        
        if (map.get("updatedAt") != null) {
            user.setUpdatedAt(((Timestamp) map.get("updatedAt")).toDate());
        } else {
            user.setUpdatedAt(new Date());
        }
        
        return user;
    }

    public Map<String, Object> toMap() {
        // Convert roles to List<String>
        List<String> rolesList = new ArrayList<>();
        if (roles != null) {
            for (UserRole role : roles) {
                rolesList.add(role.name());
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("avatarUrl", avatarUrl);
        map.put("roles", rolesList);
        map.put("status", status != null ? status.name() : null);
        map.put("preferences", preferences != null ? preferences : new HashMap<>());
        map.put("authProvider", authProvider);
        map.put("authUid", authUid);
        map.put("lastLoginAt", lastLoginAt != null ? Timestamp.of(lastLoginAt) : null);
        map.put("teams", teams != null ? teams : new ArrayList<>());
        map.put("metadata", metadata != null ? metadata : new HashMap<>());
        map.put("createdAt", getCreatedAtTimestamp());
        map.put("updatedAt", getUpdatedAtTimestamp());
        return map;
    }

    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(Set<UserRole> requiredRoles) {
        if (roles == null || requiredRoles == null) {
            return false;
        }
        return !Collections.disjoint(roles, requiredRoles);
    }
}

package com.sentinelai.guard.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Alert extends BaseFirestoreModel {
    public enum AlertType {
        SECURITY, PERFORMANCE, CONFIGURATION, THREAT, COMPLIANCE, OTHER
    }

    public enum AlertStatus {
        OPEN, IN_PROGRESS, RESOLVED, IGNORED, FALSE_POSITIVE
    }

    public enum AlertSeverity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    @DocumentId
    private String id;
    private String title;
    private String description;
    private AlertType type;
    private AlertStatus status;
    private AlertSeverity severity;
    private String source;  // e.g., "firewall", "ids", "manual"
    private String sourceId; // Reference to the source event/alert ID
    private String domainId; // Reference to the related domain
    private String domain;   // Domain name for quick reference
    private String assignedTo; // User ID or team assigned to handle this alert
    private Map<String, Object> details; // Additional alert details
    private Date resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;

    public static Alert fromMap(Map<String, Object> map) {
        Alert alert = new Alert();
        alert.setId((String) map.get("id"));
        alert.setTitle((String) map.get("title"));
        alert.setDescription((String) map.get("description"));
        
        if (map.get("type") != null) {
            alert.setType(AlertType.valueOf((String) map.get("type")));
        }
        if (map.get("status") != null) {
            alert.setStatus(AlertStatus.valueOf((String) map.get("status")));
        }
        if (map.get("severity") != null) {
            alert.setSeverity(AlertSeverity.valueOf((String) map.get("severity")));
        }
        
        alert.setSource((String) map.get("source"));
        alert.setSourceId((String) map.get("sourceId"));
        alert.setDomainId((String) map.get("domainId"));
        alert.setDomain((String) map.get("domain"));
        alert.setAssignedTo((String) map.get("assignedTo"));
        
        // Handle details with type safety
        Object details = map.get("details");
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            alert.setDetails(detailsMap);
        } else {
            alert.setDetails(new HashMap<>());
        }
        
        // Handle timestamps
        if (map.get("resolvedAt") != null) {
            alert.setResolvedAt(((Timestamp) map.get("resolvedAt")).toDate());
        }
        if (map.get("createdAt") != null) {
            alert.setCreatedAt(((Timestamp) map.get("createdAt")).toDate());
        } else {
            alert.setCreatedAt(new Date());
        }
        if (map.get("updatedAt") != null) {
            alert.setUpdatedAt(((Timestamp) map.get("updatedAt")).toDate());
        } else {
            alert.setUpdatedAt(new Date());
        }
        
        alert.setResolvedBy((String) map.get("resolvedBy"));
        alert.setResolutionNotes((String) map.get("resolutionNotes"));
        
        return alert;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("type", type != null ? type.name() : null);
        map.put("status", status != null ? status.name() : null);
        map.put("severity", severity != null ? severity.name() : null);
        map.put("source", source);
        map.put("sourceId", sourceId);
        map.put("domainId", domainId);
        map.put("domain", domain);
        map.put("assignedTo", assignedTo);
        map.put("details", details != null ? details : new HashMap<>());
        map.put("resolvedAt", resolvedAt != null ? Timestamp.of(resolvedAt) : null);
        map.put("resolvedBy", resolvedBy);
        map.put("resolutionNotes", resolutionNotes);
        map.put("createdAt", getCreatedAtTimestamp());
        map.put("updatedAt", getUpdatedAtTimestamp());
        return map;
    }
}

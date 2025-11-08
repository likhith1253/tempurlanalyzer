package com.sentinelai.guard.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DomainAnalysis extends BaseFirestoreModel {
    @DocumentId
    private String id;
    private String host;
    private Double riskScore;
    private String decision; // BLOCK, WARN, ALLOW
    private List<String> evidence;
    private String verdict; // Additional details about the decision
    private String analyzedBy; // Service/analyzer that performed the analysis
    private String source; // Source of the analysis (e.g., "API", "MANUAL")
    private boolean isActive;
    private Date lastAnalyzedAt;
    private int analysisCount;
    private List<String> tags; // For categorization
    private String notes; // Any additional notes

    public static DomainAnalysis fromMap(Map<String, Object> map) {
        return DomainAnalysis.builder()
                .id((String) map.get("id"))
                .host((String) map.get("host"))
                .riskScore((Double) map.get("riskScore"))
                .decision((String) map.get("decision"))
                .evidence((List<String>) map.get("evidence"))
                .verdict((String) map.get("verdict"))
                .analyzedBy((String) map.get("analyzedBy"))
                .source((String) map.get("source"))
                .isActive((Boolean) map.getOrDefault("isActive", true))
                .lastAnalyzedAt(map.get("lastAnalyzedAt") != null ? ((Timestamp) map.get("lastAnalyzed")).toDate() : null)
                .analysisCount(((Long) map.getOrDefault("analysisCount", 0L)).intValue())
                .tags((List<String>) map.get("tags"))
                .notes((String) map.get("notes"))
                .createdAt(map.get("createdAt") != null ? ((Timestamp) map.get("createdAt")).toDate() : new Date())
                .updatedAt(map.get("updatedAt") != null ? ((Timestamp) map.get("updatedAt")).toDate() : new Date())
                .build();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", host);
        map.put("riskScore", riskScore);
        map.put("decision", decision);
        map.put("evidence", evidence);
        map.put("verdict", verdict);
        map.put("analyzedBy", analyzedBy);
        map.put("source", source);
        map.put("isActive", isActive);
        map.put("lastAnalyzedAt", lastAnalyzedAt != null ? Timestamp.of(lastAnalyzedAt) : null);
        map.put("analysisCount", analysisCount);
        map.put("tags", tags);
        map.put("notes", notes);
        map.put("createdAt", getCreatedAtTimestamp());
        map.put("updatedAt", getUpdatedAtTimestamp());
        return map;
    }
}

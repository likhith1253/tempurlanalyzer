package com.sentinelai.guard.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Report extends BaseFirestoreModel {
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }
    private String reportId;
    private String type;
    private String startDate;
    private String endDate;
    private String generatedAt;
    private String generatedBy;
    private String filePath;
    private String downloadUrl;
    private Map<String, Object> summary;
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("reportId", reportId);
        map.put("type", type);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("generatedAt", generatedAt);
        map.put("generatedBy", generatedBy);
        map.put("filePath", filePath);
        map.put("downloadUrl", downloadUrl);
        map.put("summary", summary);
        map.put("createdAt", getCreatedAtTimestamp());
        map.put("updatedAt", getUpdatedAtTimestamp());
        return map;
    }
}

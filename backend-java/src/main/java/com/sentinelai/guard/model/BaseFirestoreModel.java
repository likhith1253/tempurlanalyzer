package com.sentinelai.guard.model;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
public abstract class BaseFirestoreModel {
    private String id;
    private Date createdAt;
    private Date updatedAt;

    public BaseFirestoreModel() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setUpdatedNow() {
        this.updatedAt = new Date();
    }

    public Timestamp getCreatedAtTimestamp() {
        return Timestamp.of(createdAt);
    }

    public Timestamp getUpdatedAtTimestamp() {
        return Timestamp.of(updatedAt);
    }
}

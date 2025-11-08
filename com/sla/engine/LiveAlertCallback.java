package com.sla.engine;

import java.time.LocalDateTime;

@FunctionalInterface
public interface LiveAlertCallback {
    void onThreatThresholdCrossed(String ipAddress, double score, LocalDateTime timestamp);
}

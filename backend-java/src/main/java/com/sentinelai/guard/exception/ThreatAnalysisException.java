package com.sentinelai.guard.exception;

public class ThreatAnalysisException extends RuntimeException {

    public ThreatAnalysisException(String message) {
        super(message);
    }

    public ThreatAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
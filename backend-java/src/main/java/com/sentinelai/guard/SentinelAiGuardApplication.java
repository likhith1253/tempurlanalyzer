package com.sentinelai.guard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the SentinelAI Guard application.
 * <p>
 * This Spring Boot application provides AI-powered cybersecurity threat analysis
 * for logs and URLs in real-time.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SentinelAiGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelAiGuardApplication.class, args);
    }
}

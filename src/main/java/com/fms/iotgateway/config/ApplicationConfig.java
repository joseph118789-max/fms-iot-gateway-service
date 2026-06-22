package com.fms.iotgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration properties loaded from application.yml.
 * Traccar connection and polling settings.
 */
@Configuration
@ConfigurationProperties(prefix = "traccar")
public class ApplicationConfig {

    private String baseUrl = "http://localhost:8082";
    private String apiKey;
    private long pollIntervalMs = 60000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }
}

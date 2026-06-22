package com.fms.iotgateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validates required environment variables at startup.
 * Fails fast with a clear error if any required var is missing.
 *
 * Phase 2b conventions: env-driven secrets, no hardcoded fallbacks.
 * EnvVarValidator pattern mirrors phase2a conventions.
 *
 * Set skip.env.validation=true to disable (e.g., for unit tests).
 */
@Component
public class EnvVarValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvVarValidator.class);

    @Autowired
    private Environment env;

    @PostConstruct
    public void validate() {
        if (Boolean.parseBoolean(env.getProperty("skip.env.validation", "false"))) {
            log.info("EnvVarValidator skipped (skip.env.validation=true)");
            return;
        }

        log.info("Validating required environment variables...");

        // DB credentials — required in all environments
        require("DB_URL", "JDBC URL for telemetry database");
        require("DB_USER", "Database username for telemetry_writer role");
        require("DB_PASSWORD", "Database password (from K8s Secret iot-gateway-creds)");

        log.info("All required environment variables present.");
    }

    private void require(String key, String description) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "REQUIRED env var not set: " + key + " — " + description
            );
        }
        // Log presence (not the value)
        log.debug("  {}: [SET]", key);
    }
}

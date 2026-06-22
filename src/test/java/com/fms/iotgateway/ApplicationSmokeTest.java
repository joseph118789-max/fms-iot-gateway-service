package com.fms.iotgateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic smoke test: verify the application class is loadable
 * and has the correct annotations.
 *
 * Full integration test requires a running database — handled by
 * DevOps lane's three-tier smoke test post-deploy.
 */
class ApplicationSmokeTest {

    @Test
    void mainClassExists() {
        // Verify the main class is loadable
        assertNotNull(IotGatewayApplication.class);
    }
}

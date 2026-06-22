package com.fms.iotgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic smoke test: verify the Spring context loads without crashing.
 * Does NOT require a real DB — uses TestPropertySource to skip DataSource init.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class ApplicationSmokeTest {

    @Test
    void contextLoads() {
        // If we get here, the Spring context loaded without crashing
    }
}

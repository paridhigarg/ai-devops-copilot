package com.paridhi.devopscopilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "github.token=test-token"
})
class DevOpsCopilotApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without errors
    }
}

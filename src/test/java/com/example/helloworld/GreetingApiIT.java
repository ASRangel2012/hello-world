package com.example.helloworld;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-stack integration test (run by the Gradle {@code integrationTest} task):
 * real Spring context, real PostgreSQL via Testcontainers, Flyway
 * migration applied on startup. Requires a Docker daemon.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class GreetingApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloEndToEndReadsTemplateFromDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/hello")
                        .param("name", "Grace").param("locale", "es"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.message").value("¡Hola, Grace!"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void localeLookupIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/hello")
                        .param("name", "Grace").param("locale", "ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("¡Hola, Grace!"));
    }

    @Test
    void unknownLocaleYieldsProblemDetail404() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/hello").param("locale", "xx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void readinessProbeIncludesDatabaseAndIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

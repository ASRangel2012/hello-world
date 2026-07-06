package com.example.helloworld.api.v1;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.helloworld.api.dto.GreetingResponse;
import com.example.helloworld.config.SecurityConfig;
import com.example.helloworld.exception.GreetingNotFoundException;
import com.example.helloworld.service.GreetingService;
import com.example.helloworld.web.CorrelationIdFilter;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GreetingController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class})
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GreetingService greetingService;

    @Test
    void helloIsPublicAndReturnsGreetingPayload() throws Exception {
        when(greetingService.greet(eq("Ada"), eq("en"))).thenReturn(
                new GreetingResponse("Hello, Ada!", "Hope is not a strategy.",
                        Instant.parse("2026-07-06T12:00:00Z"), "1.0.0"));

        mockMvc.perform(get("/api/v1/greetings/hello").param("name", "Ada"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER))
                .andExpect(jsonPath("$.message").value("Hello, Ada!"))
                .andExpect(jsonPath("$.timestamp").value("2026-07-06T12:00:00Z"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void helloReturnsProblemDetailForUnknownLocale() throws Exception {
        when(greetingService.greet(eq("World"), eq("xx")))
                .thenThrow(new GreetingNotFoundException("xx"));

        mockMvc.perform(get("/api/v1/greetings/hello").param("locale", "xx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void helloRejectsInvalidName() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/hello").param("name", "<script>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/greetings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void listIsAccessibleWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/greetings"))
                .andExpect(status().isOk());
    }
}

package com.example.helloworld.api.v1;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.helloworld.config.ApplicationConfig;
import com.example.helloworld.config.SecurityConfig;
import com.example.helloworld.exception.GreetingNotFoundException;
import com.example.helloworld.service.GreetingResult;
import com.example.helloworld.service.GreetingService;
import com.example.helloworld.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GreetingController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, ApplicationConfig.class})
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GreetingService greetingService;

    @Test
    void helloIsPublicAndReturnsGreetingPayload() throws Exception {
        when(greetingService.greet("Ada", "en"))
                .thenReturn(new GreetingResult("Hello, Ada!", "Hope is not a strategy."));

        mockMvc.perform(get("/api/v1/greetings/hello").param("name", "Ada"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.message").value("Hello, Ada!"))
                .andExpect(jsonPath("$.quoteOfTheDay").value("Hope is not a strategy."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void helloReturnsProblemDetailForUnknownLocale() throws Exception {
        when(greetingService.greet("World", "xx"))
                .thenThrow(new GreetingNotFoundException("xx"));

        mockMvc.perform(get("/api/v1/greetings/hello").param("locale", "xx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void helloRejectsInvalidName() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/hello").param("name", "<script>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void helloRejectsInvalidLocale() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/hello").param("locale", "12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    void unknownPathYieldsProblemDetail404NotA500() throws Exception {
        mockMvc.perform(get("/api/v1/greetings/nope/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
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

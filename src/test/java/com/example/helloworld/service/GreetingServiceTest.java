package com.example.helloworld.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.helloworld.api.dto.GreetingResponse;
import com.example.helloworld.client.QuoteClient;
import com.example.helloworld.client.QuoteUnavailableException;
import com.example.helloworld.domain.Greeting;
import com.example.helloworld.exception.GreetingNotFoundException;
import com.example.helloworld.repository.GreetingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-06T12:00:00Z");

    @Mock
    private GreetingRepository greetingRepository;

    @Mock
    private QuoteClient quoteClient;

    private GreetingService greetingService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        greetingService = new GreetingService(greetingRepository, quoteClient, fixedClock, "1.0.0-test");
    }

    @Test
    void greetReturnsLocalizedMessageWithTimestampAndVersion() {
        when(greetingRepository.findByLocale("en")).thenReturn(Optional.of(new Greeting("en", "Hello, %s!")));
        when(quoteClient.quoteOfTheDay()).thenReturn("Hope is not a strategy.");

        GreetingResponse response = greetingService.greet("Ada", "en");

        assertThat(response.message()).isEqualTo("Hello, Ada!");
        assertThat(response.quoteOfTheDay()).isEqualTo("Hope is not a strategy.");
        assertThat(response.timestamp()).isEqualTo(FIXED_NOW);
        assertThat(response.version()).isEqualTo("1.0.0-test");
    }

    @Test
    void greetFallsBackWhenQuoteProviderIsUnavailable() {
        when(greetingRepository.findByLocale("en")).thenReturn(Optional.of(new Greeting("en", "Hello, %s!")));
        when(quoteClient.quoteOfTheDay()).thenThrow(new QuoteUnavailableException("down"));

        GreetingResponse response = greetingService.greet("Ada", "en");

        assertThat(response.message()).isEqualTo("Hello, Ada!");
        assertThat(response.quoteOfTheDay()).isNotBlank();
    }

    @Test
    void greetThrowsForUnknownLocale() {
        when(greetingRepository.findByLocale("xx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> greetingService.greet("Ada", "xx"))
                .isInstanceOf(GreetingNotFoundException.class)
                .hasMessageContaining("xx");
    }
}

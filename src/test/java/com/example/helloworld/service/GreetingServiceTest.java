package com.example.helloworld.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.helloworld.client.QuoteClient;
import com.example.helloworld.client.QuoteUnavailableException;
import com.example.helloworld.domain.Greeting;
import com.example.helloworld.exception.GreetingNotFoundException;
import com.example.helloworld.repository.GreetingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

    @Mock
    private GreetingRepository greetingRepository;

    @Mock
    private QuoteClient quoteClient;

    private GreetingService greetingService;

    @BeforeEach
    void setUp() {
        greetingService = new GreetingService(greetingRepository, quoteClient);
    }

    @Test
    void greetReturnsLocalizedMessageAndQuote() {
        when(greetingRepository.findByLocale("en")).thenReturn(Optional.of(new Greeting("en", "Hello, %s!")));
        when(quoteClient.quoteOfTheDay()).thenReturn("Hope is not a strategy.");

        GreetingResult result = greetingService.greet("Ada", "en");

        assertThat(result.message()).isEqualTo("Hello, Ada!");
        assertThat(result.quote()).isEqualTo("Hope is not a strategy.");
    }

    @Test
    void greetNormalizesLocaleCaseAndWhitespace() {
        when(greetingRepository.findByLocale("es")).thenReturn(Optional.of(new Greeting("es", "¡Hola, %s!")));
        when(quoteClient.quoteOfTheDay()).thenReturn("q");

        GreetingResult result = greetingService.greet("Ada", "  ES ");

        assertThat(result.message()).isEqualTo("¡Hola, Ada!");
    }

    @Test
    void greetFallsBackWhenQuoteProviderIsUnavailable() {
        when(greetingRepository.findByLocale("en")).thenReturn(Optional.of(new Greeting("en", "Hello, %s!")));
        when(quoteClient.quoteOfTheDay()).thenThrow(new QuoteUnavailableException("down"));

        GreetingResult result = greetingService.greet("Ada", "en");

        assertThat(result.message()).isEqualTo("Hello, Ada!");
        assertThat(result.quote()).isNotBlank();
    }

    @Test
    void greetThrowsForUnknownLocale() {
        when(greetingRepository.findByLocale("xx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> greetingService.greet("Ada", "xx"))
                .isInstanceOf(GreetingNotFoundException.class)
                .hasMessageContaining("xx");
    }
}

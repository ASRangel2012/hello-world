package com.example.helloworld.service;

import com.example.helloworld.client.QuoteClient;
import com.example.helloworld.client.QuoteUnavailableException;
import com.example.helloworld.domain.Greeting;
import com.example.helloworld.exception.GreetingNotFoundException;
import com.example.helloworld.repository.GreetingRepository;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class);
    private static final String FALLBACK_QUOTE = "Simplicity is the soul of efficiency.";

    private final GreetingRepository greetingRepository;
    private final QuoteClient quoteClient;

    public GreetingService(GreetingRepository greetingRepository, QuoteClient quoteClient) {
        this.greetingRepository = greetingRepository;
        this.quoteClient = quoteClient;
    }

    @Timed(value = "greeting.resolve", description = "Time to resolve a localized greeting")
    public GreetingResult greet(String name, String locale) {
        String normalizedLocale = locale.trim().toLowerCase(Locale.ROOT);
        Greeting greeting = greetingRepository.findByLocale(normalizedLocale)
                .orElseThrow(() -> new GreetingNotFoundException(normalizedLocale));
        log.debug("Resolved greeting for locale={}", normalizedLocale);
        return new GreetingResult(greeting.getTemplate().formatted(name), resolveQuote());
    }

    @Timed(value = "greeting.list", description = "Time to list greeting templates")
    public List<Greeting> findAll() {
        return greetingRepository.findAll();
    }

    /**
     * The quote is decorative: degrade gracefully instead of failing the request
     * when the (retried) upstream call is still unavailable.
     */
    private String resolveQuote() {
        try {
            return quoteClient.quoteOfTheDay();
        } catch (QuoteUnavailableException e) {
            log.warn("Quote provider unavailable after retries, using fallback: {}", e.getMessage());
            return FALLBACK_QUOTE;
        }
    }
}

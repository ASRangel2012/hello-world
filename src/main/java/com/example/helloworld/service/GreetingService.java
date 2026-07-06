package com.example.helloworld.service;

import com.example.helloworld.api.dto.GreetingResponse;
import com.example.helloworld.client.QuoteClient;
import com.example.helloworld.client.QuoteUnavailableException;
import com.example.helloworld.domain.Greeting;
import com.example.helloworld.exception.GreetingNotFoundException;
import com.example.helloworld.repository.GreetingRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class);
    private static final String FALLBACK_QUOTE = "Simplicity is the soul of efficiency.";

    private final GreetingRepository greetingRepository;
    private final QuoteClient quoteClient;
    private final Clock clock;
    private final String serviceVersion;

    public GreetingService(GreetingRepository greetingRepository,
                           QuoteClient quoteClient,
                           Clock clock,
                           @Value("${info.app.version:unknown}") String serviceVersion) {
        this.greetingRepository = greetingRepository;
        this.quoteClient = quoteClient;
        this.clock = clock;
        this.serviceVersion = serviceVersion;
    }

    public GreetingResponse greet(String name, String locale) {
        Greeting greeting = greetingRepository.findByLocale(locale)
                .orElseThrow(() -> new GreetingNotFoundException(locale));
        String message = greeting.getTemplate().formatted(name);
        log.debug("Resolved greeting for locale={}", locale);
        return new GreetingResponse(message, resolveQuote(), clock.instant(), serviceVersion);
    }

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

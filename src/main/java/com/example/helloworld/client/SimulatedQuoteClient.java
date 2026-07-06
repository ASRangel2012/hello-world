package com.example.helloworld.client;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Default implementation that simulates a flaky upstream dependency so the
 * retry pattern (Spring Framework 7 core resilience) is exercised locally.
 * Every 5th invocation fails once with a transient error; the retry recovers it.
 */
@Component
@ConditionalOnProperty(name = "hello.quote.mode", havingValue = "simulated", matchIfMissing = true)
public class SimulatedQuoteClient implements QuoteClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatedQuoteClient.class);

    private static final List<String> QUOTES = List.of(
            "Simplicity is prerequisite for reliability.",
            "Make it work, make it right, make it fast.",
            "You build it, you run it.",
            "Hope is not a strategy.");

    private final AtomicLong invocations = new AtomicLong();

    @Override
    @Retryable(includes = QuoteUnavailableException.class,
               maxRetries = 2, delay = 200, multiplier = 2.0, maxDelay = 1000, jitter = 50)
    public String quoteOfTheDay() {
        if (invocations.incrementAndGet() % 5 == 0) {
            log.debug("Simulating transient upstream failure");
            throw new QuoteUnavailableException("simulated transient failure");
        }
        return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
    }
}

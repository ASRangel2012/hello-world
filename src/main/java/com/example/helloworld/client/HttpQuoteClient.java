package com.example.helloworld.client;

import com.example.helloworld.config.HelloProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real HTTP implementation, enabled with {@code hello.quote.mode=http}.
 * Connect/read timeouts come from {@code spring.http.client.*} applied to the
 * auto-configured {@link RestClient.Builder}; retries with exponential
 * back-off are declarative via {@link Retryable}.
 */
@Component
@ConditionalOnProperty(name = "hello.quote.mode", havingValue = "http")
public class HttpQuoteClient implements QuoteClient {

    private final RestClient restClient;

    public HttpQuoteClient(RestClient.Builder builder, HelloProperties properties) {
        this.restClient = builder.baseUrl(properties.quote().baseUrl()).build();
    }

    @Override
    @Retryable(includes = ResourceAccessException.class,
               maxRetries = 2, delay = 200, multiplier = 2.0, maxDelay = 1000, jitter = 50)
    public String quoteOfTheDay() {
        try {
            return restClient.get()
                    .uri("/api/v1/quotes/today")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new QuoteUnavailableException("quote provider call failed", e);
        }
    }
}

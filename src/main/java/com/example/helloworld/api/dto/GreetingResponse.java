package com.example.helloworld.api.dto;

import java.time.Instant;

public record GreetingResponse(
        String message,
        String quoteOfTheDay,
        Instant timestamp,
        String version) {
}

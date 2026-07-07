package com.example.helloworld.service;

/**
 * Service-layer result. Deliberately not the API DTO: the service layer must
 * not depend on the web layer's wire format.
 */
public record GreetingResult(String message, String quote) {
}

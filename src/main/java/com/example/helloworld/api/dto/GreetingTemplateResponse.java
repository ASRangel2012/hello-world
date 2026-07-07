package com.example.helloworld.api.dto;

/** Wire representation of a greeting template — JPA entities never leave the service layer. */
public record GreetingTemplateResponse(String locale, String template) {
}

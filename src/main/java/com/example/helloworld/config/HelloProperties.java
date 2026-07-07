package com.example.helloworld.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Fail fast at startup on invalid configuration instead of at first request. */
@Validated
@ConfigurationProperties(prefix = "hello")
public record HelloProperties(@NotNull @Valid Quote quote) {

    public record Quote(
            @NotBlank @Pattern(regexp = "simulated|http", message = "mode must be 'simulated' or 'http'")
            String mode,
            @NotBlank
            String baseUrl) {
    }
}

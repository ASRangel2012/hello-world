package com.example.helloworld.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hello")
public record HelloProperties(Quote quote) {

    public record Quote(String mode, String baseUrl) {
    }
}

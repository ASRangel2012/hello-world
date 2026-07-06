package com.example.helloworld.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {

    /** Injectable clock keeps time-dependent logic deterministic in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

package com.example.helloworld.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Caffeine-backed caching; cache names, size bound and TTL come from spring.cache.*. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {
}

package com.example.helloworld.repository;

import com.example.helloworld.domain.Greeting;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreetingRepository extends JpaRepository<Greeting, Long> {

    String CACHE_GREETINGS_BY_LOCALE = "greetings-by-locale";

    /**
     * Templates are static reference data (changed only by Flyway migrations),
     * so lookups are cached (Caffeine, bounded + TTL — see spring.cache.*).
     * Cached entities are detached; they are only ever read.
     */
    @Cacheable(cacheNames = CACHE_GREETINGS_BY_LOCALE)
    Optional<Greeting> findByLocale(String locale);
}

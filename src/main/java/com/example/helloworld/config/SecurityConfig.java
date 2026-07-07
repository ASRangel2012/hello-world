package com.example.helloworld.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Stateless HTTP Basic security. The single public read endpoint, health
 * probes, Prometheus scrape endpoint and API docs are open; everything else
 * requires authentication. Credentials come from {@code spring.security.user.*}
 * (environment-provided in production). Swap {@code httpBasic} for
 * {@code oauth2ResourceServer} when an IdP is available — the chain structure
 * stays the same.
 *
 * <p>In production the actuator endpoints live on the dedicated management
 * port (8081), which the Ingress never routes to — the permitAll rules below
 * therefore expose them only inside the cluster network.
 *
 * <p>Response hardening: X-Content-Type-Options, X-Frame-Options and
 * Cache-Control are Spring Security defaults; HSTS, CSP, Referrer-Policy and
 * Permissions-Policy are configured explicitly. HSTS is emitted only for
 * requests recognised as secure, which behind the TLS-terminating ingress
 * requires {@code server.forward-headers-strategy=framework} (set in prod).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless token/basic API: CSRF protection does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; img-src 'self' data:; "
                                        + "style-src 'self' 'unsafe-inline'; frame-ancestors 'none'"))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/greetings/hello").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(withDefaults());
        return http.build();
    }
}

package com.example.helloworld.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI helloWorldOpenApi(@Value("${info.app.version:unknown}") String version) {
        return new OpenAPI().info(new Info()
                .title("Hello World Service API")
                .description("Production-grade hello-world microservice")
                .version(version)
                .contact(new Contact().name("Platform Team").email("platform@example.com"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

import org.apache.tools.ant.filters.ReplaceTokens
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.5"
    id("org.sonarqube") version "7.3.1.8318"
}

group = "com.example"
version = "1.0.0"
description = "Production-grade hello-world microservice"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // All Spring/managed versions come from the Boot BOM — single source of truth.
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    // Web (Spring Boot 4 modular starter, replaces spring-boot-starter-web)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Boot 4 modularization: RestClient auto-config (RestClient.Builder bean +
    // spring.http.client.* timeout properties) is no longer part of the web starter.
    implementation("org.springframework.boot:spring-boot-starter-restclient")

    // Data access
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Boot 4 modularization: flyway-core alone no longer triggers auto-config —
    // the spring-boot-flyway module (via its starter) is required.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Caching (static reference data)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // AspectJ weaving for Micrometer's @Timed
    // (Boot 4 removed spring-boot-starter-aop; this is its replacement)
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // API documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Test — Testcontainers 2.x is no longer managed by the Boot BOM,
    // so its own BOM is imported (2.0.4 = the version Boot 4.0.5 is built against).
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}

springBoot {
    buildInfo()   // exposes build metadata via /actuator/info
}

// Only the executable boot JAR; the -plain.jar is noise for a service.
tasks.jar {
    enabled = false
}

// Keeps the @project.name@/@project.version@ placeholders in application.yml working.
tasks.processResources {
    filesMatching("application.yml") {
        filter<ReplaceTokens>(
            "tokens" to mapOf(
                "project.name" to project.name,
                "project.version" to project.version.toString()
            )
        )
    }
}

// Unit tests: fast, no Docker required.
tasks.test {
    useJUnitPlatform()
    exclude("**/*IT.class")
    finalizedBy(tasks.jacocoTestReport)
}

// Integration tests (*IT): Testcontainers, requires a Docker daemon.
val integrationTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs *IT integration tests with Testcontainers (requires Docker)."
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/*IT.class")
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTest)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true   // consumed by Sonar
    }
}

sonar {
    properties {
        property("sonar.projectKey", "hello-world-service")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

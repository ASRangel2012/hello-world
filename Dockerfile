# syntax=docker/dockerfile:1

########## Stage 1: build ##########
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Cache dependencies separately from source changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

########## Stage 2: extract layered JAR ##########
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /extracted
COPY --from=build /workspace/target/hello-world-service-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination .

########## Stage 3: runtime (minimal Alpine JRE, non-root) ##########
FROM eclipse-temurin:25-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/example/hello-world-service"

RUN addgroup -S -g 1001 app \
    && adduser -S -u 1001 -G app -s /sbin/nologin app
USER 1001:1001
WORKDIR /app

# Layer order: least → most frequently changing, for maximal cache reuse
COPY --from=extract --chown=1001:1001 /extracted/dependencies/ ./
COPY --from=extract --chown=1001:1001 /extracted/spring-boot-loader/ ./
COPY --from=extract --chown=1001:1001 /extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=1001:1001 /extracted/application/ ./

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
# 8080 = application, 8081 = management/actuator (prod profile)
EXPOSE 8080 8081

# No Dockerfile HEALTHCHECK: Kubernetes owns liveness/readiness via actuator
# probes, and docker-compose defines its own healthcheck. A `java -version`
# style check only proves the JVM binary works and burns CPU every interval.

ENTRYPOINT ["java", "-jar", "application.jar"]

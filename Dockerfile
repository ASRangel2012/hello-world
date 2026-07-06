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
FROM eclipse-temurin:25-jre AS extract
WORKDIR /extracted
COPY --from=build /workspace/target/hello-world-service-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination .

########## Stage 3: runtime ##########
FROM eclipse-temurin:25-jre
LABEL org.opencontainers.image.source="https://github.com/example/hello-world-service"

RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --shell /usr/sbin/nologin app
USER 1001:1001
WORKDIR /app

# Layer order: least → most frequently changing, for maximal cache reuse
COPY --from=extract --chown=1001:1001 /extracted/dependencies/ ./
COPY --from=extract --chown=1001:1001 /extracted/spring-boot-loader/ ./
COPY --from=extract --chown=1001:1001 /extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=1001:1001 /extracted/application/ ./

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD ["java", "-version"]
# Kubernetes uses the actuator probes; the HEALTHCHECK above is a minimal
# liveness signal for plain `docker run` (curl is not installed in the JRE image).

ENTRYPOINT ["java", "-jar", "application.jar"]

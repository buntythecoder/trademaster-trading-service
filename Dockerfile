# TradeMaster Trading Service - Multi-stage Docker Build
# Optimized for Java 24 Virtual Threads with production security hardening
FROM openjdk:24-jdk-slim as builder

# Security: Create non-root user for build
RUN groupadd -r trademaster && useradd -r -g trademaster trademaster

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

# Download dependencies (for better layer caching)
COPY gradle.properties ./
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build application with Java 24 preview features
RUN ./gradlew clean build -x test --no-daemon \
    && mkdir -p target \
    && cp build/libs/*.jar target/trading-service.jar

# Production Image - Distroless for minimal attack surface
FROM gcr.io/distroless/java21-debian12:nonroot

# Labels for container metadata
LABEL maintainer="TradeMaster Development Team"
LABEL version="2.0.0"
LABEL description="TradeMaster Trading Service with Java 24 Virtual Threads"
LABEL org.opencontainers.image.source="https://github.com/trademaster/trading-service"

# Environment variables
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+EnableJVMCI \
               -Xmx2g \
               -Xms1g \
               -Dspring.threads.virtual.enabled=true \
               --enable-preview \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker"

ENV SERVER_PORT=8083
ENV MANAGEMENT_SERVER_PORT=9083

# Create application directories
USER 65532:65532

# Copy JAR from builder stage
COPY --from=builder --chown=65532:65532 /app/target/trading-service.jar /app/trading-service.jar

# Create logs directory
USER root
RUN mkdir -p /opt/trademaster/logs && \
    chown -R 65532:65532 /opt/trademaster/logs
USER 65532:65532

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Expose ports
EXPOSE ${SERVER_PORT} ${MANAGEMENT_SERVER_PORT}

# JVM optimization for containers
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+EnableJVMCI", \
    "-Xmx2g", \
    "-Xms1g", \
    "-Dspring.threads.virtual.enabled=true", \
    "--enable-preview", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "/app/trading-service.jar"]
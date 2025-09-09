# TradeMaster Trading Service - Multi-stage Docker Build
# Optimized for Java 24 Virtual Threads with production security hardening
# Using Amazon Corretto 24 (recommended for 2025) instead of deprecated openjdk images
FROM amazoncorretto:24-alpine-jdk as builder

# Security: Create non-root user for build (Alpine Linux commands)
RUN addgroup -g 1001 -S trademaster && \
    adduser -S trademaster -u 1001 -G trademaster

# Install build dependencies (Alpine Linux)
RUN apk update && apk add --no-cache \
    curl \
    wget \
    unzip \
    && rm -rf /var/cache/apk/*

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
# IMPORTANT: Ensure Gradle wrapper uses version 8.14+ or 9.0+ for Java 24 support
RUN ./gradlew --version && \
    GRADLE_OPTS="--enable-preview -Xmx2g" ./gradlew clean build -x test --no-daemon \
    --stacktrace --info \
    && ls -la build/libs/ \
    && mkdir -p target \
    && find build/libs/ -name "*.jar" ! -name "*plain*" -exec cp {} target/trading-service.jar \;

# Production Image - Amazon Corretto 24 for secure, up-to-date Java 24 runtime
FROM amazoncorretto:24-alpine-jdk

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

# Install curl for health checks and create non-root user (Alpine Linux)
RUN apk add --no-cache curl && \
    addgroup -g 1000 -S trademaster && \
    adduser -u 1000 -S trademaster -G trademaster

# Create application directories and set permissions
RUN mkdir -p /app /app/logs /opt/trademaster/logs && \
    chown -R trademaster:trademaster /app /opt/trademaster/logs

# Copy JAR from builder stage
COPY --from=builder --chown=trademaster:trademaster /app/target/trading-service.jar /app/trading-service.jar

# Switch to non-root user
USER trademaster:trademaster

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
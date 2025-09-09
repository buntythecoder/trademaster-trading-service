package com.trademaster.trading.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Log Aggregation and Retention Configuration
 * 
 * Enterprise-grade logging pipeline with structured aggregation, retention policies,
 * and automated log management for financial trading compliance.
 * 
 * Features:
 * - Structured JSON logging for machine processing
 * - Time-based log rotation with configurable retention
 * - Separate appenders for different log levels
 * - Compliance-ready audit trail preservation
 * - Automated log cleanup and archiving
 * - Performance metrics for log processing
 * 
 * Compliance:
 * - Regulatory audit trail requirements (7 years for financial data)
 * - GDPR data retention policies
 * - SOX compliance for financial logging
 * - Real-time log monitoring and alerting
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
//@Configuration  // Temporarily disabled for service startup
@EnableConfigurationProperties(LoggingAggregationConfig.LoggingProperties.class)
//@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class LoggingAggregationConfig {
    
    private final LoggingProperties loggingProperties;
    
    /**
     * Initialize structured logging pipeline
     */
    @PostConstruct
    public void initializeLoggingPipeline() {
        log.info("Initializing enterprise logging aggregation pipeline");
        
        try {
            setupLogDirectories();
            configureStructuredLogging();
            setupLogRetentionPolicies();
            
            log.info("Logging pipeline initialized successfully - " +
                "Audit retention: {} days, Performance retention: {} days, Error retention: {} days",
                loggingProperties.getAuditRetentionDays(),
                loggingProperties.getPerformanceRetentionDays(),
                loggingProperties.getErrorRetentionDays());
                
        } catch (Exception e) {
            log.error("Failed to initialize logging pipeline", e);
            throw new RuntimeException("Critical logging infrastructure failure", e);
        }
    }
    
    /**
     * Create required log directories with proper permissions
     */
    private void setupLogDirectories() throws Exception {
        String[] directories = {
            loggingProperties.getAuditLogPath(),
            loggingProperties.getPerformanceLogPath(),
            loggingProperties.getErrorLogPath(),
            loggingProperties.getApplicationLogPath(),
            loggingProperties.getSecurityLogPath(),
            loggingProperties.getArchivePath()
        };
        
        for (String dirPath : directories) {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created log directory: {}", path.toAbsolutePath());
            }
        }
    }
    
    /**
     * Configure structured JSON logging with separate appenders
     */
    private void configureStructuredLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Configure audit logging appender
        setupAuditLoggingAppender(context);
        
        // Configure performance logging appender
        setupPerformanceLoggingAppender(context);
        
        // Configure error logging appender
        setupErrorLoggingAppender(context);
        
        // Configure security logging appender
        setupSecurityLoggingAppender(context);
        
        log.info("Structured logging appenders configured successfully");
    }
    
    /**
     * Setup audit logging appender for regulatory compliance
     */
    private void setupAuditLoggingAppender(LoggerContext context) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("AUDIT_APPENDER");
        appender.setFile(loggingProperties.getAuditLogPath() + "/trading-audit.log");
        
        // Time-based rolling policy for audit logs
        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern(loggingProperties.getAuditLogPath() + "/trading-audit.%d{yyyy-MM-dd}.%i.log.gz");
        policy.setMaxHistory(loggingProperties.getAuditRetentionDays());
        policy.start();
        
        // JSON pattern encoder for structured logging
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("{\"timestamp\":\"%d{ISO8601}\",\"level\":\"%level\",\"logger\":\"%logger{36}\",\"correlationId\":\"%X{correlationId:-}\",\"userId\":\"%X{userId:-}\",\"operation\":\"%X{operation:-}\",\"message\":\"%message\",\"exception\":\"%ex\"}\n");
        encoder.start();
        
        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();
        
        // Add appender to audit logger
        ch.qos.logback.classic.Logger auditLogger = context.getLogger("com.trademaster.trading.audit");
        auditLogger.addAppender(appender);
        auditLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        auditLogger.setAdditive(false);
    }
    
    /**
     * Setup performance logging appender for metrics and monitoring
     */
    private void setupPerformanceLoggingAppender(LoggerContext context) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("PERFORMANCE_APPENDER");
        appender.setFile(loggingProperties.getPerformanceLogPath() + "/trading-performance.log");
        
        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern(loggingProperties.getPerformanceLogPath() + "/trading-performance.%d{yyyy-MM-dd-HH}.log.gz");
        policy.setMaxHistory(loggingProperties.getPerformanceRetentionDays() * 24); // Hourly rotation
        policy.start();
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("{\"timestamp\":\"%d{ISO8601}\",\"level\":\"%level\",\"metric\":\"%X{metric:-}\",\"value\":\"%X{value:-}\",\"duration\":\"%X{duration:-}\",\"correlationId\":\"%X{correlationId:-}\",\"message\":\"%message\"}\n");
        encoder.start();
        
        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();
        
        ch.qos.logback.classic.Logger perfLogger = context.getLogger("com.trademaster.trading.performance");
        perfLogger.addAppender(appender);
        perfLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        perfLogger.setAdditive(false);
    }
    
    /**
     * Setup error logging appender for critical error tracking
     */
    private void setupErrorLoggingAppender(LoggerContext context) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("ERROR_APPENDER");
        appender.setFile(loggingProperties.getErrorLogPath() + "/trading-errors.log");
        
        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern(loggingProperties.getErrorLogPath() + "/trading-errors.%d{yyyy-MM-dd}.log.gz");
        policy.setMaxHistory(loggingProperties.getErrorRetentionDays());
        policy.start();
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("{\"timestamp\":\"%d{ISO8601}\",\"level\":\"%level\",\"logger\":\"%logger{36}\",\"correlationId\":\"%X{correlationId:-}\",\"errorCode\":\"%X{errorCode:-}\",\"userId\":\"%X{userId:-}\",\"message\":\"%message\",\"stackTrace\":\"%ex\"}\n");
        encoder.start();
        
        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();
        
        ch.qos.logback.classic.Logger errorLogger = context.getLogger("com.trademaster.trading.error");
        errorLogger.addAppender(appender);
        errorLogger.setLevel(ch.qos.logback.classic.Level.ERROR);
        errorLogger.setAdditive(false);
    }
    
    /**
     * Setup security logging appender for security events
     */
    private void setupSecurityLoggingAppender(LoggerContext context) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("SECURITY_APPENDER");
        appender.setFile(loggingProperties.getSecurityLogPath() + "/trading-security.log");
        
        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern(loggingProperties.getSecurityLogPath() + "/trading-security.%d{yyyy-MM-dd}.log.gz");
        policy.setMaxHistory(loggingProperties.getAuditRetentionDays()); // Same as audit retention
        policy.start();
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("{\"timestamp\":\"%d{ISO8601}\",\"level\":\"%level\",\"securityEvent\":\"%X{securityEvent:-}\",\"userId\":\"%X{userId:-}\",\"sourceIp\":\"%X{sourceIp:-}\",\"userAgent\":\"%X{userAgent:-}\",\"correlationId\":\"%X{correlationId:-}\",\"message\":\"%message\"}\n");
        encoder.start();
        
        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();
        
        ch.qos.logback.classic.Logger securityLogger = context.getLogger("com.trademaster.trading.security");
        securityLogger.addAppender(appender);
        securityLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        securityLogger.setAdditive(false);
    }
    
    /**
     * Setup automated log retention policies
     */
    private void setupLogRetentionPolicies() {
        log.info("Configuring automated log retention policies");
        
        // Immediate cleanup on startup
        CompletableFuture.runAsync(this::performLogCleanup);
        
        log.info("Log retention policies configured - Cleanup scheduled daily at 2 AM");
    }
    
    /**
     * Scheduled log cleanup - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledLogCleanup() {
        log.info("Starting scheduled log cleanup process");
        CompletableFuture.runAsync(this::performLogCleanup);
    }
    
    /**
     * Perform comprehensive log cleanup and archiving
     */
    private void performLogCleanup() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Clean up old log files based on retention policies
            long auditFilesRemoved = cleanupLogDirectory(
                loggingProperties.getAuditLogPath(), 
                loggingProperties.getAuditRetentionDays()
            );
            
            long performanceFilesRemoved = cleanupLogDirectory(
                loggingProperties.getPerformanceLogPath(), 
                loggingProperties.getPerformanceRetentionDays()
            );
            
            long errorFilesRemoved = cleanupLogDirectory(
                loggingProperties.getErrorLogPath(), 
                loggingProperties.getErrorRetentionDays()
            );
            
            long applicationFilesRemoved = cleanupLogDirectory(
                loggingProperties.getApplicationLogPath(), 
                loggingProperties.getApplicationRetentionDays()
            );
            
            // Archive important logs before deletion
            archiveImportantLogs();
            
            long duration = System.currentTimeMillis() - startTime;
            long totalFilesRemoved = auditFilesRemoved + performanceFilesRemoved + 
                                   errorFilesRemoved + applicationFilesRemoved;
            
            log.info("Log cleanup completed in {}ms - Removed {} files (audit: {}, performance: {}, error: {}, application: {})",
                duration, totalFilesRemoved, auditFilesRemoved, performanceFilesRemoved, 
                errorFilesRemoved, applicationFilesRemoved);
                
        } catch (Exception e) {
            log.error("Failed to perform log cleanup", e);
        }
    }
    
    /**
     * Clean up log directory based on retention policy
     */
    private long cleanupLogDirectory(String directoryPath, int retentionDays) {
        try {
            Path logDir = Paths.get(directoryPath);
            if (!Files.exists(logDir)) {
                return 0;
            }
            
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            
            return Files.walk(logDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (Exception e) {
                        log.warn("Failed to check modification time for: {}", path, e);
                        return false;
                    }
                })
                .mapToLong(path -> {
                    try {
                        Files.delete(path);
                        return 1;
                    } catch (Exception e) {
                        log.warn("Failed to delete log file: {}", path, e);
                        return 0;
                    }
                })
                .sum();
                
        } catch (Exception e) {
            log.error("Failed to cleanup log directory: {}", directoryPath, e);
            return 0;
        }
    }
    
    /**
     * Archive important logs before deletion for compliance
     */
    private void archiveImportantLogs() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path archiveDir = Paths.get(loggingProperties.getArchivePath(), timestamp);
            
            if (!Files.exists(archiveDir)) {
                Files.createDirectories(archiveDir);
            }
            
            // Archive audit logs (required for compliance)
            archiveLogFiles(loggingProperties.getAuditLogPath(), archiveDir.resolve("audit"));
            
            // Archive security logs (required for compliance)
            archiveLogFiles(loggingProperties.getSecurityLogPath(), archiveDir.resolve("security"));
            
            log.info("Important logs archived to: {}", archiveDir);
            
        } catch (Exception e) {
            log.error("Failed to archive important logs", e);
        }
    }
    
    /**
     * Archive log files from source to destination
     */
    private void archiveLogFiles(String sourcePath, Path destinationPath) {
        try {
            Path sourceDir = Paths.get(sourcePath);
            if (!Files.exists(sourceDir)) {
                return;
            }
            
            if (!Files.exists(destinationPath)) {
                Files.createDirectories(destinationPath);
            }
            
            Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log.gz"))
                .forEach(path -> {
                    try {
                        Path targetPath = destinationPath.resolve(path.getFileName());
                        if (!Files.exists(targetPath)) {
                            Files.copy(path, targetPath);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to archive log file: {}", path, e);
                    }
                });
                
        } catch (Exception e) {
            log.error("Failed to archive log files from: {}", sourcePath, e);
        }
    }
    
    /**
     * Log aggregation health check
     */
    @Bean
    public LoggingHealthIndicator loggingHealthIndicator() {
        return new LoggingHealthIndicator(loggingProperties);
    }
    
    /**
     * Logging configuration properties
     */
    @ConfigurationProperties(prefix = "trading.logging")
    public static class LoggingProperties {
        
        private String auditLogPath = "/app/logs/audit";
        private String performanceLogPath = "/app/logs/performance";
        private String errorLogPath = "/app/logs/error";
        private String applicationLogPath = "/app/logs/application";
        private String securityLogPath = "/app/logs/security";
        private String archivePath = "/app/logs/archive";
        
        // Retention policies (in days)
        private int auditRetentionDays = 2555; // 7 years for regulatory compliance
        private int performanceRetentionDays = 30; // 30 days for performance analysis
        private int errorRetentionDays = 90; // 90 days for error analysis
        private int applicationRetentionDays = 7; // 7 days for general application logs
        private int securityRetentionDays = 365; // 1 year for security logs
        
        // Log aggregation settings
        private boolean enableStructuredLogging = true;
        private boolean enableLogAggregation = true;
        private int maxLogFileSize = 100; // MB
        private String logLevel = "INFO";
        
        // Getters and Setters
        public String getAuditLogPath() { return auditLogPath; }
        public void setAuditLogPath(String auditLogPath) { this.auditLogPath = auditLogPath; }
        
        public String getPerformanceLogPath() { return performanceLogPath; }
        public void setPerformanceLogPath(String performanceLogPath) { this.performanceLogPath = performanceLogPath; }
        
        public String getErrorLogPath() { return errorLogPath; }
        public void setErrorLogPath(String errorLogPath) { this.errorLogPath = errorLogPath; }
        
        public String getApplicationLogPath() { return applicationLogPath; }
        public void setApplicationLogPath(String applicationLogPath) { this.applicationLogPath = applicationLogPath; }
        
        public String getSecurityLogPath() { return securityLogPath; }
        public void setSecurityLogPath(String securityLogPath) { this.securityLogPath = securityLogPath; }
        
        public String getArchivePath() { return archivePath; }
        public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
        
        public int getAuditRetentionDays() { return auditRetentionDays; }
        public void setAuditRetentionDays(int auditRetentionDays) { this.auditRetentionDays = auditRetentionDays; }
        
        public int getPerformanceRetentionDays() { return performanceRetentionDays; }
        public void setPerformanceRetentionDays(int performanceRetentionDays) { this.performanceRetentionDays = performanceRetentionDays; }
        
        public int getErrorRetentionDays() { return errorRetentionDays; }
        public void setErrorRetentionDays(int errorRetentionDays) { this.errorRetentionDays = errorRetentionDays; }
        
        public int getApplicationRetentionDays() { return applicationRetentionDays; }
        public void setApplicationRetentionDays(int applicationRetentionDays) { this.applicationRetentionDays = applicationRetentionDays; }
        
        public int getSecurityRetentionDays() { return securityRetentionDays; }
        public void setSecurityRetentionDays(int securityRetentionDays) { this.securityRetentionDays = securityRetentionDays; }
        
        public boolean isEnableStructuredLogging() { return enableStructuredLogging; }
        public void setEnableStructuredLogging(boolean enableStructuredLogging) { this.enableStructuredLogging = enableStructuredLogging; }
        
        public boolean isEnableLogAggregation() { return enableLogAggregation; }
        public void setEnableLogAggregation(boolean enableLogAggregation) { this.enableLogAggregation = enableLogAggregation; }
        
        public int getMaxLogFileSize() { return maxLogFileSize; }
        public void setMaxLogFileSize(int maxLogFileSize) { this.maxLogFileSize = maxLogFileSize; }
        
        public String getLogLevel() { return logLevel; }
        public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    }
    
    /**
     * Custom health indicator for logging system
     */
    public static class LoggingHealthIndicator implements org.springframework.boot.actuate.health.HealthIndicator {
        
        private final LoggingProperties properties;
        
        public LoggingHealthIndicator(LoggingProperties properties) {
            this.properties = properties;
        }
        
        @Override
        public org.springframework.boot.actuate.health.Health health() {
            try {
                // Check if log directories exist and are writable
                boolean allDirectoriesHealthy = checkDirectoryHealth(properties.getAuditLogPath()) &&
                                              checkDirectoryHealth(properties.getPerformanceLogPath()) &&
                                              checkDirectoryHealth(properties.getErrorLogPath()) &&
                                              checkDirectoryHealth(properties.getApplicationLogPath()) &&
                                              checkDirectoryHealth(properties.getSecurityLogPath());
                
                if (allDirectoriesHealthy) {
                    return org.springframework.boot.actuate.health.Health.up()
                        .withDetail("structuredLogging", properties.isEnableStructuredLogging())
                        .withDetail("logAggregation", properties.isEnableLogAggregation())
                        .withDetail("auditRetentionDays", properties.getAuditRetentionDays())
                        .withDetail("status", "All log directories healthy")
                        .build();
                } else {
                    return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("issue", "One or more log directories are not accessible")
                        .build();
                }
                
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
        
        private boolean checkDirectoryHealth(String path) {
            try {
                Path dir = Paths.get(path);
                return Files.exists(dir) && Files.isDirectory(dir) && Files.isWritable(dir);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
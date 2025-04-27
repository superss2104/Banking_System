package Service;

import database.DatabaseConnector;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utils.AuditLogger;
import utils.AuditLogger.AuditEventType;
import utils.SystemMonitoring;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemMonitorService extends SystemMonitoring {
    private static final Logger logger = Logger.getLogger(SystemMonitorService.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    // Singleton instance
    private static SystemMonitorService instance;
    private final AuditLogger auditLogger;
    private final Map<String, Integer> loginAttempts;
    private final Map<String, LocalDateTime> accountLockouts;
    private final ObservableList<Map<String, Object>> systemEvents;
    private final BooleanProperty systemHealthy;
    private final StringProperty systemStatus;
    private final int MAX_LOGIN_ATTEMPTS = 5;
    private final int LOCKOUT_DURATION_MINUTES = 30;
    private ScheduledExecutorService scheduler;

    public SystemMonitorService() throws SQLException {
        this.auditLogger = AuditLogger.getInstance();
        this.loginAttempts = new ConcurrentHashMap<>();
        this.accountLockouts = new ConcurrentHashMap<>();
        this.systemEvents = FXCollections.observableArrayList();
        this.systemHealthy = new SimpleBooleanProperty(true);
        this.systemStatus = new SimpleStringProperty("System operating normally");
        initializeScheduledTasks();
    }

    public static synchronized SystemMonitorService getInstance() throws SQLException {
        if (instance == null) {
            instance = new SystemMonitorService();
        }
        return instance;
    }

    private void initializeScheduledTasks() {
        this.scheduler = new ScheduledThreadPoolExecutor(2);

        // Schedule database health check every 15 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> health = checkDatabaseHealth();
                Boolean connectionActive = (Boolean) health.get("connectionActive");

                Platform.runLater(() -> {
                    systemHealthy.set(connectionActive);
                    systemStatus.set(connectionActive ? "System operating normally" : "Database connection issue detected");
                });

                if (!connectionActive) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("timestamp", LocalDateTime.now());
                    event.put("type", "ERROR");
                    event.put("message", "Database connection failed: " + health.get("error"));
                    Platform.runLater(() -> systemEvents.add(event));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in scheduled health check", e);
            }
        }, 0, 15, TimeUnit.MINUTES);

        // Clean up expired lockouts every minute
        scheduler.scheduleAtFixedRate(this::cleanupExpiredLockouts, 1, 1, TimeUnit.MINUTES);
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        try (Connection conn = DatabaseConnector.getConnection()) {
            status.put("dbConnectionActive", true);

            // Get active user count (using recent logins from audit_logs)
            String userCountSql = "SELECT COUNT(DISTINCT username) FROM audit_logs WHERE event_type = ? AND timestamp > ?";
            try (PreparedStatement stmt = conn.prepareStatement(userCountSql)) {
                stmt.setString(1, AuditEventType.LOGIN.toString());
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        status.put("activeUserCount", rs.getInt(1));
                    }
                }
            }

            // Get transaction count
            String transactionCountSql = "SELECT COUNT(*) FROM transactions WHERE timestamp > ?";
            try (PreparedStatement stmt = conn.prepareStatement(transactionCountSql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        status.put("transactionCount24h", rs.getInt(1));
                    }
                }
            }

            // Get total accounts
            String accountCountSql = "SELECT COUNT(*) FROM accounts";
            try (PreparedStatement stmt = conn.prepareStatement(accountCountSql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        status.put("totalAccounts", rs.getInt(1));
                    }
                }
            }

            // Get active accounts
            String activeAccountCountSql = "SELECT COUNT(*) FROM accounts WHERE status = 'ACTIVE'";
            try (PreparedStatement stmt = conn.prepareStatement(activeAccountCountSql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        status.put("activeAccounts", rs.getInt(1));
                    }
                }
            }

            // Get failed login attempts
            String failedLoginSql = "SELECT COUNT(*) FROM audit_logs WHERE event_type = ? AND timestamp > ?";
            try (PreparedStatement stmt = conn.prepareStatement(failedLoginSql)) {
                stmt.setString(1, AuditEventType.LOGIN_FAILED.toString());
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        status.put("failedLoginAttempts24h", rs.getInt(1));
                    }
                }
            }

            // Runtime metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            status.put("systemUptime", getSystemUptime());
            status.put("cpuUsage", estimateCpuUsage());
            status.put("memoryUsageBytes", usedMemory);
            status.put("memoryUsageMB", usedMemory / (1024 * 1024));
            status.put("totalMemoryMB", totalMemory / (1024 * 1024));
            status.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));

            auditLogger.logEvent(AuditEventType.SYSTEM, "System status checked");
            return status;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving system status", e);
            status.put("dbConnectionActive", false);
            status.put("error", "Database error: " + e.getMessage());
            status.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));
            return status;
        }
    }

    private double estimateCpuUsage() {
        return Math.random() * 30 + 10; // Simulated 10-40%
    }

    private String getSystemUptime() {
        long uptime = (long) (Math.random() * 86400); // Simulated up to 24 hours
        long days = uptime / 86400;
        uptime %= 86400;
        long hours = uptime / 3600;
        uptime %= 3600;
        long minutes = uptime / 60;
        return String.format("%dd %dh %dm", days, hours, minutes);
    }


    public Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();

        try (Connection conn = DatabaseConnector.getConnection()) {
            health.put("connectionActive", conn != null && !conn.isClosed());
            health.put("databaseType", conn.getMetaData().getDatabaseProductName());
            health.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());

            // Measure response time
            long startTime = System.currentTimeMillis();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                stmt.executeQuery();
            }
            long endTime = System.currentTimeMillis();
            health.put("responseTimeMs", endTime - startTime);

            String tableSql = "SELECT COUNT(*) FROM (SELECT 1 FROM users UNION SELECT 1 FROM accounts UNION SELECT 1 FROM transactions UNION SELECT 1 FROM audit_logs LIMIT 1) t";
            try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        health.put("tableCount", rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                health.put("tableCount", "Not available");
            }

            auditLogger.logEvent(AuditEventType.SYSTEM, "Database health check completed successfully");
            return health;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database health check failed", e);
            health.put("connectionActive", false);
            health.put("error", "Database error: " + e.getMessage());
            auditLogger.logEvent(AuditEventType.SYSTEM, "Database health check failed: " + e.getMessage());
            return health;
        }
    }

    public Map<String, Object> getAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String eventType) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            logger.warning("Invalid date range for audit logs");
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Invalid date range");
            result.put("count", 0);
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        StringBuilder sql = new StringBuilder(
                "SELECT log_id, username, event_type, description, timestamp FROM audit_logs WHERE timestamp BETWEEN ? AND ?");

        if (eventType != null && !eventType.trim().isEmpty()) {
            sql.append(" AND event_type = ?");
        }
        sql.append(" ORDER BY timestamp DESC");

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            if (eventType != null && !eventType.trim().isEmpty()) {
                stmt.setString(3, eventType);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> logs = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("id", rs.getLong("log_id"));
                    log.put("timestamp", rs.getTimestamp("timestamp").toLocalDateTime().format(ISO_FORMATTER));
                    log.put("eventType", rs.getString("event_type"));
                    log.put("username", rs.getString("username"));
                    log.put("message", rs.getString("description"));
                    logs.add(log);
                }

                result.put("logs", logs);
                result.put("count", logs.size());
                result.put("startDate", startDate.format(ISO_FORMATTER));
                result.put("endDate", endDate.format(ISO_FORMATTER));
                result.put("eventType", eventType != null && !eventType.trim().isEmpty() ? eventType : "ALL");

                logger.info("Retrieved " + logs.size() + " audit logs");
            }
            return result;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving audit logs", e);
            result.put("error", "Database error: " + e.getMessage());
            result.put("count", 0);
            return result;
        }
    }

    public ObservableList<Map<String, Object>> getSystemEvents() {
        return this.systemEvents;
    }

    public BooleanProperty systemHealthyProperty() {
        return systemHealthy;
    }

    public StringProperty systemStatusProperty() {
        return systemStatus;
    }

    public boolean recordLoginAttempt(String username, boolean successful) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot record login attempt with null or empty username");
            return false;
        }

        if (successful) {
            loginAttempts.remove(username);
            accountLockouts.remove(username);
            auditLogger.logEvent(AuditEventType.LOGIN, "Successful login: " + username);
            return false;
        } else {
            int attempts = loginAttempts.getOrDefault(username, 0) + 1;
            loginAttempts.put(username, attempts);
            auditLogger.logEvent(AuditEventType.LOGIN_FAILED, "Failed login attempt: " + username + " (Attempt " + attempts + ")");

            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                accountLockouts.put(username, LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                auditLogger.logEvent(AuditEventType.ACCOUNT_LOCKED, "Account locked due to multiple failed attempts: " + username);

                Map<String, Object> event = new HashMap<>();
                event.put("timestamp", LocalDateTime.now());
                event.put("type", "SECURITY");
                event.put("message", "Account locked: " + username + " after " + attempts + " failed attempts");
                Platform.runLater(() -> systemEvents.add(event));

                return true;
            }
            return false;
        }
    }

    public boolean isAccountLocked(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        LocalDateTime lockoutExpiry = accountLockouts.get(username);
        if (lockoutExpiry != null) {
            if (LocalDateTime.now().isBefore(lockoutExpiry)) {
                return true;
            } else {
                accountLockouts.remove(username);
                loginAttempts.remove(username);
                auditLogger.logEvent(AuditEventType.ACCOUNT_UNLOCKED, "Account unlocked: " + username);
                return false;
            }
        }
        return false;
    }

    public long getLockoutRemainingMinutes(String username) {
        if (username == null || username.trim().isEmpty()) {
            return 0;
        }

        LocalDateTime lockoutExpiry = accountLockouts.get(username);
        if (lockoutExpiry != null && LocalDateTime.now().isBefore(lockoutExpiry)) {
            return java.time.Duration.between(LocalDateTime.now(), lockoutExpiry).toMinutes();
        }
        return 0;
    }

    private void cleanupExpiredLockouts() {
        LocalDateTime now = LocalDateTime.now();
        accountLockouts.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue())) {
                auditLogger.logEvent(AuditEventType.ACCOUNT_UNLOCKED, "Account unlocked: " + entry.getKey());
                loginAttempts.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public void recordSystemEvent(String eventType, String message) {
        if (eventType == null || message == null) {
            logger.warning("Cannot record system event with null parameters");
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now());
        event.put("type", eventType);
        event.put("message", message);
        Platform.runLater(() -> systemEvents.add(event));

        // Use SYSTEM event type for generic events
        auditLogger.logEvent(AuditEventType.SYSTEM, message);
    }

    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        metrics.put("usedMemoryBytes", usedMemory);
        metrics.put("totalMemoryBytes", totalMemory);
        metrics.put("maxMemoryBytes", maxMemory);
        metrics.put("usedMemoryMB", usedMemory / (1024 * 1024));
        metrics.put("totalMemoryMB", totalMemory / (1024 * 1024));
        metrics.put("maxMemoryMB", maxMemory / (1024 * 1024));
        metrics.put("memoryUtilizationPercent", ((double) usedMemory / totalMemory) * 100);
        metrics.put("activeThreads", Thread.activeCount());
        metrics.put("cpuUsagePercent", estimateCpuUsage());
        metrics.put("dbActiveConnections", 3);
        metrics.put("dbIdleConnections", 7);
        metrics.put("dbMaxConnections", 10);
        metrics.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));

        return metrics;
    }
}
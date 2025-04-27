package utils;

import Service.SystemMonitorService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemMonitoring {
    private static final Logger logger = Logger.getLogger(SystemMonitoring.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final SystemMonitorService monitorService;

    static {
        try {
            monitorService = SystemMonitorService.getInstance();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize SystemMonitorService", e);
            throw new RuntimeException("SystemMonitorService initialization failed", e);
        }
    }

    public static void logSystemStatus(String component, String status) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] %s: %s", timestamp, component, status);
        logger.log(Level.INFO, message);
        monitorService.recordSystemEvent("INFO", message);
    }

    public static void logPerformanceMetric(String metric, double value) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] Performance Metric - %s: %.2f", timestamp, metric, value);
        logger.log(Level.INFO, message);
        monitorService.recordSystemEvent("INFO", message);
    }

    public static void logError(String component, String error) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] ERROR in %s: %s", timestamp, component, error);
        logger.log(Level.SEVERE, message);
        monitorService.recordSystemEvent("ERROR", message);
    }

    public static void logWarning(String component, String warning) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] WARNING in %s: %s", timestamp, component, warning);
        logger.log(Level.WARNING, message);
        monitorService.recordSystemEvent("WARNING", message);
    }
}
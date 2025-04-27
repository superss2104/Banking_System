package controller;

import Service.SystemMonitorService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import utils.AuditLogger;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SystemMonitorController {
    private final SystemMonitorService monitorService;
    private final Timeline updateTimeline;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label activeUsersLabel;
    @FXML
    private Label totalTransactionsLabel;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private Label uptimeLabel;
    @FXML
    private Label cpuUsageLabel;
    @FXML
    private Label memoryUsageLabel;
    @FXML
    private Label diskUsageLabel;
    @FXML
    private Label responseTimeLabel;
    @FXML
    private Label activeConnectionsLabel;
    @FXML
    private Label healthStatusLabel;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private TableView<Map<String, Object>> securityEventsTable;
    @FXML
    private TableColumn<Map<String, Object>, String> timestampColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> eventTypeColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> descriptionColumn;

    public SystemMonitorController() throws SQLException {
        this.monitorService = SystemMonitorService.getInstance();
        // Update every 5 seconds
        this.updateTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> {
                    try {
                        updateDashboard();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
        this.updateTimeline.setCycleCount(Animation.INDEFINITE);
    }

    @FXML
    public void initialize() throws SQLException {
        // Set up table columns
        timestampColumn.setCellValueFactory(cellData -> {
            Object timestamp = cellData.getValue().get("timestamp");
            if (timestamp instanceof LocalDateTime) {
                return new SimpleStringProperty(
                        ((LocalDateTime) timestamp).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
            }
            return new SimpleStringProperty("N/A");
        });
        eventTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getOrDefault("type", "UNKNOWN")))
        );
        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getOrDefault("message", "N/A")))
        );

        // Start the update timeline
        updateTimeline.play();

        // Initial update
        updateDashboard();
    }

    private void updateDashboard() throws SQLException {
        try {
            // Fetch system status
            Map<String, Object> status = monitorService.getSystemStatus();
            Map<String, Object> metrics = monitorService.getPerformanceMetrics();
            Map<String, Object> health = monitorService.checkDatabaseHealth();
            ObservableList<Map<String, Object>> events = monitorService.getSystemEvents();

            // Update system overview
            totalUsersLabel.setText(String.valueOf(status.getOrDefault("totalAccounts", "N/A")));
            activeUsersLabel.setText(String.valueOf(status.getOrDefault("activeUserCount", "N/A")));
            totalTransactionsLabel.setText(String.valueOf(status.getOrDefault("transactionCount24h", "N/A")));
            totalAmountLabel.setText("N/A"); // No amount data available
            uptimeLabel.setText(String.valueOf(status.getOrDefault("systemUptime", "N/A")));

            // Update performance metrics
            cpuUsageLabel.setText(String.format("%.1f%%", metrics.getOrDefault("cpuUsagePercent", 0.0)));
            memoryUsageLabel.setText(String.format("%d/%d MB",
                    metrics.getOrDefault("usedMemoryMB", 0L),
                    metrics.getOrDefault("totalMemoryMB", 0L)));
            diskUsageLabel.setText("N/A"); // No disk usage data available
            responseTimeLabel.setText(String.format("%d ms",
                    health.getOrDefault("responseTimeMs", 0L)));
            activeConnectionsLabel.setText(String.valueOf(metrics.getOrDefault("dbActiveConnections", "N/A")));

            // Update security events
            securityEventsTable.setItems(events != null ? events : FXCollections.emptyObservableList());

            // Update system health status
            boolean isHealthy = monitorService.systemHealthyProperty().get();
            healthStatusLabel.setText(isHealthy ? "System is healthy" : "System issues detected");
            healthStatusLabel.setStyle(isHealthy ?
                    "-fx-text-fill: green;" : "-fx-text-fill: red;");

            // Update last updated timestamp
            lastUpdatedLabel.setText("Last Updated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            AuditLogger.getInstance().logEvent(AuditLogger.AuditEventType.SYSTEM_ERROR,
                    "Error updating monitoring dashboard: " + e.getMessage());
            lastUpdatedLabel.setText("Error updating dashboard");
        }
    }

    public void stopUpdates() {
        updateTimeline.stop();
    }
}
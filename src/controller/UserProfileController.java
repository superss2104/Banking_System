package controller;

import Application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import model.Customer;
import utils.AuditLogger;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class UserProfileController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @FXML
    private Label usernameLabel;
    @FXML
    private Label userIdLabel;
    @FXML
    private Label firstNameLabel;
    @FXML
    private Label lastNameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label createdAtLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button backBtn;
    private Customer currentCustomer;
    private AuditLogger auditLogger;

    @FXML
    public void initialize() throws SQLException {
        Main app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();
        auditLogger = AuditLogger.getInstance();

        if (currentCustomer != null) {
            // Populate profile information
            usernameLabel.setText(currentCustomer.getUsername());
            userIdLabel.setText(currentCustomer.getUserId()); // Kept for FXML compatibility
            firstNameLabel.setText(currentCustomer.getFirstName());
            lastNameLabel.setText(currentCustomer.getLastName());
            emailLabel.setText(currentCustomer.getEmail());
            phoneLabel.setText(currentCustomer.getPhoneNumber());
            createdAtLabel.setText(currentCustomer.getCreatedAt() != null
                    ? currentCustomer.getCreatedAt().format(DATE_FORMATTER) : "Unknown");
            statusLabel.setText("Active"); // No status field in Customer

            messageLabel.setText("Welcome to your profile, " + currentCustomer.getFirstName() + "!");

            auditLogger.logEvent(AuditLogger.AuditEventType.NAVIGATION,
                    "User viewed profile", currentCustomer.getUserId());
        } else {
            messageLabel.setText("Error: No user is currently logged in!");
            statusLabel.setText("Error");

            // Keep back button enabled to allow return to log in
            backBtn.setDisable(false);
        }
    }

    @FXML
    public void handleBackToDashboard() {
        Main app = Main.getInstance();
        if (currentCustomer != null) {
            auditLogger.logEvent(AuditLogger.AuditEventType.NAVIGATION,
                    "User returned to dashboard from profile", currentCustomer.getUserId());
            if (app.isAdmin()) {
                Main.getSceneManager().switchToAdminDashboardScene();
            } else {
                Main.getSceneManager().switchToDashboardScene();
            }
        } else {
            auditLogger.logEvent(AuditLogger.AuditEventType.NAVIGATION,
                    "Redirected to login from profile (no user)");
            Main.getSceneManager().switchToLoginScene();
        }
    }
}
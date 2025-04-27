package controller;

import Application.Main;
import Service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utils.AlertHelper;

import java.sql.SQLException;

import static Application.Main.sceneManager;

public class LoginController {
    private final AuthService authService;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberMeCheckbox;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    public LoginController() throws SQLException {
        this.authService = new AuthService();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showErrorAlert("Login Error", "Missing Credentials",
                    "Please enter both username and password");
            return;
        }

        progressIndicator.setVisible(true);
        statusLabel.setText("Authenticating...");

        new Thread(() -> {
            try {
                boolean success = Main.getInstance().login(username, password); // Use Main.login
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    if (success) {
                        statusLabel.setText("Login successful!");
                        if (Main.getInstance().isAdmin()) {
                            sceneManager.switchToAdminDashboardScene();
                        } else {
                            sceneManager.switchToDashboardScene();
                        }
                    } else {
                        statusLabel.setText("Login failed! Invalid credentials.");
                        AlertHelper.showErrorAlert("Login Failed", "Invalid Credentials",
                                "The username or password you entered is incorrect.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Authentication error!");
                    AlertHelper.showErrorAlert("Login Error", "Authentication Failed",
                            e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            Main.getInstance().loadRegisterScene();
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Navigation Error", "Failed to load registration form",
                    e.getMessage());
        }
    }
}
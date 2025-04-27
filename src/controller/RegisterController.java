package controller;

import Application.Main;
import Service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Customer;
import utils.AlertHelper;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private Button registerButton;
    @FXML
    private Button backButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private AuthService authService;

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void initialize() {
        progressIndicator.setVisible(false);
        statusLabel.setText("");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || firstName.isEmpty() ||
                lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            AlertHelper.showErrorAlert("Error", "Missing Information",
                    "Please fill in all required fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            AlertHelper.showErrorAlert("Error", "Password Mismatch",
                    "Passwords do not match");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            AlertHelper.showErrorAlert("Registration Error", "Invalid Username",
                    "Username must be 3-20 characters long and contain only letters, numbers, or underscores.");
            return;
        }

        if (password.length() < 8) {
            AlertHelper.showErrorAlert("Registration Error", "Invalid Password",
                    "Password must be at least 8 characters long.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            AlertHelper.showErrorAlert("Registration Error", "Invalid Email",
                    "Please enter a valid email address");
            return;
        }

        if (!firstName.matches("^[a-zA-Z\\s]{2,50}$") || !lastName.matches("^[a-zA-Z\\s]{2,50}$")) {
            AlertHelper.showErrorAlert("Registration Error", "Invalid Name",
                    "First and last names must be 2-50 characters long and contain only letters and spaces.");
            return;
        }

        if (!phone.matches("^\\+?[0-9]{10,15}$")) {
            AlertHelper.showErrorAlert("Registration Error", "Invalid Phone Number",
                    "Please enter a valid phone number (10-15 digits, optional leading +).");
            return;
        }

        progressIndicator.setVisible(true);
        statusLabel.setText("Registering...");

        new Thread(() -> {
            try {
                Customer customer = authService.register(username, password, firstName, lastName, email, phone);
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    if (customer != null) {
                        statusLabel.setText("Registration successful!");
                        AlertHelper.showInformationAlert("Registration Success",
                                "Account Created",
                                "Your account has been created successfully. You can now login.");
                        Main.getSceneManager().switchToLoginScene();
                    } else {
                        AlertHelper.showErrorAlert("Registration Failed",
                                "System Error",
                                "An unexpected error occurred. Please try again later.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Registration error!");
                    AlertHelper.showErrorAlert("Registration Error", "Failed to Register",
                            "An unexpected error occurred: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        Main.getSceneManager().switchToLoginScene();
    }
}
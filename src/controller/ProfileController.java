package controller;

import Application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Customer;
import utils.AlertHelper;

public class ProfileController {

    @FXML
    private Label messageLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label userIdLabel;
    @FXML
    private Label usernameLabel;
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

    private Customer currentCustomer;
    private Main app;

    @FXML
    public void initialize() {
        app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();

        if (currentCustomer != null) {
            messageLabel.setText("Welcome, " + (currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "Guest"));
            userIdLabel.setText(currentCustomer.getUsername()); // Using username as userId
            usernameLabel.setText(currentCustomer.getUsername());
            firstNameLabel.setText(currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "");
            lastNameLabel.setText(currentCustomer.getLastName() != null ? currentCustomer.getLastName() : "");
            emailLabel.setText(currentCustomer.getEmail() != null ? currentCustomer.getEmail() : "");
            phoneLabel.setText(currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "");
            createdAtLabel.setText(currentCustomer.getCreatedAt() != null ? currentCustomer.getCreatedAt().toString() : "");
            statusLabel.setText("Ready");
        } else {
            statusLabel.setText("Error: No user available.");
            AlertHelper.showWarningAlert("Warning", "Initialization Error", "Please log in.");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToDashboardScene();
            statusLabel.setText("Returned to Dashboard.");
        } else {
            statusLabel.setText("Error: Cannot return to dashboard.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to return to dashboard.");
        }
    }
}
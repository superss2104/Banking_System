package controller;

import Application.Main;
import Service.AccountService;
import Service.TransactionService;
import Service.UPIService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Customer;
import utils.AlertHelper;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    private Customer currentCustomer;
    private AccountService accountService;
    private TransactionService transactionService;
    private UPIService upiService;
    private Main app; // Reference to Main instance

    @FXML
    public void initialize() {
        app = Main.getInstance(); // Get the singleton instance of Main
        currentCustomer = app.getCurrentCustomer();
        accountService = app.getAccountService();
        transactionService = app.getTransactionService();
        upiService = app.getUpiService();

        if (currentCustomer != null && accountService != null && transactionService != null && upiService != null) {
            welcomeLabel.setText("Welcome, " + (currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "Guest"));
        } else {
            welcomeLabel.setText("Welcome, Guest");
            AlertHelper.showWarningAlert("Warning", "Initialization Error", "Please log in or check service availability.");
        }
    }

    @FXML
    private void handleLogout() {
        if (app != null) {
            app.logout();
        }
    }

    @FXML
    private void handleAccountOperations() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToAccountManagementScene(); // Use Main to access SceneManager
        } else {
            welcomeLabel.setText("Error: SceneManager not available.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "SceneManager is not initialized.");
        }
    }

    @FXML
    private void handleTransactionOperations() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToTransactionOperationsScene(); // Use Main to access SceneManager
        } else {
            welcomeLabel.setText("Error: SceneManager not available.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "SceneManager is not initialized.");
        }
    }

    @FXML
    private void handleUpiServices() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToUpiServicesScene(); // Use Main to access SceneManager
        } else {
            welcomeLabel.setText("Error: SceneManager not available.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "SceneManager is not initialized.");
        }
    }

    @FXML
    private void handleViewProfile() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToProfileViewScene(); // Use Main to access SceneManager
        } else {
            welcomeLabel.setText("Error: SceneManager not available.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "SceneManager is not initialized.");
        }
    }

    @FXML
    private void handleHelp() {
        AlertHelper.showInformationAlert("Help", "Customer Help Information",
                "Contact support at cs24b043@iittp.ac.in / cs24b041@iittp.ac.in for assistance.");
        if (currentCustomer != null) {
            System.out.println("Help displayed for " + currentCustomer.getUsername());
        }
    }
}
package controller;

import Application.Main;
import Service.AccountService;
import Service.TransactionService;
import Service.UPIService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Customer;
import utils.AlertHelper;

import java.math.BigDecimal;

public class CustomerDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TextField fromUpiIdField;
    @FXML
    private TextField toUpiIdField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField descriptionField;

    private Customer currentCustomer;
    private AccountService accountService;
    private TransactionService transactionService;
    private UPIService upiService;
    private Main app;

    @FXML
    public void initialize() {
        app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();
        accountService = app.getAccountService();
        transactionService = app.getTransactionService();
        upiService = app.getUpiService();

        if (currentCustomer != null && accountService != null && transactionService != null && upiService != null) {
            welcomeLabel.setText("Welcome, " + (currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "Guest"));
            System.out.println("CustomerDashboard initialized for user: " + currentCustomer.getUserId());
        } else {
            System.err.println("No user or services are currently available!");
            welcomeLabel.setText("Welcome, Guest");
            AlertHelper.showWarningAlert("Warning", "Initialization Error", "Please log in or check service availability.");
        }
    }

    @FXML
    private void handleLogout() {
        if (app != null) {
            app.logout();
        } else {
            System.err.println("Cannot logout: Main instance unavailable.");
            AlertHelper.showErrorAlert("Error", "Logout Failed", "Application context is unavailable.");
        }
    }

    @FXML
    private void handleViewAccounts() {
        if (currentCustomer != null && accountService != null) {
            accountService.viewMyAccounts(currentCustomer.getUserId());
        } else {
            System.err.println("Cannot view accounts: No logged-in customer or account service unavailable.");
            AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to view accounts.");
        }
    }

    @FXML
    private void handleViewTransactions() {
        if (currentCustomer != null && transactionService != null) {
            transactionService.getTransactionHistory(currentCustomer.getUserId());
        } else {
            System.err.println("Cannot view transactions: No logged-in customer or transaction service unavailable.");
            AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to view transactions.");
        }
    }

    @FXML
    private void handleViewProfile() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToProfileScene();
        } else {
            System.err.println("Cannot switch to profile view: Scene manager unavailable.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to load profile view.");
        }
    }

    @FXML
    private void handleMakePayment() {
        if (currentCustomer != null && upiService != null) {
            try {
                String fromUpiId = fromUpiIdField.getText().trim();
                String toUpiId = toUpiIdField.getText().trim();
                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountField.getText().trim());
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new NumberFormatException("Amount must be positive");
                    }
                } catch (NumberFormatException e) {
                    AlertHelper.showErrorAlert("Error", "Invalid Amount", "Please enter a valid positive amount.");
                    return;
                }

                // Validate UPI IDs
                if (!upiService.isValidUpiId(fromUpiId) || !upiService.isValidUpiId(toUpiId)) {
                    AlertHelper.showErrorAlert("Error", "Invalid UPI ID", "Please enter valid UPI IDs (e.g., user@bank).");
                    return;
                }

                // Check if the fromUpiId is registered to the current user
                String accountId = upiService.getAccountIdByUpiId(fromUpiId);
                if (accountId == null || !upiService.hasSufficientBalance(accountId, amount)) {
                    AlertHelper.showErrorAlert("Error", "Insufficient Funds", "Insufficient balance or invalid UPI ID.");
                    return;
                }

                String description = descriptionField.getText().trim(); //Get text from TextField
                boolean success = upiService.makeUPIPayment(fromUpiId, toUpiId, amount, description);
                if (success) {
                    AlertHelper.showInformationAlert("Success", "UPI Payment", "Payment process completed successfully.");
                    fromUpiIdField.clear();
                    toUpiIdField.clear();
                    amountField.clear();
                    descriptionField.clear();
                } else {
                    AlertHelper.showErrorAlert("Error", "UPI Payment Failed", "Payment initiation failed. Check logs for details.");
                }
            } catch (Exception e) {
                System.err.println("UPI payment failed: " + e.getMessage());
                AlertHelper.showErrorAlert("Error", "UPI Payment Failed", e.getMessage());
            }
        } else {
            System.err.println("Cannot make payment: No logged-in customer or UPI service unavailable.");
            AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to make a payment.");
        }
    }

    @FXML
    private void handleAccountOperations() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToAccountOperationsScene();
        } else {
            System.err.println("Cannot switch to account operations: Scene manager unavailable.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to load account operations.");
        }
    }

    @FXML
    private void handleTransactionOperations() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToTransactionScene();
        } else {
            System.err.println("Cannot switch to transaction operations: Scene manager unavailable.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to load transaction operations.");
        }
    }

    @FXML
    private void handleUPIServices() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToUpiScene();
        } else {
            System.err.println("Cannot switch to UPI services: Scene manager unavailable.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to load UPI services.");
        }
    }

    @FXML
    private void handleHelp() {
        AlertHelper.showInformationAlert("Help", "Customer Help Information",
                "Contact support at cs24b043@iittp.ac.in / cs24b041@iittp.ac.in for assistance.");
        System.out.println("Customer Help: Contact support at cs24b041@iittp.ac.in / cs24b043@iittp.ac.in");
    }
}
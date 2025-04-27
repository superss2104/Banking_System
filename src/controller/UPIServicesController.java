package controller;

import Application.Main;
import Service.AccountService;
import Service.UPIService;
import database.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Account;
import model.Customer;
import utils.AlertHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UPIServicesController {

    private static final Logger logger = Logger.getLogger(UPIServicesController.class.getName());
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField upiIdField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField descriptionField;
    @FXML
    private Button registerUpiBtn;
    @FXML
    private Button makePaymentBtn;
    @FXML
    private Button checkStatusBtn;
    @FXML
    private ComboBox<String> accountComboBox;
    @FXML
    private ComboBox<String> fromUpiComboBox;
    @FXML
    private ComboBox<String> toUpiComboBox;
    private Customer currentCustomer;
    private UPIService upiService;
    private Main app;
    private AccountService accountService;

    @FXML
    public void initialize() {
        app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();
        upiService = app.getUpiService();
        accountService = app.getAccountService();

        if (currentCustomer == null || upiService == null || accountService == null) {
            statusLabel.setText("Error: No user, UPI service, or account service available.");
            AlertHelper.showWarningAlert("Warning", "Initialization Error", "Please log in or check service availability.");
            return;
        }

        logger.info("Initializing UPIServicesController for user: " + currentCustomer.getUsername());
        welcomeLabel.setText("Welcome, " + (currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "Guest"));
        statusLabel.setText("Ready");
        populateAccountList();
        populateUpiComboBoxes();
    }

    private void populateAccountList() {
        if (currentCustomer != null && accountService != null) {
            String username = currentCustomer.getUsername();
            List<Account> userAccounts = accountService.getAccountsByUsername(username);
            if (userAccounts != null && !userAccounts.isEmpty()) {
                accountComboBox.setItems(FXCollections.observableArrayList(
                        userAccounts.stream()
                                .map(account -> account.getAccountId() + " - " + account.getAccountType() + " (" + account.getStatus() + ")")
                                .toList()
                ));
                accountComboBox.getSelectionModel().select(0);
            } else {
                statusLabel.setText("No accounts found for " + username);
                AlertHelper.showWarningAlert("Warning", "No Accounts", "No accounts found for " + username + ". Please create an account first.");
            }
        }
    }

    private void populateUpiComboBoxes() {
        try {
            String username = currentCustomer.getUsername();
            logger.info("Populating UPI IDs for username: " + username);
            List<String> upiIds = upiService.getUpiIdsByUsername(username);
            logger.info("Retrieved UPI IDs for 'From': " + upiIds);
            if (upiIds != null && !upiIds.isEmpty()) {
                fromUpiComboBox.setItems(FXCollections.observableArrayList(upiIds));
                fromUpiComboBox.getSelectionModel().selectFirst();
            } else {
                logger.warning("No UPI IDs found for username: " + username);
                fromUpiComboBox.setItems(FXCollections.observableArrayList());
            }

            List<String> allUpiIds = upiService.getAllUpiIds();
            logger.info("Retrieved all UPI IDs for 'To': " + allUpiIds);
            if (allUpiIds != null && !allUpiIds.isEmpty()) {
                toUpiComboBox.setItems(FXCollections.observableArrayList(allUpiIds));
                toUpiComboBox.getSelectionModel().selectFirst();
            } else {
                logger.warning("No UPI IDs available.");
                toUpiComboBox.setItems(FXCollections.observableArrayList());
            }
        } catch (Exception e) {
            logger.severe("Error populating UPI combo boxes: " + e.getMessage());
            AlertHelper.showErrorAlert("Error", "Initialization Error", "Failed to load UPI IDs.");
        }
    }

    @FXML
    private void handleRegisterUPI() {
        if (currentCustomer != null && upiService != null) {
            String upiId = upiIdField.getText().trim();
            if (upiId.isEmpty() || !upiService.isValidUpiId(upiId)) {
                statusLabel.setText("Invalid UPI ID format.");
                AlertHelper.showWarningAlert("Warning", "Invalid Input", "UPI ID should be in the format 'username@provider'.");
                return;
            }

            String selectedAccount = accountComboBox.getSelectionModel().getSelectedItem();
            if (selectedAccount == null) {
                statusLabel.setText("Please select an account.");
                AlertHelper.showWarningAlert("Warning", "Selection Required", "Please select an account from the list.");
                return;
            }
            String accountId = selectedAccount.split(" - ")[0];

            if (upiService.registerUpiId(currentCustomer, accountId, upiId)) {
                statusLabel.setText("UPI ID " + upiId + " registered for account " + accountId);
                AlertHelper.showInformationAlert("Success", "Registration Complete", "UPI ID " + upiId + " registered successfully.");
                upiIdField.clear();
                populateUpiComboBoxes();
            } else {
                statusLabel.setText("Failed to register UPI ID " + upiId + ". Check logs.");
                AlertHelper.showErrorAlert("Error", "Registration Failed", "Failed to register UPI ID " + upiId + ". Please check the logs for details.");
            }
        }
    }

    @FXML
    private void handleMakePayment() {
        if (currentCustomer == null || upiService == null) {
            statusLabel.setText("Error: No logged-in user or UPI service.");
            AlertHelper.showErrorAlert("Error", "Authentication Error", "Please log in to make a payment.");
            return;
        }

        String fromUpiId = fromUpiComboBox.getValue();
        String toUpiId = toUpiComboBox.getValue();
        if (fromUpiId == null || toUpiId == null || fromUpiId.equals(toUpiId)) {
            statusLabel.setText("Please select valid different UPI IDs.");
            AlertHelper.showWarningAlert("Warning", "Invalid Selection", "Please select valid sender and receiver UPI IDs.");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Invalid amount.");
                AlertHelper.showWarningAlert("Warning", "Invalid Input", "Please enter a positive amount.");
                return;
            }
            String description = descriptionField.getText().trim();
            if (description.isEmpty()) {
                description = "UPI Payment from " + fromUpiId + " to " + toUpiId;
            }

            if (!upiService.isValidUpiId(toUpiId)) {
                statusLabel.setText("Invalid recipient UPI ID.");
                AlertHelper.showErrorAlert("Error", "Invalid UPI ID", "Recipient UPI ID not found.");
                return;
            }

            String accountId = upiService.getAccountIdByUpiId(fromUpiId);
            if (accountId == null || !upiService.hasSufficientBalance(accountId, amount)) {
                statusLabel.setText("Insufficient funds.");
                AlertHelper.showErrorAlert("Error", "Insufficient Funds", "Insufficient balance or invalid UPI ID.");
                return;
            }

            boolean confirmed = AlertHelper.showConfirmation("Confirm Payment", null,
                    String.format("From: %s\nTo: %s\nAmount: $%s\nDescription: %s",
                            fromUpiId, toUpiId, amount, description));
            if (!confirmed) {
                return;
            }

            boolean success = upiService.makeUPIPayment(fromUpiId, toUpiId, amount, description);
            if (success) {
                statusLabel.setText("Payment successful from " + fromUpiId + " to " + toUpiId);
                AlertHelper.showInformationAlert("Success", "Payment Complete", "Payment of $" + amount + " completed.");
                amountField.clear();
                descriptionField.clear();
            } else {
                statusLabel.setText("Payment failed from " + fromUpiId + " to " + toUpiId);
                AlertHelper.showErrorAlert("Error", "Payment Failed", "Payment failed. Check logs for details.");
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount format.");
            AlertHelper.showWarningAlert("Warning", "Invalid Input", "Please enter a valid amount.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during payment: " + e.getMessage(), e);
            statusLabel.setText("Database error occurred.");
            AlertHelper.showErrorAlert("Error", "Database Error", "Failed to process payment.");
        }
    }

    @FXML
    private void handleCheckStatus() {
        if (currentCustomer != null && upiService != null) {
            String transactionId = upiIdField.getText().trim();
            if (transactionId.isEmpty()) {
                statusLabel.setText("Please enter a transaction ID.");
                AlertHelper.showWarningAlert("Warning", "Invalid Input", "Please enter a transaction ID.");
                return;
            }

            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT status FROM transactions WHERE transaction_id = ?")) {
                stmt.setString(1, transactionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        statusLabel.setText("Status for " + transactionId + ": " + status);
                        AlertHelper.showInformationAlert("Status", "Transaction Status", "Status for " + transactionId + ": " + status);
                    } else {
                        statusLabel.setText("Transaction not found.");
                        AlertHelper.showWarningAlert("Warning", "Not Found", "Transaction " + transactionId + " not found.");
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error checking transaction status", e);
                statusLabel.setText("Error checking status.");
                AlertHelper.showErrorAlert("Error", "Database Error", "Failed to check transaction status.");
            }
        }
    }

    @FXML
    private void handleHelp() {
        AlertHelper.showInformationAlert("Help", "UPI Help", "Contact support at cs24b043@iittp.ac.in / cs24b041@iittp.ac.in for UPI assistance.");
        statusLabel.setText("Help displayed.");
    }

    @FXML
    private void handleBackToMain() {
        if (app != null && Main.getSceneManager() != null) {
            Main.getSceneManager().switchToDashboardScene();
            statusLabel.setText("Returned to Dashboard.");
        } else {
            statusLabel.setText("Error: Cannot return to dashboard.");
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to return to dashboard.");
        }
    }
}
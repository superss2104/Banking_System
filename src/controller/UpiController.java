package controller;

import Application.Main;
import Service.AccountService;
import Service.UPIService;
import database.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Account;
import model.Customer;
import utils.AlertHelper;
import utils.AuditLogger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class UpiController {
    private static final Logger logger = Logger.getLogger(UpiController.class.getName());
    private final ObservableList<String> upiIdsList = FXCollections.observableArrayList();
    @FXML
    private TabPane upiTabPane;
    @FXML
    private TextField newUpiIdField;
    @FXML
    private ListView<String> existingUpiIdsList;
    @FXML
    private Button registerUpiButton;
    @FXML
    private ComboBox<String> sourceUpiCombo;
    @FXML
    private TextField recipientUpiField;
    @FXML
    private TextField paymentAmountField;
    @FXML
    private TextField paymentDescriptionField;
    @FXML
    private Button makePaymentButton;
    @FXML
    private TextField transactionIdField;
    @FXML
    private Button checkStatusButton;
    @FXML
    private Label statusResultLabel;
    @FXML
    private Button backButton;
    private Customer currentCustomer;
    private UPIService upiService;
    private AccountService accountService;
    private AuditLogger auditLogger;

    @FXML
    private void initialize() throws SQLException {
        Main app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();
        upiService = app.getUpiService();
        accountService = app.getAccountService();
        auditLogger = AuditLogger.getInstance();

        if (currentCustomer == null) {
            AlertHelper.showErrorAlert("Error", null, "No user logged in");
            disableControls();
            return;
        }

        logger.info("Initializing UpiController for user: " + currentCustomer.getUsername());
        loadUpiIds();
    }

    private void disableControls() {
        registerUpiButton.setDisable(true);
        makePaymentButton.setDisable(true);
        checkStatusButton.setDisable(true);
    }

    private void loadUpiIds() {
        try {
            String username = currentCustomer.getUsername();
            logger.info("Loading UPI IDs for username: " + username);
            if (username == null || username.trim().isEmpty()) {
                logger.severe("Username is null or empty for current user");
                AlertHelper.showErrorAlert("Error", null, "Invalid user session");
                return;
            }

            List<String> upiIds = upiService.getUpiIdsByUsername(username);
            logger.info("Retrieved UPI IDs: " + upiIds);
            upiIdsList.clear();
            if (upiIds != null && !upiIds.isEmpty()) {
                upiIdsList.addAll(upiIds);
            } else {
                logger.warning("No UPI IDs found for username: " + username);
            }
            existingUpiIdsList.setItems(upiIdsList);
            sourceUpiCombo.setItems(upiIdsList);
        } catch (Exception e) {
            logger.severe("Error loading UPI IDs: " + e.getMessage());
            AlertHelper.showErrorAlert("Error", null, "Error loading UPI IDs");
        }
    }

    @FXML
    private void handleRegisterUpi(ActionEvent event) {
        if (currentCustomer == null) {
            AlertHelper.showErrorAlert("Error", null, "No user logged in");
            return;
        }

        String upiId = newUpiIdField.getText().trim();
        if (upiId.isEmpty()) {
            AlertHelper.showErrorAlert("Error", null, "Please enter a UPI ID");
            return;
        }

        if (!upiId.contains("@") || upiId.length() < 5) {
            AlertHelper.showErrorAlert("Error", null, "Invalid UPI ID format",
                    "UPI ID should be in the format 'username@provider'");
            return;
        }

        try {
            List<Account> accounts = accountService.getAccountsByUsername(currentCustomer.getUserId());
            if (accounts == null || accounts.isEmpty()) {
                AlertHelper.showErrorAlert("Error", null, "No accounts found");
                return;
            }

            Account selectedAccount = accounts.get(0); // Default to first account
            if (accounts.size() > 1) {
                String accountDetails = accounts.stream()
                        .map(a -> a.getAccountId() + " (" + a.getAccountType() + ", Balance: $" + a.getBalance() + ")")
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
                boolean confirmed = AlertHelper.showConfirmation("Confirm Account", null,
                        "Multiple accounts found. Using:\n" + selectedAccount.getAccountId() +
                                "\n\nAvailable accounts:\n" + accountDetails);
                if (!confirmed) {
                    return;
                }
            }

            boolean success = upiService.registerUpiId(currentCustomer, selectedAccount.getAccountId(), upiId);
            if (success) {
                auditLogger.logEvent(AuditLogger.AuditEventType.UPI_REGISTRATION,
                        "UPI ID registered: " + upiId, currentCustomer.getUserId());
                AlertHelper.showInformationAlert("Success", null,
                        "UPI ID registered successfully: " + upiId);
                newUpiIdField.clear();
                loadUpiIds();
            } else {
                AlertHelper.showErrorAlert("Error", null, "Failed to register UPI ID",
                        "This UPI ID may already be registered or the account is invalid.");
            }
        } catch (Exception e) {
            logger.severe("Error registering UPI ID: " + e.getMessage());
            AlertHelper.showErrorAlert("Error", null, "Error registering UPI ID");
        }
    }

    @FXML
    private void handleMakePayment(ActionEvent event) {
        if (currentCustomer == null) {
            AlertHelper.showErrorAlert("Error", null, "No user logged in");
            return;
        }

        String fromUpiId = sourceUpiCombo.getValue();
        String toUpiId = recipientUpiField.getText().trim();
        String amountText = paymentAmountField.getText().trim();

        if (fromUpiId == null || fromUpiId.isEmpty()) {
            AlertHelper.showErrorAlert("Error", null, "Please select a source UPI ID");
            return;
        }

        if (toUpiId.isEmpty()) {
            AlertHelper.showErrorAlert("Error", null, "Please enter a recipient UPI ID");
            return;
        }

        if (fromUpiId.equals(toUpiId)) {
            AlertHelper.showErrorAlert("Error", null, "Source and recipient UPI IDs cannot be the same");
            return;
        }

        if (amountText.isEmpty()) {
            AlertHelper.showErrorAlert("Error", null, "Please enter an amount");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertHelper.showErrorAlert("Error", null, "Amount must be greater than zero");
                return;
            }

            String description = paymentDescriptionField.getText().trim();
            if (description.isEmpty()) {
                description = "UPI Payment from " + fromUpiId + " to " + toUpiId;
            }

            if (!upiService.isValidUpiId(toUpiId)) {
                AlertHelper.showErrorAlert("Error", null, "Recipient UPI ID not found");
                return;
            }

            String accountId = upiService.getAccountIdByUpiId(fromUpiId);
            if (accountId == null || !upiService.hasSufficientBalance(accountId, amount)) {
                AlertHelper.showErrorAlert("Error", null, "Insufficient balance for this payment");
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
                auditLogger.logEvent(AuditLogger.AuditEventType.UPI_TRANSACTION,
                        "UPI payment completed: " + fromUpiId + " to " + toUpiId, currentCustomer.getUserId());
                AlertHelper.showInformationAlert("Success", null,
                        "Payment successful. Transaction IDs not available in current setup."); // Adjust if transaction ID is added
                paymentAmountField.clear();
                paymentDescriptionField.clear();
                recipientUpiField.clear();
                transactionIdField.setText("");
                upiTabPane.getSelectionModel().select(2); // Assuming tab 2 is status
            } else {
                AlertHelper.showErrorAlert("Error", null, "Failed to complete payment.");
            }
        } catch (NumberFormatException e) {
            logger.severe("Invalid amount format: " + e.getMessage());
            AlertHelper.showErrorAlert("Error", null, "Invalid amount format");
        } catch (Exception e) {
            logger.severe("Error during payment: " + e.getMessage());
            AlertHelper.showErrorAlert("Error", null, "An error occurred during payment");
        }
    }

    @FXML
    private void handleCheckStatus(ActionEvent event) {
        String transactionId = transactionIdField.getText().trim();

        if (transactionId.isEmpty()) {
            AlertHelper.showErrorAlert("Error", null, "Please enter a transaction ID");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT transaction_id, status, timestamp FROM transactions WHERE transaction_id = ?")) {
            stmt.setString(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    String timestamp = rs.getTimestamp("timestamp").toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    auditLogger.logEvent(AuditLogger.AuditEventType.UPI_TRANSACTION,
                            "Checked UPI transaction status: " + transactionId,
                            currentCustomer != null ? currentCustomer.getUserId() : "SYSTEM");
                    statusResultLabel.setText(String.format("Transaction ID: %s\nStatus: %s\nDate: %s",
                            transactionId, status, timestamp));
                } else {
                    statusResultLabel.setText("Transaction not found");
                    AlertHelper.showErrorAlert("Error", null, "Transaction not found");
                }
            }
        } catch (SQLException e) {
            logger.severe("Error checking transaction status: " + e.getMessage());
            AlertHelper.showErrorAlert("Error", null, "Error checking transaction status");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        Main app = Main.getInstance();
        if (app.isAdmin()) {
            Main.getSceneManager().switchToAdminDashboardScene();
        } else {
            Main.getSceneManager().switchToDashboardScene();
        }
    }
}
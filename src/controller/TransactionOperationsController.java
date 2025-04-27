package controller;

import Application.Main;
import Service.AccountService;
import Service.TransactionService;
import database.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Account;
import model.Customer;
import model.Transaction;
import utils.AlertHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionOperationsController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<String> accountComboBox;
    @FXML
    private HBox toAccountContainer;
    @FXML
    private ComboBox<String> toAccountComboBox;
    @FXML
    private TextField amountField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Button depositButton;
    @FXML
    private Button withdrawButton;
    @FXML
    private Button transferButton;
    @FXML
    private Button viewHistoryButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button backButton;
    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, String> transactionIdColumn;
    @FXML
    private TableColumn<Transaction, String> dateColumn;
    @FXML
    private TableColumn<Transaction, String> typeColumn;
    @FXML
    private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML
    private TableColumn<Transaction, String> fromAccountColumn;
    @FXML
    private TableColumn<Transaction, String> toAccountColumn;
    @FXML
    private TableColumn<Transaction, String> statusColumn;
    @FXML
    private TableColumn<Transaction, String> descriptionColumn;

    private Customer currentCustomer;
    private AccountService accountService;
    private TransactionService transactionService;
    private Main app;

    @FXML
    public void initialize() {
        app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();
        accountService = app.getAccountService();
        transactionService = app.getTransactionService();

        if (currentCustomer == null || accountService == null || transactionService == null) {
            statusLabel.setText("Error: Missing user or services.");
            AlertHelper.showWarningAlert("Warning", "Initialization Error", "Please log in or check service availability.");
            return;
        }

        welcomeLabel.setText("Welcome, " + (currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "Guest"));
        statusLabel.setText("Ready");
        populateAccountComboBox();
        setupTableColumns();
        toggleTransferSection(false);
    }

    private void populateAccountComboBox() {
        if (accountService != null && currentCustomer != null) {
            // Fetch all accounts directly from the database
            List<Account> allAccounts = new ArrayList<>();
            String sql = "SELECT account_id, username, account_type, balance, created_at, last_updated, status FROM accounts";
            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Account account = new Account(
                            rs.getString("account_id"),
                            rs.getString("username"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_updated").toLocalDateTime(),
                            rs.getString("status")
                    );
                    allAccounts.add(account);
                }
                DatabaseConnector.releaseConnection();
            } catch (SQLException e) {
                statusLabel.setText("Error retrieving accounts from database.");
                DatabaseConnector.releaseConnection();
                return;
            }

            if (!allAccounts.isEmpty()) {
                var allAccountIds = FXCollections.observableArrayList(
                        allAccounts.stream()
                                .map(account -> String.format("%s - %s (Balance: %s)",
                                        account.getAccountId(),
                                        account.getAccountType(),
                                        account.getBalance()))
                                .toList()
                );
                toAccountComboBox.setItems(allAccountIds);
            } else {
                statusLabel.setText("No accounts found in the database.");
            }

            String username = currentCustomer.getUsername();
            List<Account> userAccounts = accountService.getAccountsByUsername(username);
            if (userAccounts != null && !userAccounts.isEmpty()) {
                var userAccountIds = FXCollections.observableArrayList(
                        userAccounts.stream()
                                .map(account -> String.format("%s - %s (Balance: %s)",
                                        account.getAccountId(),
                                        account.getAccountType(),
                                        account.getBalance()))
                                .toList()
                );
                accountComboBox.setItems(userAccountIds);
            } else {
                statusLabel.setText("No accounts found for user: " + username);
            }
        }
    }

    private void setupTableColumns() {
        transactionIdColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTransactionId()));
        dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTimestamp().toString()));
        typeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType()));
        amountColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAmount()));
        fromAccountColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFromAccountId() != null ? cellData.getValue().getFromAccountId() : ""));
        toAccountColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getToAccountId() != null ? cellData.getValue().getToAccountId() : ""));
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        descriptionColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription() != null ? cellData.getValue().getDescription() : ""));
    }

    private void toggleTransferSection(boolean show) {
        toAccountContainer.setVisible(show);
        toAccountContainer.setManaged(show);
        if (!show) {
            toAccountComboBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    private void handleDeposit() {
        toggleTransferSection(false);
        String accountId = accountComboBox.getValue();
        if (accountId == null || accountId.trim().isEmpty()) {
            statusLabel.setText("Please select an account.");
            return;
        }
        accountId = accountId.split(" - ")[0];
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                statusLabel.setText("Amount must be positive.");
                return;
            }
            String description = descriptionField.getText().trim();
            Transaction transaction = transactionService.deposit(accountId, amount, description.isEmpty() ? null : description);
            if (transaction != null) {
                statusLabel.setText("Deposit successful for " + accountId);
                refreshTransactionTable();
                populateAccountComboBox();
                app.requestBalanceRefresh(accountId);
            } else {
                statusLabel.setText("Deposit failed for " + accountId + ". Check logs.");
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount format.");
        }
    }

    @FXML
    private void handleWithdraw() {
        toggleTransferSection(false);
        String accountId = accountComboBox.getValue();
        if (accountId == null || accountId.trim().isEmpty()) {
            statusLabel.setText("Please select an account.");
            return;
        }
        accountId = accountId.split(" - ")[0];
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                statusLabel.setText("Amount must be positive.");
                return;
            }
            String description = descriptionField.getText().trim();
            Transaction transaction = transactionService.withdrawFunds(accountId, amount, description.isEmpty() ? null : description);
            if (transaction != null) {
                statusLabel.setText("Withdrawal successful for " + accountId);
                refreshTransactionTable();
                populateAccountComboBox();
                app.requestBalanceRefresh(accountId);
            } else {
                statusLabel.setText("Withdrawal failed for " + accountId + ". Check logs.");
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount format.");
        }
    }

    @FXML
    private void handleTransfer() {
        toggleTransferSection(true);
        String fromAccountId = accountComboBox.getValue();
        String toAccountId = toAccountComboBox.getValue();

        if (fromAccountId == null || fromAccountId.trim().isEmpty()) {
            statusLabel.setText("Please select a 'From' account from the drop-down menu.");
            return;
        }
        if (toAccountId == null || toAccountId.trim().isEmpty()) {
            statusLabel.setText("Please select a 'To' account from the drop-down menu.");
            return;
        }
        fromAccountId = fromAccountId.split(" - ")[0];
        toAccountId = toAccountId.split(" - ")[0];
        if (fromAccountId.equals(toAccountId)) {
            statusLabel.setText("Please select different 'From' and 'To' accounts.");
            return;
        }

        String username = currentCustomer.getUsername();
        List<Account> userAccounts = accountService.getAccountsByUsername(username);
        String finalFromAccountId = fromAccountId;
        boolean isFromAccountOwned = userAccounts.stream()
                .anyMatch(account -> account.getAccountId().equals(finalFromAccountId));
        if (!isFromAccountOwned) {
            statusLabel.setText("The 'From' account does not belong to you.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                statusLabel.setText("Amount must be positive.");
                return;
            }
            String description = descriptionField.getText().trim().isEmpty() ? null : descriptionField.getText().trim();

            Transaction transaction = transactionService.transferFunds(fromAccountId, toAccountId, amount, description);
            if (transaction != null) {
                statusLabel.setText("Transfer successful from " + fromAccountId + " to " + toAccountId);
                refreshTransactionTable();
                populateAccountComboBox();
                app.requestBalanceRefresh(fromAccountId);
                app.requestBalanceRefresh(toAccountId);
            } else {
                statusLabel.setText("Transfer failed from " + fromAccountId + " to " + toAccountId + ". Check logs.");
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount format.");
        }
    }

    @FXML
    private void handleViewHistory() {
        if (currentCustomer != null && transactionService != null) {
            String username = currentCustomer.getUsername();
            List<Transaction> transactions = transactionService.getTransactionHistory(username);
            if (transactions != null) {
                transactionTable.setItems(FXCollections.observableArrayList(transactions));
                statusLabel.setText("Transaction history loaded for " + username);
            } else {
                statusLabel.setText("No transaction history found for " + username);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        populateAccountComboBox();
        refreshTransactionTable();
        statusLabel.setText("Refreshed accounts and transactions.");
    }

    private void refreshTransactionTable() {
        if (currentCustomer != null && transactionService != null) {
            String username = currentCustomer.getUsername();
            List<Transaction> transactions = transactionService.getTransactionHistory(username);
            if (transactions != null) {
                transactionTable.setItems(FXCollections.observableArrayList(transactions));
            } else {
                transactionTable.setItems(FXCollections.emptyObservableList());
            }
        }
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
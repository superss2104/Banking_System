package controller;

import Application.Main;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Account;
import model.Transaction;
import utils.AlertHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionController {
    private final ObservableList<Account> accountsList = FXCollections.observableArrayList();
    private final ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    @FXML
    private TabPane transactionTabPane;
    // Deposit Tab
    @FXML
    private ComboBox<Account> depositAccountCombo;
    @FXML
    private TextField depositAmountField;
    @FXML
    private Button depositButton;
    // Withdraw Tab
    @FXML
    private ComboBox<Account> withdrawAccountCombo;
    @FXML
    private TextField withdrawAmountField;
    @FXML
    private Button withdrawButton;
    // Transfer Tab
    @FXML
    private ComboBox<Account> sourceAccountCombo;
    @FXML
    private TextField destinationAccountField;
    @FXML
    private TextField transferAmountField;
    @FXML
    private TextField transferDescriptionField;
    @FXML
    private Button transferButton;
    // History Tab
    @FXML
    private ComboBox<Account> historyAccountCombo;
    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, String> transactionIdColumn;
    @FXML
    private TableColumn<Transaction, String> fromAccountColumn;
    @FXML
    private TableColumn<Transaction, String> toAccountColumn;
    @FXML
    private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML
    private TableColumn<Transaction, String> typeColumn;
    @FXML
    private TableColumn<Transaction, String> statusColumn;
    @FXML
    private TableColumn<Transaction, LocalDateTime> timestampColumn;
    @FXML
    private TableColumn<Transaction, String> descriptionColumn;
    @FXML
    private Button viewHistoryButton;
    @FXML
    private Button backButton;

    @FXML
    private void initialize() {
        loadAccounts();

        // Configure transaction table columns
        transactionIdColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        fromAccountColumn.setCellValueFactory(new PropertyValueFactory<>("fromAccountId"));
        toAccountColumn.setCellValueFactory(new PropertyValueFactory<>("toAccountId"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription() != null ?
                        cellData.getValue().getDescription() : ""));

        // Format timestamp column
        timestampColumn.setCellFactory(column -> new TableCell<Transaction, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });
    }

    private void loadAccounts() {
        try {
            Main app = Main.getInstance();
            if (app.getCurrentCustomer() == null) {
                AlertHelper.showErrorAlert("Error", "No User", "No logged-in user found");
                return;
            }
            String userId = app.getCurrentCustomer().getUserId();
            List<Account> accounts = app.getAccountService().getAccountsByUsername(userId);

            accountsList.clear();
            if (accounts != null) {
                accountsList.addAll(accounts);
            }

            depositAccountCombo.setItems(accountsList);
            withdrawAccountCombo.setItems(accountsList);
            sourceAccountCombo.setItems(accountsList);
            historyAccountCombo.setItems(accountsList);

            // Setup custom cell factories
            setupAccountComboDisplay(depositAccountCombo);
            setupAccountComboDisplay(withdrawAccountCombo);
            setupAccountComboDisplay(sourceAccountCombo);
            setupAccountComboDisplay(historyAccountCombo);
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Load Accounts Failed", "Unable to load accounts");
        }
    }

    private void setupAccountComboDisplay(ComboBox<Account> comboBox) {
        comboBox.setCellFactory(param -> new ListCell<Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountId() + " (Balance: $" + item.getBalance().toString() + ")");
            }
        });

        comboBox.setButtonCell(new ListCell<Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountId() + " (Balance: $" + item.getBalance().toString() + ")");
            }
        });
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        Account selectedAccount = depositAccountCombo.getValue();
        if (selectedAccount == null) {
            AlertHelper.showErrorAlert("Error", "Deposit Error", "Please select an account");
            return;
        }

        String amountText = depositAmountField.getText().trim();
        if (amountText.isEmpty()) {
            AlertHelper.showErrorAlert("Error", "Deposit Error", "Please enter an amount");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertHelper.showErrorAlert("Error", "Deposit Error", "Amount must be greater than zero");
                return;
            }

            Main app = Main.getInstance();
            String description = "Deposit to account " + selectedAccount.getAccountId();
            Transaction transaction = app.getTransactionService().deposit(
                    selectedAccount.getAccountId(), amount.doubleValue(), description);

            if (transaction != null) {
                AlertHelper.showInformationAlert("Success", "Deposit Successful",
                        String.format("$%s deposited to account %s", amount, selectedAccount.getAccountId()));
                depositAmountField.clear();
                loadAccounts();
            } else {
                AlertHelper.showErrorAlert("Error", "Deposit Error", "Deposit failed. Please try again.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showErrorAlert("Error", "Deposit Error", "Invalid amount format");
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Deposit Error", "An error occurred during deposit");
        }
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        Account selectedAccount = withdrawAccountCombo.getValue();
        if (selectedAccount == null) {
            AlertHelper.showErrorAlert("Error", "Withdrawal Error", "Please select an account");
            return;
        }

        String amountText = withdrawAmountField.getText().trim();
        if (amountText.isEmpty()) {
            AlertHelper.showErrorAlert("Error", "Withdrawal Error", "Please enter an amount");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertHelper.showErrorAlert("Error", "Withdrawal Error", "Amount must be greater than zero");
                return;
            }

            if (amount.compareTo(selectedAccount.getBalance()) > 0) {
                AlertHelper.showErrorAlert("Error", "Withdrawal Error", "Insufficient funds",
                        String.format("Your balance is $%s", selectedAccount.getBalance()));
                return;
            }

            Main app = Main.getInstance();
            String description = "Withdrawal from account " + selectedAccount.getAccountId();
            Transaction transaction = app.getTransactionService().withdrawFunds(
                    selectedAccount.getAccountId(), amount.doubleValue(), description);

            if (transaction != null) {
                AlertHelper.showInformationAlert("Success", "Withdrawal Successful",
                        String.format("$%s withdrawn from account %s", amount, selectedAccount.getAccountId()));
                withdrawAmountField.clear();
                loadAccounts();
            } else {
                AlertHelper.showErrorAlert("Error", "Withdrawal Error", "Withdrawal failed. Please check your balance.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showErrorAlert("Error", "Withdrawal Error", "Invalid amount format");
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Withdrawal Error", "An error occurred during withdrawal");
        }
    }

    @FXML
    private void handleTransfer(ActionEvent event) {
        Account sourceAccount = sourceAccountCombo.getValue();
        String destinationAccountId = destinationAccountField.getText().trim();

        if (sourceAccount == null) {
            AlertHelper.showErrorAlert("Error", "Transfer Error", "Please select a source account");
            return;
        }

        if (destinationAccountId.isEmpty()) {
            AlertHelper.showErrorAlert("Error", "Transfer Error", "Please enter a destination account ID");
            return;
        }

        if (sourceAccount.getAccountId().equals(destinationAccountId)) {
            AlertHelper.showErrorAlert("Error", "Transfer Error", "Source and destination accounts cannot be the same");
            return;
        }

        String amountText = transferAmountField.getText().trim();
        if (amountText.isEmpty()) {
            AlertHelper.showErrorAlert("Error", "Transfer Error", "Please enter an amount");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertHelper.showErrorAlert("Error", "Transfer Error", "Amount must be greater than zero");
                return;
            }

            if (amount.compareTo(sourceAccount.getBalance()) > 0) {
                AlertHelper.showErrorAlert("Error", "Transfer Error", "Insufficient funds",
                        String.format("Your balance is $%s", sourceAccount.getBalance()));
                return;
            }

            String description = transferDescriptionField.getText().trim();
            if (description.isEmpty()) {
                description = String.format("Transfer from %s to %s", sourceAccount.getAccountId(), destinationAccountId);
            }

            Main app = Main.getInstance();
            Transaction transaction = app.getTransactionService().transferFunds(
                    sourceAccount.getAccountId(), destinationAccountId, amount.doubleValue(), description);

            if (transaction != null) {
                AlertHelper.showInformationAlert("Success", "Transfer Successful",
                        String.format("$%s transferred from %s to %s", amount, sourceAccount.getAccountId(), destinationAccountId));
                transferAmountField.clear();
                transferDescriptionField.clear();
                destinationAccountField.clear();
                loadAccounts();
            } else {
                AlertHelper.showErrorAlert("Error", "Transfer Error", "Transfer failed. Please verify the destination account.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showErrorAlert("Error", "Transfer Error", "Invalid amount format");
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Transfer Error", "An error occurred during transfer");
        }
    }

    @FXML
    private void handleViewHistory(ActionEvent event) {
        Account selectedAccount = historyAccountCombo.getValue();
        if (selectedAccount == null) {
            AlertHelper.showErrorAlert("Error", "History Error", "Please select an account");
            return;
        }

        try {
            Main app = Main.getInstance();
            String userId = app.getCurrentCustomer().getUserId();
            List<Transaction> transactions = app.getTransactionService().getTransactionHistory(userId);

            transactionsList.clear();
            if (transactions != null) {
                List<Transaction> filteredTransactions = transactions.stream()
                        .filter(t -> (t.getFromAccountId() != null && t.getFromAccountId().equals(selectedAccount.getAccountId())) ||
                                (t.getToAccountId() != null && t.getToAccountId().equals(selectedAccount.getAccountId())))
                        .collect(Collectors.toList());
                transactionsList.addAll(filteredTransactions);
            }
            transactionTable.setItems(transactionsList);
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "History Error", "Unable to load transaction history");
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
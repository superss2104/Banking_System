package controller;

import Application.Main;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Account;
import model.Customer;
import utils.AlertHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountController {
    private static final Logger logger = Logger.getLogger(AccountController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObservableList<Account> accountsList = FXCollections.observableArrayList();
    @FXML
    private TableView<Account> accountsTable;
    @FXML
    private TableColumn<Account, String> accountIdColumn;
    @FXML
    private TableColumn<Account, BigDecimal> balanceColumn;
    @FXML
    private TableColumn<Account, String> accountTypeColumn;
    @FXML
    private TableColumn<Account, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Account, Boolean> activeColumn;
    @FXML
    private TextField accountDetailsField;
    @FXML
    private ComboBox<String> accountTypeCombo;
    @FXML
    private Button createAccountButton;
    @FXML
    private Button viewDetailsButton;
    @FXML
    private Button updateDetailsButton;
    @FXML
    private Button backButton;
    private Customer currentCustomer;

    @FXML
    private void initialize() {
        // Initialize account type dropdown
        accountTypeCombo.setItems(FXCollections.observableArrayList(
                Account.AccountType.SAVINGS.name(),
                Account.AccountType.CHECKING.name(),
                Account.AccountType.FIXED_DEPOSIT.name()));

        // Configure table columns
        accountIdColumn.setCellValueFactory(new PropertyValueFactory<>("accountId"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        accountTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Format balance column to show currency
        balanceColumn.setCellFactory(column -> new TableCell<Account, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("$%.2f", amount));
            }
        });

        // Format active column to show Yes/No
        activeColumn.setCellFactory(column -> new TableCell<Account, Boolean>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                setText(empty || active == null ? null : (active ? "Yes" : "No"));
            }
        });

        // Format createdAt column
        createdAtColumn.setCellFactory(column -> new TableCell<Account, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FORMATTER));
            }
        });

        // Load accounts
        loadAccounts();
    }

    private void loadAccounts() {
        try {
            Main app = Main.getInstance();
            if (app.getCurrentCustomer() == null) {
                logger.warning("No user logged in");
                AlertHelper.showErrorAlert("Error", "Session Error", "No user logged in");
                Main.getSceneManager().switchToLoginScene();
                return;
            }
            String userId = app.getCurrentCustomer().getUserId();
            List<Account> accounts = app.getAccountService().getAccountsByUserId(userId);

            accountsList.setAll(accounts);
            accountsTable.setItems(accountsList);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading accounts", e);
            AlertHelper.showErrorAlert("Error", "Loading Error", "Error loading accounts: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateAccount(ActionEvent event) {
        try {
            String accountType = accountTypeCombo.getValue();
            String details = accountDetailsField.getText();

            if (accountType == null || accountType.isEmpty()) {
                AlertHelper.showErrorAlert("Error", "Validation Error", "Please select an account type");
                return;
            }

            // Validate account type
            try {
                Account.AccountType.valueOf(accountType);
            } catch (IllegalArgumentException e) {
                AlertHelper.showErrorAlert("Error", "Validation Error", "Invalid account type");
                return;
            }

            Main app = Main.getInstance();
            String userId = app.getCurrentCustomer().getUserId();
            Account newAccount = app.getAccountService().createAccount(userId, accountType, BigDecimal.ZERO);

            if (newAccount != null) {
                if (details != null && !details.isEmpty()) {
                    if (!app.getAccountService().updateAccount(newAccount.getAccountId(), details)) {
                        AlertHelper.showErrorAlert("Error", "Update Error", "Failed to set account details");
                        return;
                    }
                }

                loadAccounts();
                AlertHelper.showInformationAlert("Success", "Account Created",
                        "Your new account has been created successfully.");

                accountTypeCombo.setValue(null);
                accountDetailsField.clear();
            } else {
                AlertHelper.showErrorAlert("Error", "Creation Error", "Failed to create account");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating account", e);
            AlertHelper.showErrorAlert("Error", "Creation Error", "Error creating account: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateDetails(ActionEvent event) {
        Account selectedAccount = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            AlertHelper.showErrorAlert("Error", "Selection Error", "Please select an account to update");
            return;
        }

        String newDetails = accountDetailsField.getText();

        try {
            Main app = Main.getInstance();
            if (app.getAccountService().updateAccount(selectedAccount.getAccountId(), newDetails)) {
                loadAccounts();
                AlertHelper.showInformationAlert("Success", "Update Success",
                        "Account details updated successfully");
                accountDetailsField.clear();
            } else {
                AlertHelper.showErrorAlert("Error", "Update Error", "Failed to update account details");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating account details", e);
            AlertHelper.showErrorAlert("Error", "Update Error", "Error updating account details: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        Account selectedAccount = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            AlertHelper.showErrorAlert("Error", "Selection Error", "Please select an account to view");
            return;
        }

        accountDetailsField.setText("");
        AlertHelper.showInformationAlert("Info", "View Details",
                "Details not available in Account object. Contact support for database details.");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        Main app = Main.getInstance();
        Main.getSceneManager().switchToDashboardScene();
    }

    public void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
        loadAccounts(); // Reload on scene switch
    }
}


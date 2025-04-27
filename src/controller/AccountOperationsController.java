package controller;

import Application.Main;
import Service.AccountService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.Account;
import model.Customer;
import utils.AlertHelper;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AccountOperationsController implements Initializable {

    private static final Logger logger = Logger.getLogger(AccountOperationsController.class.getName());
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button createAccountBtn;
    @FXML
    private Button viewAccountDetailsBtn;
    @FXML
    private Button updateAccountBtn;
    @FXML
    private Button updateBalanceBtn;
    @FXML
    private Button helpBtn;
    @FXML
    private Button backBtn;
    @FXML
    private VBox updateForm;
    @FXML
    private ComboBox<String> accountComboBox;
    @FXML
    private ComboBox<String> updateAccountComboBox;
    @FXML
    private ComboBox<String> accountTypeField;
    @FXML
    private TextField balanceField;
    @FXML
    private ComboBox<String> statusComboBox;
    private Customer currentCustomer;
    private AccountService accountService;
    private Main app;
    private ChangeListener<Boolean> visibilityListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        app = Main.getInstance();
        currentCustomer = app.getCurrentCustomer();
        accountService = app.getAccountService();

        logInitializationStatus("welcomeLabel", welcomeLabel);
        logInitializationStatus("statusLabel", statusLabel);
        logInitializationStatus("createAccountBtn", createAccountBtn);
        logInitializationStatus("viewAccountDetailsBtn", viewAccountDetailsBtn);
        logInitializationStatus("updateAccountBtn", updateAccountBtn);
        logInitializationStatus("updateBalanceBtn", updateBalanceBtn);
        logInitializationStatus("helpBtn", helpBtn);
        logInitializationStatus("backBtn", backBtn);
        logInitializationStatus("updateForm", updateForm);
        logInitializationStatus("accountComboBox", accountComboBox);
        logInitializationStatus("updateAccountComboBox", updateAccountComboBox);
        logInitializationStatus("accountTypeField", accountTypeField);
        logInitializationStatus("balanceField", balanceField);
        logInitializationStatus("statusComboBox", statusComboBox);

        if (currentCustomer != null && accountService != null) {
            welcomeLabel.setText("Welcome, " + (currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "Guest"));
            statusLabel.setText("Ready");
            statusLabel.setMinHeight(100.0);
            statusLabel.applyCss();
            populateAccountList();
            if (accountTypeField != null) {
                accountTypeField.setItems(FXCollections.observableArrayList("Fixed Deposit", "Savings", "Checking"));
                accountTypeField.setValue("Savings");
            }
            if (statusComboBox != null) {
                statusComboBox.setItems(FXCollections.observableArrayList("ACTIVE", "SUSPENDED"));
                statusComboBox.setValue("ACTIVE");
            }
            // Check if a balance refresh is needed
            if (app.needsBalanceRefresh() && app.getLastSelectedAccount() != null) {
                refreshBalance(app.getLastSelectedAccount());
                app.setNeedsBalanceRefresh(false);
            }

            // Add visibility listener to refresh balance when the view becomes visible
            setupVisibilityListener();
        } else {
            statusLabel.setText("Error: No user or account service available.");
            AlertHelper.showWarningAlert("Warning", "Initialization Error", "Please log in or check service availability.");
        }
    }

    private void setupVisibilityListener() {
        // Remove any existing listener to avoid duplicates
        if (visibilityListener != null) {
            updateForm.visibleProperty().removeListener(visibilityListener);
        }

        visibilityListener = (obs, oldValue, newValue) -> {
            if (newValue && app.needsBalanceRefresh() && app.getLastSelectedAccount() != null) {
                Platform.runLater(() -> {
                    refreshBalance(app.getLastSelectedAccount());
                    app.setNeedsBalanceRefresh(false);
                });
            }
        };
        updateForm.visibleProperty().addListener(visibilityListener);
    }

    private void logInitializationStatus(String fieldName, Object field) {
        if (field == null) {
            logger.severe(fieldName + " is null");
        } else {
            logger.info(fieldName + " initialized successfully");
        }
    }

    private void populateAccountList() {
        if (currentCustomer != null && accountService != null) {
            String username = currentCustomer.getUsername();
            List<Account> userAccounts = accountService.getAccountsByUserId(username);
            if (userAccounts != null && !userAccounts.isEmpty()) {
                var userAccountItems = FXCollections.observableArrayList(
                        userAccounts.stream()
                                .map(account -> String.format("%s - %s (Balance: %s, Status: %s)",
                                        account.getAccountId(),
                                        account.getAccountType(),
                                        account.getBalance(),
                                        account.getStatus()))
                                .collect(Collectors.toList())
                );
                if (accountComboBox != null) {
                    accountComboBox.setItems(userAccountItems);
                    String lastSelected = app.getLastSelectedAccount();
                    if (lastSelected != null) {
                        String selectedItem = userAccountItems.stream()
                                .filter(item -> item.startsWith(lastSelected + " - "))
                                .findFirst()
                                .orElse(null);
                        if (selectedItem != null) {
                            accountComboBox.getSelectionModel().select(selectedItem);
                        } else {
                            accountComboBox.getSelectionModel().select(0);
                        }
                    } else {
                        accountComboBox.getSelectionModel().select(0);
                    }
                }
                if (updateAccountComboBox != null) {
                    updateAccountComboBox.setItems(userAccountItems);
                    String lastSelected = app.getLastSelectedAccount();
                    if (lastSelected != null) {
                        String selectedItem = userAccountItems.stream()
                                .filter(item -> item.startsWith(lastSelected + " - "))
                                .findFirst()
                                .orElse(null);
                        if (selectedItem != null) {
                            updateAccountComboBox.getSelectionModel().select(selectedItem);
                        } else {
                            updateAccountComboBox.getSelectionModel().select(0);
                        }
                    } else {
                        updateAccountComboBox.getSelectionModel().select(0);
                    }
                }
                logger.info("Populated ComboBoxes with " + userAccountItems.size() + " accounts for " + username);
            } else {
                if (statusLabel != null) statusLabel.setText("No accounts found for " + username);
                logger.warning("No accounts found for username: " + username);
                if (accountComboBox != null) accountComboBox.setItems(FXCollections.observableArrayList());
                if (updateAccountComboBox != null) updateAccountComboBox.setItems(FXCollections.observableArrayList());
            }
        } else {
            logger.severe("currentCustomer or accountService is null during populateAccountList");
        }
    }

    @FXML
    private void handleCreateAccount() {
        if (currentCustomer != null && accountService != null) {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Savings", FXCollections.observableArrayList(
                    "Fixed Deposit", "Savings", "Checking"
            ));
            dialog.setTitle("Create New Account");
            dialog.setHeaderText("Select Account Type");
            dialog.setContentText("Choose an account type:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(selectedType -> {
                String username = currentCustomer.getUsername();
                Account account = accountService.createAccount(username, selectedType, BigDecimal.ZERO);
                if (account != null) {
                    Platform.runLater(() -> {
                        if (statusLabel != null) {
                            statusLabel.setText("Account created successfully for " + username + " with ID: " + account.getAccountId() + " (Type: " + selectedType + ")");
                        }
                        AlertHelper.showInformationAlert("Success", "Account Created", "New account created with ID: " + account.getAccountId());
                    });
                    populateAccountList();
                } else {
                    Platform.runLater(() -> {
                        if (statusLabel != null) {
                            statusLabel.setText("Failed to create account for " + username);
                        }
                        AlertHelper.showErrorAlert("Error", "Creation Failed", "Could not create account. Check logs.");
                    });
                }
            });
        } else {
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Error: Cannot create account.");
                AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to create an account.");
            });
        }
    }

    @FXML
    private void handleViewAccountDetails() {
        if (currentCustomer != null && accountService != null) {
            String selectedItem = (accountComboBox != null) ? accountComboBox.getSelectionModel().getSelectedItem() : null;
            if (selectedItem == null) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Please select an account from the dropdown.");
                    AlertHelper.showErrorAlert("Error", "Selection Error", "No account selected. Please choose an account from the dropdown.");
                });
                return;
            }
            String accountId = selectedItem.split(" - ")[0];
            app.setLastSelectedAccount(accountId);
            refreshBalance(accountId);
        } else {
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Error: Cannot view account details.");
                AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to view account details.");
            });
        }
    }

    @FXML
    private void handleUpdateAccount() {
        if (currentCustomer != null && accountService != null) {
            String selectedItem = (updateAccountComboBox != null) ? updateAccountComboBox.getSelectionModel().getSelectedItem() : null;
            if (selectedItem == null) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Please select an account to update.");
                    AlertHelper.showErrorAlert("Error", "Selection Error", "No account selected. Please choose an account from the dropdown.");
                });
                return;
            }
            String accountId = selectedItem.split(" - ")[0];
            app.setLastSelectedAccount(accountId);
            Account currentAccount = accountService.getAccountById(accountId);
            if (currentAccount == null) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Account not found for ID: " + accountId);
                    AlertHelper.showErrorAlert("Error", "Account Not Found", "Selected account does not exist.");
                });
                return;
            }

            logger.info("Attempting to update account: " + accountId + ", Current Type: " + currentAccount.getAccountType() + ", Current Status: " + currentAccount.getStatus());

            ChoiceDialog<String> typeDialog = new ChoiceDialog<>(currentAccount.getAccountType(),
                    FXCollections.observableArrayList("Fixed Deposit", "Savings", "Checking"));
            typeDialog.setTitle("Update Account Type");
            typeDialog.setHeaderText("Select New Account Type");
            typeDialog.setContentText("Choose a new account type:");

            Optional<String> typeResult = typeDialog.showAndWait();
            String newAccountType = typeResult.orElse(currentAccount.getAccountType());

            ChoiceDialog<String> statusDialog = new ChoiceDialog<>(currentAccount.getStatus(),
                    FXCollections.observableArrayList("ACTIVE", "SUSPENDED"));
            statusDialog.setTitle("Update Account Status");
            statusDialog.setHeaderText("Select New Status");
            statusDialog.setContentText("Choose a new status:");

            Optional<String> statusResult = statusDialog.showAndWait();
            String newStatus = statusResult.orElse(currentAccount.getStatus());

            logger.info("New Type: " + newAccountType + ", New Status: " + newStatus);

            boolean typeUpdated = accountService.updateAccountType(accountId, newAccountType);
            boolean statusUpdated = accountService.updateAccountStatus(accountId, newStatus, newStatus.equals("ACTIVE"));

            if (typeUpdated && statusUpdated) {
                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("Account " + accountId + " updated successfully. Type: " + newAccountType + ", Status: " + newStatus);
                    }
                    AlertHelper.showInformationAlert("Success", "Update Successful", "Account updated with new type and status.");
                });
                populateAccountList();
                refreshBalance(accountId);
            } else {
                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("Failed to update account " + accountId + ". Check logs.");
                    }
                    AlertHelper.showErrorAlert("Error", "Update Failed", "Could not update account. Check logs.");
                });
            }
        } else {
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Error: Cannot update account.");
                AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to update an account.");
            });
        }
    }

    @FXML
    private void handleUpdateBalance() {
        if (currentCustomer != null && accountService != null) {
            String selectedItem = (updateAccountComboBox != null) ? updateAccountComboBox.getSelectionModel().getSelectedItem() : null;
            if (selectedItem == null) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Please select an account to update the balance.");
                    AlertHelper.showErrorAlert("Error", "Selection Error", "No account selected. Please choose an account from the dropdown.");
                });
                return;
            }
            String accountId = selectedItem.split(" - ")[0];
            app.setLastSelectedAccount(accountId);

            BigDecimal newBalance;
            try {
                newBalance = new BigDecimal(balanceField != null ? balanceField.getText().trim() : "0");
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new NumberFormatException("Negative balance not allowed");
                }
            } catch (NumberFormatException e) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Please enter a valid positive balance.");
                    AlertHelper.showErrorAlert("Error", "Invalid Input", "Balance must be a positive number.");
                });
                return;
            }

            boolean success = accountService.updateAccount(accountId, newBalance);
            if (success) {
                Platform.runLater(() -> {
                    if (statusLabel != null)
                        statusLabel.setText("Balance for account " + accountId + " updated successfully.");
                    AlertHelper.showInformationAlert("Success", "Balance Updated", "Account balance updated.");
                });
                populateAccountList();
                if (balanceField != null) balanceField.clear();
                refreshBalance(accountId);
            } else {
                Platform.runLater(() -> {
                    if (statusLabel != null)
                        statusLabel.setText("Failed to update balance for account " + accountId + ". Check logs.");
                    AlertHelper.showErrorAlert("Error", "Update Failed", "Could not update balance. Check logs.");
                });
            }
        } else {
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Error: Cannot update account balance.");
                AlertHelper.showErrorAlert("Error", "Access Denied", "Please log in to update account balance.");
            });
        }
    }

    @FXML
    private void handleHelp() {
        Platform.runLater(() -> {
            AlertHelper.showInformationAlert("Help", "Account Management Help",
                    "Contact support at cs24b043@iittp.ac.in / cs24b041@iittp.ac.in for assistance with account operations.");
            if (statusLabel != null) statusLabel.setText("Help displayed.");
        });
    }

    @FXML
    private void handleBackToMain() {
        if (app != null && app.getSceneManager() != null) {
            app.getSceneManager().switchToDashboardScene();
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Returned to Dashboard.");
            });
        } else {
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Error: Cannot return to dashboard.");
                AlertHelper.showErrorAlert("Error", "Navigation Error", "Unable to return to dashboard.");
            });
        }
    }

    private void refreshBalance(String accountId) {
        Platform.runLater(() -> {
            Account account = accountService.getAccountById(accountId);
            if (account != null) {
                String details = String.format(
                        "Details for selected account:\n" +
                                "Account ID: %s\n" +
                                "Username: %s\n" +
                                "Type: %s\n" +
                                "Balance: %s\n" +
                                "Created: %s\n" +
                                "Status: %s\n" +
                                "Last Updated: %s",
                        account.getAccountId(),
                        account.getUsername(),
                        account.getAccountType(),
                        account.getBalance(),
                        account.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        account.getStatus(),
                        account.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                statusLabel.setText(details);
                logger.info("Refreshed balance for account: " + accountId + " with balance: " + account.getBalance());
                populateAccountList();
            } else {
                statusLabel.setText("Account not found for ID: " + accountId);
            }
        });
    }
}
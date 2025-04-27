package controller;

import Application.Main;
import Service.AdminService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.AlertHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController {
    @FXML
    private TextField usernameField, passwordField, firstNameField, lastNameField, emailField, phoneField;
    @FXML
    private ComboBox<String> roleComboBox, reportTypeCombo;
    @FXML
    private DatePicker startDatePicker, endDatePicker;
    @FXML
    private TextArea systemLogsArea, reportResultArea;
    @FXML
    private ListView<String> accountListView;
    private AdminService adminService;

    @FXML
    private void initialize() {
        Main app = Main.getInstance();
        adminService = app.getAdminService();
        roleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
        roleComboBox.setValue("USER");
        reportTypeCombo.setItems(FXCollections.observableArrayList(
                "Financial Summary", "User Activity", "Account Status"));
        reportTypeCombo.setValue("Financial Summary");
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        loadAccountList();
    }

    private void loadAccountList() {
        List<String> accounts = adminService.getAllAccounts().stream()
                .map(accountId -> accountId + " - Status: " + adminService.getAccountStatus(accountId))
                .collect(Collectors.toList());
        accountListView.setItems(FXCollections.observableArrayList(accounts));
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                email.isEmpty() || phone.isEmpty() || role == null) {
            AlertHelper.showErrorAlert("Error", "Input Error", "All fields are required.");
            return;
        }

        boolean success = adminService.addCustomer(username, password, firstName, lastName, email, phone, role);
        if (success) {
            AlertHelper.showInformationAlert("Success", "User Added", "User " + username + " has been added successfully.");
            clearUserFields();
        } else {
            AlertHelper.showErrorAlert("Error", "Add Failed", "Failed to add user. Check logs for details or ensure username is unique.");
        }
    }

    @FXML
    private void handleUpdateUser(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                email.isEmpty() || phone.isEmpty() || role == null) {
            AlertHelper.showErrorAlert("Error", "Input Error", "All fields are required.");
            return;
        }

        boolean success = adminService.updateCustomer(username, password, firstName, lastName, email, phone, role);
        if (success) {
            AlertHelper.showInformationAlert("Success", "User Updated", "User " + username + " has been updated successfully.");
            clearUserFields();
        } else {
            AlertHelper.showErrorAlert("Error", "Update Failed", "Failed to update user. Check logs for details or ensure username exists.");
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            AlertHelper.showErrorAlert("Error", "Input Error", "Username is required to delete a user.");
            return;
        }

        boolean success = adminService.deleteCustomer(username);
        if (success) {
            AlertHelper.showInformationAlert("Success", "User Deleted", "User " + username + " has been deleted successfully.");
            clearUserFields();
        } else {
            AlertHelper.showErrorAlert("Error", "Delete Failed", "Failed to delete user. Check logs for details or ensure username exists.");
        }
    }

    @FXML
    private void handleRefreshLogs(ActionEvent event) {
        systemLogsArea.setText("System logs not available (refresh placeholder).");
        AlertHelper.showInformationAlert("Success", "Logs Refreshed", "System logs have been refreshed (placeholder).");
    }

    @FXML
    private void handleGenerateReport(ActionEvent event) {
        String reportType = reportTypeCombo.getValue();
        LocalDateTime start = startDatePicker.getValue().atStartOfDay();
        LocalDateTime end = endDatePicker.getValue().atTime(23, 59, 59);

        String report;
        switch (reportType) {
            case "Financial Summary":
                report = adminService.getSystemStats(start, end);
                break;
            case "User Activity":
                report = adminService.getUserActivityReport(start, end);
                break;
            case "Account Status":
                report = adminService.getAccountStatusReport(start, end);
                break;
            default:
                report = "Unknown report type.";
        }

        reportResultArea.setText(report);
        AlertHelper.showInformationAlert("Success", "Report Generated", "Report has been generated and displayed.");
    }

    @FXML
    private void handleSuspendAccount(ActionEvent event) {
        String selectedItem = accountListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertHelper.showErrorAlert("Error", "Selection Error", "Please select an account from the list.");
            return;
        }

        String accountId = selectedItem.split(" - ")[0]; // Extract accountId from the selected item
        boolean success = adminService.suspendAccount(accountId);
        if (success) {
            AlertHelper.showInformationAlert("Success", "Account Suspended", "Account " + accountId + " has been suspended.");
            loadAccountList(); // Refresh the list to reflect the new status
        } else {
            AlertHelper.showErrorAlert("Error", "Suspend Failed", "Failed to suspend account. Check logs for details or ensure account exists.");
        }
    }

    @FXML
    private void handleReactivateAccount(ActionEvent event) {
        String selectedItem = accountListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertHelper.showErrorAlert("Error", "Selection Error", "Please select an account from the list.");
            return;
        }

        String accountId = selectedItem.split(" - ")[0]; // Extract accountId from the selected item
        boolean success = adminService.reactivateAccount(accountId);
        if (success) {
            AlertHelper.showInformationAlert("Success", "Account Reactivated", "Account " + accountId + " has been reactivated.");
            loadAccountList(); // Refresh the list to reflect the new status
        } else {
            AlertHelper.showErrorAlert("Error", "Reactivate Failed", "Failed to reactivate account. Check logs for details or ensure account exists.");
        }
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            Parent dashboardView = FXMLLoader.load(getClass().getResource("/view/AdminDashboardView.fxml"));
            Scene dashboardScene = new Scene(dashboardView);
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            dashboardScene.getStylesheets().add(getClass().getResource("/Styles/base.css").toExternalForm());
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Admin Dashboard - Banking System");
            currentStage.show();
        } catch (IOException e) {
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Failed to load dashboard view", e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Scene loginScene = new Scene(loginView);
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            loginScene.getStylesheets().add(getClass().getResource("/Styles/base.css").toExternalForm());
            currentStage.setScene(loginScene);
            currentStage.setTitle("Login - Banking System");
            currentStage.show();
        } catch (IOException e) {
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Failed to load login view", e.getMessage());
        }
    }

    private void clearUserFields() {
        usernameField.clear();
        passwordField.clear();
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
    }
}
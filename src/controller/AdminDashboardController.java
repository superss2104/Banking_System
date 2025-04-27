package controller;

import Application.Main;
import Service.AdminService;
import Service.ReportService;
import Service.SystemMonitorService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.Customer;
import model.Transaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import utils.AlertHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private ListView<String> usersListView;
    @FXML
    private ListView<String> systemLogsListView;
    @FXML
    private ListView<String> reportsListView;
    @FXML
    private ListView<String> accountsListView;

    private AdminService adminService;
    private SystemMonitorService monitorService;
    private ReportService reportService;

    @FXML
    public void initialize() {
        Main app = Main.getInstance();
        adminService = app.getAdminService();
        reportService = app.getReportService();
        try {
            monitorService = SystemMonitorService.getInstance();
        } catch (SQLException e) {
            AlertHelper.showErrorAlert("Error", "Initialization Error", "Failed to initialize system monitor service", e.getMessage());
            return;
        }
        welcomeLabel.setText("Welcome, Administrator");

        loadUsers();
        loadSystemLogs();
        loadReports();
        loadAccounts();
    }

    private void loadUsers() {
        List<Customer> customers = adminService.getAllCustomers();
        List<String> userSummaries = customers.stream()
                .map(c -> c.getUsername() + " - " + c.getFirstName() + " " + c.getLastName())
                .toList();
        usersListView.setItems(FXCollections.observableArrayList(userSummaries));
    }

    private void loadSystemLogs() {
        try {
            List<String> logs = monitorService.getSystemEvents().stream()
                    .map(event -> String.format("%s [%s] %s",
                            event.getOrDefault("timestamp", LocalDate.now()).toString(),
                            event.getOrDefault("type", "UNKNOWN"),
                            event.getOrDefault("message", "No message")))
                    .collect(Collectors.toList());
            systemLogsListView.setItems(FXCollections.observableArrayList(logs.isEmpty() ? List.of("System logs not available") : logs));
        } catch (Exception e) {
            systemLogsListView.setItems(FXCollections.observableArrayList(List.of("Error loading logs: " + e.getMessage())));
        }
    }

    private void loadReports() {
        List<String> reports = List.of(
                "Financial Summary - Last 30 Days",
                "User Activity - Last 30 Days",
                "Account Status - Current"
        );
        reportsListView.setItems(FXCollections.observableArrayList(reports));
    }

    private void loadAccounts() {
        List<String> accounts = adminService.getAllAccounts().stream()
                .map(a -> "Account ID: " + a + " - Status: " + adminService.getAccountStatus(a))
                .toList();
        accountsListView.setItems(FXCollections.observableArrayList(accounts));
    }

    @FXML
    private void handleLogout() {
        try {
            URL resource = getClass().getResource("/view/LoginView.fxml");
            if (resource == null) {
                throw new IOException("LoginView.fxml resource not found");
            }
            Parent loginView = FXMLLoader.load(resource);
            Scene loginScene = new Scene(loginView);
            Stage currentStage = (Stage) welcomeLabel.getScene().getWindow();
            URL cssResource = getClass().getResource("/Styles/base.css");
            if (cssResource != null) {
                loginScene.getStylesheets().add(cssResource.toExternalForm());
            }
            currentStage.setScene(loginScene);
            currentStage.setTitle("Login - Banking System");
            currentStage.show();
        } catch (IOException e) {
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Failed to load login view", e.getMessage());
        }
    }

    @FXML
    private void handleAddUser() {
        navigateToAdminView();
    }

    @FXML
    private void handleUpdateUser() {
        navigateToAdminView();
    }

    @FXML
    private void handleDeleteUser() {
        navigateToAdminView();
    }

    @FXML
    private void handleAssignRole() {
        navigateToAdminView();
    }

    @FXML
    private void handleRefreshLogs() {
        loadSystemLogs();
        AlertHelper.showInformationAlert("Success", "Logs Refreshed", "System logs have been updated (placeholder).");
    }

    @FXML
    private void handleViewSystemStatistics() {
        String stats = adminService.getSystemStats();
        AlertHelper.showInformationAlert("System Statistics", "Overview", stats);
    }

    @FXML
    private void handleTransactionReport() {
        AlertHelper.showInformationAlert("Report", "Financial Summary", "Report generation not available.");
    }

    @FXML
    private void handleUserActivityReport() {
        AlertHelper.showInformationAlert("Report", "User Activity", "Report generation not available.");
    }

    @FXML
    private void handleAccountStatusReport() {
        AlertHelper.showInformationAlert("Report", "Account Status", "Report generation not available.");
    }

    @FXML
    private void handleExportPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Transaction Report PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            Window window = welcomeLabel.getScene().getWindow();
            File file = fileChooser.showSaveDialog(window);

            if (file == null) {
                return;
            }

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            ReportService.TransactionReport report = reportService.generateTransactionReport(startDate, endDate);

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.setFont(PDType1Font.HELVETICA, 14);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText("Banking System - Transaction Report");
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.showText("Period: " + startDate + " to " + endDate);
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Total Deposits: $" + report.getTotalDeposits());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Total Withdrawals: $" + report.getTotalWithdrawals());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Total Transactions: " + report.getTransactions().size());
                    contentStream.endText();

                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, 650);
                    contentStream.showText("ID          Type        Amount      Date");
                    contentStream.endText();

                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, 630);
                    List<Transaction> transactions = report.getTransactions();
                    for (int i = 0; i < transactions.size() && i < 40; i++) {
                        Transaction t = transactions.get(i);
                        String line = String.format("%-12s %-12s $%-10.2f %s",
                                t.getTransactionId().substring(0, Math.min(12, t.getTransactionId().length())),
                                t.getType(),
                                t.getAmount().doubleValue(),
                                t.getTimestamp().toLocalDate());
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -15);
                    }
                    contentStream.endText();
                }

                document.save(file);
            }

            AlertHelper.showInformationAlert("Success", "PDF Export", "Transaction report exported to " + file.getAbsolutePath());
        } catch (IOException e) {
            AlertHelper.showErrorAlert("Error", "PDF Export Error", "Failed to generate or save PDF", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Unexpected Error", "An unexpected error occurred", e.getMessage());
        }
    }

    @FXML
    private void handleExportCSV() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Transaction Report CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            Window window = welcomeLabel.getScene().getWindow();
            File file = fileChooser.showSaveDialog(window);

            if (file == null) {
                return; // User canceled
            }

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            ReportService.TransactionReport report = reportService.generateTransactionReport(startDate, endDate);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Write header
                writer.write("Transaction ID,Type,Amount,Date");
                writer.newLine();

                // Write summary
                writer.write("Total Deposits,$" + report.getTotalDeposits());
                writer.newLine();
                writer.write("Total Withdrawals,$" + report.getTotalWithdrawals());
                writer.newLine();
                writer.write("Total Transactions," + report.getTransactions().size());
                writer.newLine();
                writer.newLine();

                // Write transaction details
                List<Transaction> transactions = report.getTransactions();
                for (Transaction t : transactions) {
                    String line = String.format("%s,%s,$%.2f,%s",
                            t.getTransactionId(),
                            t.getType(),
                            t.getAmount().doubleValue(),
                            t.getTimestamp().toLocalDate());
                    writer.write(line);
                    writer.newLine();
                }
            }

            AlertHelper.showInformationAlert("Success", "CSV Export", "Transaction report exported to " + file.getAbsolutePath());
        } catch (IOException e) {
            AlertHelper.showErrorAlert("Error", "CSV Export Error", "Failed to generate or save CSV", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Unexpected Error", "An unexpected error occurred", e.getMessage());
        }
    }

    @FXML
    private void handleViewAllAccounts() {
        loadAccounts();
        AlertHelper.showInformationAlert("Accounts", "All Accounts", "Account list has been refreshed");
    }

    @FXML
    private void handleSuspendAccount() {
        navigateToAdminView();
    }

    @FXML
    private void handleReactivateAccount() {
        navigateToAdminView();
    }

    private void navigateToAdminView() {
        try {
            URL resource = getClass().getResource("/view/AdminView.fxml");
            if (resource == null) {
                throw new IOException("AdminView.fxml resource not found");
            }
            Parent adminView = FXMLLoader.load(resource);
            Scene adminScene = new Scene(adminView);
            Stage currentStage = (Stage) welcomeLabel.getScene().getWindow();
            URL cssResource = getClass().getResource("/Styles/base.css");
            if (cssResource != null) {
                adminScene.getStylesheets().add(cssResource.toExternalForm());
            }
            currentStage.setScene(adminScene);
            currentStage.setTitle("Admin Management - Banking System");
            currentStage.show();
        } catch (IOException e) {
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Failed to load admin view", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showErrorAlert("Error", "Navigation Error", "Failed to load admin view: Invalid identifier or configuration issue", e.getMessage());
        }
    }
}
package Application;

import Service.*;
import controller.RegisterController;
import database.DatabaseConnector;
import database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.Customer;
import utils.AuditLogger;
import utils.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static SceneManager sceneManager;
    private static Main instance;
    private final AuditLogger auditLogger = new AuditLogger();
    private AuthService authService;
    private AccountService accountService;
    private TransactionService transactionService;
    private UPIService upiService;
    private ReportService reportService;
    private AdminService adminService;
    private CustomerService customerService;
    private Customer currentCustomer;
    private boolean isAdmin = false;
    private Stage primaryStage;
    private String lastSelectedAccountId;
    private boolean needsBalanceRefresh;

    public Main() throws SQLException {
    }

    public static Main getInstance() {
        return instance;
    }

    public static SceneManager getSceneManager() {
        return sceneManager;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;

        try {
            initializeDatabase();
            initializeServices();
            sceneManager = new SceneManager(primaryStage);
            showLoginScreen();
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setTitle("Banking System");

            // Set the taskbar/window icon
            primaryStage.getIcons().add(new Image(getClass().getResource("/resources/app-icon.png").toString()));




            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();

            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize application", e);
            showErrorAndExit("Application Initialization Error", "Failed to initialize the application: " + e.getMessage());
        }
    }

    private void initializeDatabase() {
        try {
            DatabaseInitializer initializer = new DatabaseInitializer(DatabaseConnector.getConnection());
            initializer.initializeDatabase();
            logger.info("Database initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
            showErrorAndExit("Database Initialization Error", "Failed to initialize database: " + e.getMessage());
        }
    }

    private void initializeServices() {
        try {
            authService = new AuthService();
            accountService = new AccountService();
            transactionService = new TransactionService();
            upiService = new UPIService();
            reportService = new ReportService();
            adminService = new AdminService();
            customerService = new CustomerService();
            logger.info("All services initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize services", e);
            throw new RuntimeException("Service initialization failed", e);
        }
    }

    private void showLoginScreen() {
        sceneManager.switchToLoginScene();
    }

    public void loadRegisterScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RegisterView.fxml"));
            Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.setAuthService(authService);
            this.primaryStage.setScene(new Scene(root));
            this.primaryStage.setFullScreen(true);
            this.primaryStage.setFullScreenExitHint("");
            this.primaryStage.setTitle("Banking System - Register");
            primaryStage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load register scene", e);
            showErrorAndExit("Scene Loading Error", "Failed to load register scene: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        System.exit(1);
    }

    public void shutdown() {
        try {
            DatabaseConnector.closeConnection();
            auditLogger.log(AuditLogger.AuditEventType.APPLICATION_SHUTDOWN, "Application shutdown");
            logger.info("Application shutdown gracefully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during application shutdown", e);
        }
    }

    public boolean login(String username, String password) {
        try {
            currentCustomer = authService.login(username, password);

            if (currentCustomer != null) {
                isAdmin = authService.isAdmin(currentCustomer);
                auditLogger.log(AuditLogger.AuditEventType.USER_LOGIN, "User logged in: " + username);
                auditLogger.log(AuditLogger.AuditEventType.LOGIN, "User logged in successfully: " + username);
                return true;
            } else {
                auditLogger.log(AuditLogger.AuditEventType.LOGIN_FAILED, "Failed login attempt: " + username);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Login error", e);
            auditLogger.log(AuditLogger.AuditEventType.LOGIN_FAILED, "Failed login attempt: " + username);
            return false;
        }
    }

    public boolean register(String username, String password, String firstName,
                            String lastName, String email, String phone) {
        try {
            Customer customer = authService.register(username, password, firstName, lastName, email, phone);

            if (customer != null) {
                auditLogger.log(AuditLogger.AuditEventType.NEW_USER, "New user registered: " + username);
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Registration error", e);
            auditLogger.log(AuditLogger.AuditEventType.REGISTRATION_FAILED, "Failed registration attempt: " + username);
        }

        return false;
    }

    public void logout() {
        if (currentCustomer != null) {
            String username = currentCustomer.getUsername();
            auditLogger.log(AuditLogger.AuditEventType.LOGOUT, "User logged out: " + username);

            currentCustomer = null;
            isAdmin = false;
            sceneManager.switchToLoginScene();
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");

            logger.info("User logged out: " + username);
        }
    }

    public Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public UPIService getUpiService() {
        return upiService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public Object getLogger() {
        return logger;
    }

    public void setLastSelectedAccount(String accountId) {
        this.lastSelectedAccountId = accountId;
    }

    public String getLastSelectedAccount() {
        return lastSelectedAccountId;
    }

    public void setNeedsBalanceRefresh(boolean needsRefresh) {
        this.needsBalanceRefresh = needsRefresh;
    }

    public boolean needsBalanceRefresh() {
        return needsBalanceRefresh;
    }

    public void requestBalanceRefresh(String accountId) {
        this.lastSelectedAccountId = accountId;
        this.needsBalanceRefresh = true;
    }
}
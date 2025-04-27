package utils;

import Application.Main;
import controller.RegisterController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Customer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneManager {
    private static final Logger logger = Logger.getLogger(SceneManager.class.getName());

    private final Stage primaryStage;
    private Scene currentScene;

    public SceneManager(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchScene(String fxmlPath, String title) {
        try {
            String fullPath = "/view/" + fxmlPath;
            logger.info("Loading FXML from: " + fullPath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fullPath));
            if (loader.getLocation() == null) {
                throw new IOException("FXML resource not found: " + fullPath);
            }
            Parent root = loader.load();

            final Parent finalRoot = root;
            Platform.runLater(() -> {
                try {
                    currentScene = new Scene(finalRoot);
                    primaryStage.setScene(currentScene);
                    primaryStage.setTitle(title);
                    primaryStage.setFullScreen(true); // Ensure full-screen mode
                    primaryStage.setFullScreenExitHint(""); // Prevent exit hint
                    primaryStage.show();
                    logger.info("Successfully switched to scene: " + title);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error setting scene on FX thread: " + fxmlPath, e);
                    showErrorAlertAndFallback("Scene Error", "Failed to set scene: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error switching to scene: " + fxmlPath, e);
            showErrorAlertAndFallback("Scene Loading Error", "Failed to load scene: " + fxmlPath + ". Error: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error switching to scene: " + fxmlPath, e);
            showErrorAlertAndFallback("Unexpected Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    public void switchToLoginScene() {
        switchScene("LoginView.fxml", "Banking System - Login");
    }

    public void switchToRegisterScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RegisterView.fxml"));
            Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.setAuthService(Main.getInstance().getAuthService());
            Platform.runLater(() -> {
                try {
                    currentScene = new Scene(root);
                    primaryStage.setScene(currentScene);
                    primaryStage.setTitle("Banking System - Register");
                    primaryStage.setFullScreen(true); // Ensure full-screen mode
                    primaryStage.setFullScreenExitHint(""); // Prevent exit hint
                    primaryStage.show();
                    logger.info("Successfully switched to scene: Banking System - Register");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error setting register scene on FX thread", e);
                    showErrorAlertAndFallback("Scene Loading Error", "Failed to load register scene: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error switching to register scene", e);
            showErrorAlertAndFallback("Scene Loading Error", "Failed to load register scene: " + e.getMessage());
        }
    }

    public void switchToDashboardScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to dashboard with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("DashboardView.fxml", "Banking System - Customer Dashboard");
    }

    public void switchToCustomerDashboardScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to customer dashboard with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("CustomerDashboardView.fxml", "Banking System - Customer Dashboard");
    }

    public void switchToAdminDashboardScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null || !Main.getInstance().isAdmin()) {
            logger.warning("Attempted to switch to admin dashboard without admin privileges");
            switchToLoginScene();
            return;
        }
        switchScene("AdminDashboardView.fxml", "Banking System - Admin Dashboard");
    }

    public void switchToAdminViewScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null || !Main.getInstance().isAdmin()) {
            logger.warning("Attempted to switch to admin view without admin privileges");
            switchToLoginScene();
            return;
        }
        switchScene("AdminView.fxml", "Banking System - Admin Dashboard");
    }

    public void switchToAccountOperationsScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to account operations with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("AccountOperationsView.fxml", "Banking System - Account Operations");
    }

    public void switchToAccountViewScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to account view with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("AccountView.fxml", "Banking System - Account Management");
    }

    public void switchToTransactionOperationsScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to transaction operations with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("TransactionOperationsView.fxml", "Banking System - Transaction Operations");
    }

    public void switchToTransactionViewScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to transaction view with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("TransactionView.fxml", "Banking System - Transactions");
    }

    public void switchToProfileViewScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to profile view with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("ProfileView.fxml", "Banking System - Profile");
    }

    public void switchToUserProfileViewScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to user profile view with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("UserProfileView.fxml", "Banking System - User Profile");
    }

    public void switchToUpiServicesScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to UPI services with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("UPIServicesView.fxml", "Banking System - UPI Services");
    }

    public void switchToUpiViewScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to UPI view with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("UpiView.fxml", "Banking System - UPI Services");
    }

    public void switchToSystemMonitorScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null || !Main.getInstance().isAdmin()) {
            logger.warning("Attempted to switch to system monitor without admin privileges");
            switchToLoginScene();
            return;
        }
        switchScene("SystemMonitorView.fxml", "Banking System - System Monitor");
    }

    public void switchToAccountManagementScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to account management with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("AccountManagement.fxml", "Banking System - Account Management");
    }

    public void switchToAccountScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to account scene with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("AccountManagement.fxml", "Banking System - Account Management"); // Maps to account management
    }

    public void switchToTransactionScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to transaction scene with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("TransactionOperationsView.fxml", "Banking System - Transaction Operations"); // Maps to transaction operations
    }

    public void switchToProfileScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to profile scene with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("ProfileView.fxml", "Banking System - Profile"); // Maps to profile view
    }

    public void switchToUpiScene() {
        Customer currentCustomer = Main.getInstance().getCurrentCustomer();
        if (currentCustomer == null) {
            logger.warning("Attempted to switch to UPI scene with no logged-in customer");
            switchToLoginScene();
            return;
        }
        switchScene("UPIServicesView.fxml", "Banking System - UPI Services"); // Maps to UPI services
    }

    private void showErrorAlertAndFallback(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            switchToLoginScene();
        });
    }
}
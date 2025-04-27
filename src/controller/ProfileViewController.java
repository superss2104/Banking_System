package controller;

import Application.Main;
import Service.CustomerService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Customer;
import utils.SceneManager;

import java.time.format.DateTimeFormatter;

public class ProfileViewController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label userIdLabel;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label firstNameLabel;
    @FXML
    private Label lastNameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Label createdAtLabel;
    @FXML
    private Label statusLabel;

    private Customer currentCustomer;
    private CustomerService customerService;
    private SceneManager sceneManager;

    @FXML
    public void initialize() {
        Main app = Main.getInstance();
        customerService = app.getCustomerService();
        sceneManager = Main.getSceneManager();
        currentCustomer = app.getCurrentCustomer();

        if (currentCustomer != null) {
            loadCustomerProfile();
        } else {
            handleNotLoggedIn();
        }
    }

    private void loadCustomerProfile() {
        welcomeLabel.setText("Welcome, " + currentCustomer.getFirstName());
        userIdLabel.setText(currentCustomer.getUserId());
        usernameLabel.setText(currentCustomer.getUsername());
        firstNameLabel.setText(currentCustomer.getFirstName());
        lastNameLabel.setText(currentCustomer.getLastName());
        emailLabel.setText(currentCustomer.getEmail());
        phoneLabel.setText(currentCustomer.getPhone());
        addressLabel.setText("N/A"); // Address not available in Customer

        // Handle createdAt
        if (currentCustomer.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            createdAtLabel.setText(currentCustomer.getCreatedAt().format(formatter));
        } else {
            createdAtLabel.setText("N/A");
        }

        statusLabel.setText("Profile loaded successfully");
    }

    private void handleNotLoggedIn() {
        welcomeLabel.setText("Welcome, Guest");
        statusLabel.setText("Please log in to view profile");

        userIdLabel.setText("");
        usernameLabel.setText("");
        firstNameLabel.setText("");
        lastNameLabel.setText("");
        emailLabel.setText("");
        phoneLabel.setText("");
        addressLabel.setText("");
        createdAtLabel.setText("");
    }

    @FXML
    private void handleBackToMain() {
        sceneManager.switchToDashboardScene();
    }
}
package Service;

import database.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Account;
import model.Customer;
import utils.AuditLogger;
import utils.AuditLogger.AuditEventType;
import utils.EncryptionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerService {
    private static final Logger logger = Logger.getLogger(CustomerService.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final AuditLogger auditLogger;
    private final AccountService accountService;

    public CustomerService() throws SQLException {
        this.auditLogger = AuditLogger.getInstance();
        this.accountService = new AccountService();
    }

    public Customer getCustomerByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot get customer with null or empty username");
            return null;
        }

        String sql = "SELECT username, password, first_name, last_name, email, phone, role, created_at FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving customer by username", e);
        }

        return null;
    }

    public boolean isUsernameTaken(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot check if null or empty username is taken");
            return false;
        }

        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking if username is taken", e);
        }

        return false;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();

        String sql = "SELECT username, password, first_name, last_name, email, phone, role, created_at FROM users ORDER BY last_name, first_name";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                customers.add(customer);
            }

            logger.info("Retrieved " + customers.size() + " customers");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving all customers", e);
        }

        return customers;
    }

    public ObservableList<Customer> getAllCustomersObservable() {
        return FXCollections.observableArrayList(getAllCustomers());
    }

    public boolean updateCustomer(String username, String firstName, String lastName, String email, String phone) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot update customer with null or empty username");
            return false;
        }

        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, phone = ? WHERE username = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, username);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Customer updated successfully: " + username);
                auditLogger.logEvent(AuditEventType.ACCOUNT_UPDATE, "Customer updated: " + username);
                return true;
            } else {
                logger.warning("No customer found with username: " + username);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating customer", e);
            return false;
        }
    }

    public boolean deleteCustomer(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot delete customer with null or empty username");
            return false;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Delete associated accounts
                List<Account> accounts = accountService.getAccountsByUsername(username);
                for (Account account : accounts) {
                    accountService.deleteAccount(account.getAccountId());
                }

                // Delete the customer
                String sql = "DELETE FROM users WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        conn.commit();
                        logger.info("Customer deleted successfully: " + username);
                        auditLogger.logEvent(AuditEventType.ACCOUNT_DELETION, "Customer deleted: " + username);
                        return true;
                    } else {
                        conn.rollback();
                        logger.warning("No customer found with username: " + username);
                        return false;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Error deleting customer", e);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting customer", e);
            return false;
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || username.trim().isEmpty() ||
                oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
            logger.warning("Cannot change password with null or empty parameters");
            return false;
        }

        Customer customer = getCustomerByUsername(username);
        if (customer == null) {
            logger.warning("No customer found with username: " + username);
            return false;
        }

        // Verify old password
        if (!EncryptionUtil.verifyPassword(oldPassword, customer.getPassword())) {
            logger.warning("Old password verification failed for customer: " + username);
            auditLogger.logEvent(AuditEventType.UNAUTHORIZED_ACCESS,
                    "Password change attempt with incorrect old password for: " + username);
            return false;
        }

        String hashedNewPassword = EncryptionUtil.hashPassword(newPassword);

        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hashedNewPassword);
            stmt.setString(2, username);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Password changed successfully for customer: " + username);
                auditLogger.logEvent(AuditEventType.ACCOUNT_UPDATE,
                        "Password changed for: " + username);
                return true;
            } else {
                logger.warning("No customer found with username: " + username);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error changing password", e);
            return false;
        }
    }

    public List<Account> getCustomerAccounts(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot get accounts for null or empty username");
            return new ArrayList<>();
        }

        return accountService.getAccountsByUsername(username);
    }

    public void logout(Customer customer) {
        if (customer == null) {
            logger.warning("Cannot log out null customer");
            return;
        }

        logger.info("Customer logged out: " + customer.getUsername());
        auditLogger.logEvent(AuditEventType.LOGOUT, "User logged out: " + customer.getUsername());
    }

    public String viewProfile(Customer customer) {
        if (customer == null) {
            logger.warning("Cannot view profile for null customer");
            return "No customer selected";
        }

        String profile = "User Profile: " + customer.getUsername() + "\n" +
                "---------------------------\n" +
                "Name: " + customer.getFirstName() + " " + customer.getLastName() + "\n" +
                "Email: " + customer.getEmail() + "\n" +
                "Phone: " + customer.getPhone() + "\n" +
                "Role: " + customer.getRole() + "\n";

        logger.info("Profile viewed for customer: " + customer.getUsername());

        return profile;
    }
}
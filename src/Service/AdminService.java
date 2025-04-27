package Service;

import database.DatabaseConnector;
import model.Customer;
import utils.EncryptionUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminService {
    private static final Logger logger = Logger.getLogger(AdminService.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT username, first_name, last_name, email, phone, role, created_at FROM users WHERE role = 'USER'";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getString("username"),
                        null,
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                customers.add(customer);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving customers", e);
        }
        return customers;
    }

    public List<String> getAllAccounts() {
        List<String> accounts = new ArrayList<>();
        String sql = "SELECT account_id FROM accounts";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                accounts.add(rs.getString("account_id"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving accounts", e);
        }
        return accounts;
    }

    public String getAccountStatus(String accountId) {
        String sql = "SELECT status FROM accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving account status", e);
        }
        return "UNKNOWN";
    }

    public String getSystemStats() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM users WHERE role = 'USER') as total_users, " +
                "(SELECT COUNT(*) FROM accounts) as total_accounts, " +
                "(SELECT COUNT(*) FROM transactions WHERE type = 'DEPOSIT') as total_deposits";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return String.format("System Statistics\nTotal Users: %d\nTotal Accounts: %d\nTotal Deposits: %d",
                        rs.getInt("total_users"), rs.getInt("total_accounts"), rs.getInt("total_deposits"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving system stats", e);
        }
        return "Error retrieving statistics.";
    }

    public String getSystemStats(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM users WHERE role = 'USER' AND created_at BETWEEN ? AND ?) as total_users, " +
                "(SELECT COUNT(*) FROM accounts WHERE created_at BETWEEN ? AND ?) as total_accounts, " +
                "(SELECT COUNT(*) FROM transactions WHERE type = 'DEPOSIT' AND timestamp BETWEEN ? AND ?) as total_deposits";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            stmt.setTimestamp(3, Timestamp.valueOf(start));
            stmt.setTimestamp(4, Timestamp.valueOf(end));
            stmt.setTimestamp(5, Timestamp.valueOf(start));
            stmt.setTimestamp(6, Timestamp.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return String.format("Financial Summary (%s to %s)\nTotal Users: %d\nTotal Accounts: %d\nTotal Deposits: %d",
                        start.format(ISO_FORMATTER), end.format(ISO_FORMATTER),
                        rs.getInt("total_users"), rs.getInt("total_accounts"), rs.getInt("total_deposits"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving system stats with date range", e);
        }
        return "Error retrieving financial summary.";
    }

    public String getUserActivityReport(LocalDateTime start, LocalDateTime end) {
        StringBuilder report = new StringBuilder("User Activity Report (" + start.format(ISO_FORMATTER) + " to " + end.format(ISO_FORMATTER) + ")\n");
        String sql = "SELECT username, created_at FROM users WHERE created_at BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            int userCount = 0;
            while (rs.next()) {
                userCount++;
                report.append(String.format("User: %s, Created: %s\n",
                        rs.getString("username"), rs.getTimestamp("created_at").toLocalDateTime().format(ISO_FORMATTER)));
            }
            report.append("Total Active Users: ").append(userCount).append("\n");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving user activity report", e);
            return "Error retrieving user activity report.";
        }
        return report.toString();
    }

    public String getAccountStatusReport(LocalDateTime start, LocalDateTime end) {
        StringBuilder report = new StringBuilder("Account Status Report (" + start.format(ISO_FORMATTER) + " to " + end.format(ISO_FORMATTER) + ")\n");
        String sql = "SELECT account_id, status, created_at FROM accounts WHERE created_at BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            int accountCount = 0;
            while (rs.next()) {
                accountCount++;
                report.append(String.format("Account: %s, Status: %s, Created: %s\n",
                        rs.getString("account_id"), rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime().format(ISO_FORMATTER)));
            }
            report.append("Total Accounts: ").append(accountCount).append("\n");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving account status report", e);
            return "Error retrieving account status report.";
        }
        return report.toString();
    }

    public boolean addCustomer(String username, String password, String firstName, String lastName,
                               String email, String phone, String role) {
        String hashedPassword = EncryptionUtil.hashPassword(password); // Use EncryptionUtil for hashing
        String sql = "INSERT INTO users (username, password, first_name, last_name, email, phone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, email);
            stmt.setString(6, phone);
            stmt.setString(7, role);
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding customer: " + username, e);
            return false;
        }
    }

    public boolean updateCustomer(String username, String password, String firstName, String lastName,
                                  String email, String phone, String role) {
        String hashedPassword = EncryptionUtil.hashPassword(password); // Use EncryptionUtil for hashing
        String sql = "UPDATE users SET password = ?, first_name = ?, last_name = ?, email = ?, phone = ?, role = ? WHERE username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, email);
            stmt.setString(5, phone);
            stmt.setString(6, role);
            stmt.setString(7, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating customer: " + username, e);
            return false;
        }
    }

    public boolean deleteCustomer(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting customer: " + username, e);
            return false;
        }
    }

    public boolean suspendAccount(String accountId) {
        String sql = "UPDATE accounts SET status = 'SUSPENDED' WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error suspending account: " + accountId, e);
            return false;
        }
    }

    public boolean reactivateAccount(String accountId) {
        String sql = "UPDATE accounts SET status = 'ACTIVE' WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error reactivating account: " + accountId, e);
            return false;
        }
    }
}
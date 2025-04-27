package Service;

import database.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Account;
import model.Customer;
import model.Transaction.TransactionStatus;
import model.UPITransaction;
import utils.AuditLogger;
import utils.AuditLogger.AuditEventType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UPIService {
    private static final Logger logger = Logger.getLogger(UPIService.class.getName());
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuditLogger auditLogger;
    private final ObservableList<UPITransaction> transactions;

    public UPIService() throws SQLException {
        this.accountService = new AccountService();
        this.transactionService = new TransactionService();
        this.auditLogger = AuditLogger.getInstance();
        this.transactions = FXCollections.observableArrayList();
        loadSavedTransactions();
    }

    private void loadSavedTransactions() {
        String sql = "SELECT * FROM transactions WHERE type = 'UPI_PAYMENT' ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UPITransaction transaction = new UPITransaction(
                        rs.getString("transaction_id"),
                        rs.getString("from_account_id"),
                        rs.getString("to_account_id"),
                        rs.getBigDecimal("amount"),
                        TransactionStatus.valueOf(rs.getString("status").toUpperCase()),
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getString("description")
                );
                transactions.add(transaction);
            }
            logger.info("Loaded " + transactions.size() + " UPI transactions from database");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading UPI transactions", e);
        }
    }

    public ObservableList<UPITransaction> getTransactions() {
        return transactions;
    }

    public boolean registerUpiId(Customer customer, String accountId, String upiId) {
        if (upiId == null || upiId.trim().isEmpty() || !isValidUpiId(upiId)) {
            logger.warning("Invalid UPI ID: " + upiId);
            return false;
        }

        if (isUpiIdRegistered(upiId)) {
            logger.warning("UPI ID already registered: " + upiId);
            return false;
        }

        Account account = accountService.getAccountById(accountId);
        if (account == null || !account.getUsername().equals(customer.getUsername()) || !account.isActive()) {
            logger.warning("Account not found, does not belong to customer, or not active: " + accountId);
            return false;
        }

        String sql = "INSERT INTO upi (upi_id, username, account_id, created_at, last_updated, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setString(1, upiId);
            stmt.setString(2, customer.getUsername());
            stmt.setString(3, accountId);
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setString(6, "ACTIVE");

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("UPI ID registered successfully: " + upiId);
                auditLogger.logEvent(AuditEventType.UPI_REGISTRATION, "UPI ID registered: " + upiId);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error registering UPI ID: " + upiId, e);
        }
        return false;
    }

    private boolean isUpiIdRegistered(String upiId) {
        String sql = "SELECT COUNT(*) FROM upi WHERE upi_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, upiId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking UPI ID registration: " + upiId, e);
        }
        return false;
    }

    public boolean makeUPIPayment(String fromUpiId, String toUpiId, BigDecimal amount, String description) throws SQLException {
        if (!isValidUpiId(fromUpiId) || !isValidUpiId(toUpiId) || fromUpiId.equals(toUpiId)) {
            logger.warning("Invalid or same UPI IDs: from=" + fromUpiId + ", to=" + toUpiId);
            return false;
        }

        String fromAccountId = getAccountIdByUpiId(fromUpiId);
        String toAccountId = getAccountIdByUpiId(toUpiId);
        if (fromAccountId == null || toAccountId == null) {
            logger.warning("Could not resolve account IDs for UPI IDs: from=" + fromUpiId + ", to=" + toUpiId);
            return false;
        }

        Account fromAccount = accountService.getAccountById(fromAccountId);
        Account toAccount = accountService.getAccountById(toAccountId);
        if (fromAccount == null || !fromAccount.isActive() || toAccount == null || !toAccount.isActive()) {
            logger.warning("UPI payment failed: From account " + fromAccountId + " or to account " + toAccountId + " not found or not active");
            return false;
        }

        if (!hasSufficientBalance(fromAccountId, amount)) {
            logger.warning("Insufficient balance for account: " + fromAccountId);
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            // Deduct from sender
            String updateFromSql = "UPDATE accounts SET balance = balance - ?, last_updated = NOW() WHERE account_id = ? AND balance >= ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateFromSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setString(2, fromAccountId);
                pstmt.setBigDecimal(3, amount);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    logger.severe("Failed to deduct amount from account: " + fromAccountId);
                    return false;
                }
            }

            // Add to receiver
            String updateToSql = "UPDATE accounts SET balance = balance + ?, last_updated = NOW() WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateToSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setString(2, toAccountId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    logger.severe("Failed to add amount to account: " + toAccountId);
                    return false;
                }
            }

            // Log transaction
            String transactionId = java.util.UUID.randomUUID().toString();
            String insertTransactionSql = "INSERT INTO transactions (transaction_id, from_account_id, to_account_id, amount, type, status, timestamp, description) VALUES (?, ?, ?, ?, 'UPI_PAYMENT', 'COMPLETED', ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertTransactionSql)) {
                pstmt.setString(1, transactionId);
                pstmt.setString(2, fromAccountId);
                pstmt.setString(3, toAccountId);
                pstmt.setBigDecimal(4, amount);
                pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(6, description);
                pstmt.executeUpdate();
            }

            conn.commit();
            UPITransaction upiTransaction = new UPITransaction(transactionId, fromUpiId, toUpiId, fromAccountId, toAccountId, amount, TransactionStatus.COMPLETED, LocalDateTime.now(), description);
            transactions.add(upiTransaction);
            logger.info("UPI payment successful: " + transactionId);
            auditLogger.logEvent(AuditEventType.UPI_PAYMENT, "UPI payment from " + fromUpiId + " to " + toUpiId + " for " + amount);
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during UPI payment", e);
            if (conn != null) conn.rollback();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    DatabaseConnector.releaseConnection();
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error releasing connection", e);
                }
            }
        }
    }

    public String getAccountIdByUpiId(String upiId) {
        String sql = "SELECT account_id FROM upi WHERE upi_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, upiId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("account_id");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving account ID for UPI ID: " + upiId, e);
        }
        return null;
    }

    public List<String> getUpiIdsByUsername(String username) {
        List<String> upiIds = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Username is null or empty");
            return upiIds;
        }
        String sql = "SELECT upi_id FROM upi WHERE username = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    upiIds.add(rs.getString("upi_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving UPI IDs for username: " + username, e);
        }
        return upiIds;
    }

    public boolean hasSufficientBalance(String accountId, BigDecimal amount) {
        Account account = accountService.getAccountById(accountId);
        return account != null && account.getBalance() != null && account.getBalance().compareTo(amount) >= 0;
    }

    public boolean isValidUpiId(String upiId) {
        return upiId != null && upiId.trim().matches("^[a-zA-Z0-9]+@[a-zA-Z0-9]+$");
    }

    public List<String> getAllUpiIds() {
        List<String> upiIds = new ArrayList<>();
        String sql = "SELECT upi_id FROM upi WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                upiIds.add(rs.getString("upi_id"));
            }
            logger.info("Retrieved " + upiIds.size() + " active UPI IDs");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving UPI IDs", e);
        }
        return upiIds;
    }
}
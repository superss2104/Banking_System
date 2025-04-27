package Service;

import Interfaces.AccountOperations;
import database.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Account;
import utils.AuditLogger;
import utils.AuditLogger.AuditEventType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountService implements AccountOperations {
    private static final Logger logger = Logger.getLogger(AccountService.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final AuditLogger auditLogger;
    private final ObservableList<Account> accounts;

    public AccountService() throws SQLException {
        this.auditLogger = AuditLogger.getInstance();
        this.accounts = FXCollections.observableArrayList();
        loadAccounts();
    }

    private void loadAccounts() {
        String sql = "SELECT account_id, username, account_type, balance, created_at, last_updated, status FROM accounts WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Account account = new Account(
                        rs.getString("account_id"),
                        rs.getString("username"),
                        rs.getString("account_type"),
                        rs.getBigDecimal("balance"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_updated").toLocalDateTime(),
                        rs.getString("status")
                );
                accounts.add(account);
            }
            DatabaseConnector.releaseConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading accounts", e);
            DatabaseConnector.releaseConnection();
        }
    }

    @Override
    public Account createAccount(String username, String accountType, BigDecimal initialBalance) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Cannot create account with null or empty username");
            return null;
        }

        String accountId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        String sql = "INSERT INTO accounts (account_id, username, account_type, balance, created_at, last_updated, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            stmt.setString(2, username);
            stmt.setString(3, accountType);
            stmt.setBigDecimal(4, initialBalance != null ? initialBalance : BigDecimal.ZERO);
            stmt.setString(5, now.format(ISO_FORMATTER));
            stmt.setString(6, now.format(ISO_FORMATTER));
            stmt.setString(7, "ACTIVE");

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                Account account = new Account(
                        accountId,
                        username,
                        accountType,
                        initialBalance != null ? initialBalance : BigDecimal.ZERO,
                        now,
                        now,
                        "ACTIVE"
                );
                accounts.add(account);
                auditLogger.logEvent(AuditEventType.ACCOUNT_CREATION,
                        "Account created: " + accountId + " for user: " + username);
                DatabaseConnector.releaseConnection();
                return account;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating account", e);
        }
        DatabaseConnector.releaseConnection();
        return null;
    }

    @Override
    public boolean updateAccount(String accountId, String newAccountType, BigDecimal newBalance, String newStatus) {
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warning("Cannot update account with null or empty account ID");
            return false;
        }
        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warning("Cannot update balance to negative value");
            return false;
        }
        Account account = getAccount(accountId);
        if (account == null) {
            logger.warning("Account not found: " + accountId);
            return false;
        }

        String sql = "UPDATE accounts SET account_type = ?, balance = ?, status = ?, last_updated = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newAccountType);
            stmt.setBigDecimal(2, newBalance);
            stmt.setString(3, newStatus);
            stmt.setString(4, LocalDateTime.now().format(ISO_FORMATTER));
            stmt.setString(5, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                account.setAccountType(newAccountType);
                account.setBalance(newBalance);
                account.setStatus(newStatus);
                account.setLastUpdated(LocalDateTime.now());
                if (!"ACTIVE".equals(newStatus)) {
                    accounts.remove(account);
                }
                auditLogger.logEvent(AuditEventType.ACCOUNT_UPDATE, "Account updated: " + accountId);
                logger.info("Account updated successfully: " + accountId);
                DatabaseConnector.releaseConnection();
                return true;
            } else {
                logger.warning("No account found with ID: " + accountId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating account", e);
        }
        DatabaseConnector.releaseConnection();
        return false;
    }

    @Override
    public boolean updateAccount(String accountId, BigDecimal newBalance) {
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warning("Cannot update account with null or empty account ID");
            return false;
        }
        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warning("Cannot update balance to negative value");
            return false;
        }
        Account account = getAccount(accountId);
        if (account == null) {
            logger.warning("Account not found: " + accountId);
            return false;
        }

        String sql = "UPDATE accounts SET balance = ?, last_updated = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, LocalDateTime.now().format(ISO_FORMATTER));
            stmt.setString(3, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                account.setBalance(newBalance);
                account.setLastUpdated(LocalDateTime.now());
                auditLogger.logEvent(AuditEventType.ACCOUNT_UPDATE, "Account balance updated: " + accountId);
                logger.info("Account balance updated successfully: " + accountId);
                DatabaseConnector.releaseConnection();
                return true;
            } else {
                logger.warning("No account found with ID: " + accountId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating account balance", e);
        }
        DatabaseConnector.releaseConnection();
        return false;
    }

    @Override
    public boolean updateAccount(String accountNumber, String newDetails) {
        return false;
    }

    public Account getAccount(String accountId) {
        for (Account account : accounts) {
            if (account.getAccountId().equals(accountId)) {
                return account;
            }
        }

        String sql = "SELECT account_id, username, account_type, balance, created_at, last_updated, status FROM accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account(
                            rs.getString("account_id"),
                            rs.getString("username"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_updated").toLocalDateTime(),
                            rs.getString("status")
                    );
                    if (account.isActive() && !accounts.contains(account)) {
                        accounts.add(account);
                    }
                    DatabaseConnector.releaseConnection();
                    return account;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving account", e);
        }
        DatabaseConnector.releaseConnection();
        return null;
    }

    public boolean updateAccountType(String accountId, String newAccountType) {
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warning("Cannot update account type with null or empty account ID");
            return false;
        }
        Account account = getAccount(accountId);
        if (account == null) {
            logger.warning("Account not found: " + accountId);
            return false;
        }

        String sql = "UPDATE accounts SET account_type = ?, last_updated = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newAccountType);
            stmt.setString(2, LocalDateTime.now().format(ISO_FORMATTER));
            stmt.setString(3, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                account.setAccountType(newAccountType);
                account.setLastUpdated(LocalDateTime.now());
                auditLogger.logEvent(AuditEventType.ACCOUNT_UPDATE, "Account type updated: " + accountId);
                logger.info("Account type updated successfully: " + accountId);
                DatabaseConnector.releaseConnection();
                return true;
            } else {
                logger.warning("No account found with ID: " + accountId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating account type", e);
        }
        DatabaseConnector.releaseConnection();
        return false;
    }

    public List<Account> getAccountsByUsername(String username) {
        List<Account> userAccounts = new ArrayList<>();
        String sql = "SELECT account_id, username, account_type, balance, created_at, last_updated, status FROM accounts WHERE username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Account account = new Account(
                            rs.getString("account_id"),
                            rs.getString("username"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_updated").toLocalDateTime(),
                            rs.getString("status")
                    );
                    userAccounts.add(account);
                    // Update the cache if the account exists
                    accounts.removeIf(a -> a.getAccountId().equals(account.getAccountId()));
                    if (account.isActive()) {
                        accounts.add(account);
                    }
                }
            }
            logger.info("Retrieved " + userAccounts.size() + " accounts for username: " + username);
            DatabaseConnector.releaseConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving accounts by username", e);
            DatabaseConnector.releaseConnection();
        }
        return userAccounts;
    }

    public List<Account> getAccountsByUserId(String userId) {
        return getAccountsByUsername(userId);
    }

    public ObservableList<Account> getAccountsObservable() {
        return accounts;
    }

    public boolean updateBalance(String accountId, BigDecimal newBalance) {
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warning("Cannot update balance for null or empty account ID");
            return false;
        }
        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warning("Cannot update balance to negative value");
            return false;
        }
        Account account = getAccount(accountId);
        if (account == null) {
            logger.warning("Account not found: " + accountId);
            return false;
        }
        String sql = "UPDATE accounts SET balance = ?, last_updated = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, LocalDateTime.now().format(ISO_FORMATTER));
            stmt.setString(3, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Account balance updated successfully: " + accountId);
                account.setBalance(newBalance);
                account.setLastUpdated(LocalDateTime.now());
                DatabaseConnector.releaseConnection();
                return true;
            } else {
                logger.warning("No account found with ID: " + accountId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating account balance", e);
        }
        DatabaseConnector.releaseConnection();
        return false;
    }

    @Override
    public BigDecimal getAccountBalance(String accountId) {
        Account account = getAccount(accountId);
        DatabaseConnector.releaseConnection();
        return account != null ? account.getBalance() : null;
    }

    @Override
    public String getAccountDetails(String accountId) {
        Account account = getAccount(accountId);
        DatabaseConnector.releaseConnection();
        return account != null ? account.toString() : null;
    }

    @Override
    public boolean deleteAccount(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warning("Cannot delete account with null or empty account ID");
            return false;
        }
        Account account = getAccount(accountId);
        if (account == null) {
            logger.warning("Account not found: " + accountId);
            return false;
        }
        String sql = "DELETE FROM accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Account deleted successfully: " + accountId);
                accounts.removeIf(a -> a.getAccountId().equals(accountId));
                auditLogger.logEvent(AuditEventType.ACCOUNT_DELETION, "Account deleted: " + accountId);
                DatabaseConnector.releaseConnection();
                return true;
            } else {
                logger.warning("No account found with ID: " + accountId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting account", e);
        }
        DatabaseConnector.releaseConnection();
        return false;
    }

    @Override
    public boolean suspendAccount(String accountId) {
        boolean result = updateAccountStatus(accountId, "SUSPENDED", false);
        DatabaseConnector.releaseConnection();
        return result;
    }

    @Override
    public boolean reactivateAccount(String accountId) {
        boolean result = updateAccountStatus(accountId, "ACTIVE", true);
        DatabaseConnector.releaseConnection();
        return result;
    }

    public boolean updateAccountStatus(String accountId, String status, boolean active) {
        String sql = "UPDATE accounts SET status = ?, last_updated = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, LocalDateTime.now().format(ISO_FORMATTER));
            stmt.setString(3, accountId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                String action = active ? "reactivated" : "suspended";
                logger.info("Account " + action + " successfully: " + accountId);
                accounts.stream()
                        .filter(a -> a.getAccountId().equals(accountId))
                        .findFirst()
                        .ifPresent(a -> {
                            a.setStatus(status);
                            a.setLastUpdated(LocalDateTime.now());
                            if (!active) {
                                accounts.remove(a);
                            }
                        });
                auditLogger.logEvent(AuditEventType.ACCOUNT_UPDATE, "Account " + action + ": " + accountId);
                DatabaseConnector.releaseConnection();
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating account status", e);
        }
        DatabaseConnector.releaseConnection();
        return false;
    }

    public Account createAccount(String userId, Account.AccountType accountType) throws SQLException {
        String accountId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO accounts (account_id, username, account_type, balance, created_at, last_updated, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            stmt.setString(2, userId);
            stmt.setString(3, accountType.name());
            stmt.setBigDecimal(4, BigDecimal.ZERO);
            stmt.setString(5, now.format(ISO_FORMATTER));
            stmt.setString(6, now.format(ISO_FORMATTER));
            stmt.setString(7, "ACTIVE");
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                Account account = new Account(
                        accountId,
                        userId,
                        accountType.name(),
                        BigDecimal.ZERO,
                        now,
                        now,
                        "ACTIVE"
                );
                accounts.add(account);
                auditLogger.logEvent(AuditEventType.ACCOUNT_CREATION,
                        "Account created: " + accountId + " for user: " + userId);
                logger.info("Created new account: " + accountId);
                DatabaseConnector.releaseConnection();
                return account;
            }
            throw new SQLException("Failed to create account");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating account", e);
            DatabaseConnector.releaseConnection();
            throw e;
        }
    }

    public Account getAccountById(String accountId) {
        // Always fetch from the database to ensure the latest data
        String sql = "SELECT account_id, username, account_type, balance, created_at, last_updated, status FROM accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account(
                            rs.getString("account_id"),
                            rs.getString("username"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_updated").toLocalDateTime(),
                            rs.getString("status")
                    );
                    // Update the cache
                    accounts.removeIf(a -> a.getAccountId().equals(accountId));
                    if (account.isActive()) {
                        accounts.add(account);
                    }
                    DatabaseConnector.releaseConnection();
                    return account;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving account by ID", e);
        }
        DatabaseConnector.releaseConnection();
        return null;
    }

    public void viewMyAccounts(String userId) {
        List<Account> userAccounts = getAccountsByUserId(userId).stream()
                .filter(Account::isActive)
                .toList();
        if (userAccounts.isEmpty()) {
            logger.info("No active accounts found for user: " + userId);
            DatabaseConnector.releaseConnection();
            return;
        }
        logger.info("Found " + userAccounts.size() + " active accounts for user: " + userId);
        for (Account account : userAccounts) {
            logger.info(account.toString());
        }
        DatabaseConnector.releaseConnection();
    }
}
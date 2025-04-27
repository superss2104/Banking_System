package Service;

import database.DatabaseConnector;
import model.Account;
import model.Transaction;
import model.UPITransaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionService {
    private static final Logger logger = Logger.getLogger(TransactionService.class.getName());
    private final AccountService accountService;

    public TransactionService() throws SQLException {
        this.accountService = new AccountService();
    }

    public Transaction deposit(String accountId, double amount, String description) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        Transaction transaction = new Transaction(transactionId, accountId, BigDecimal.valueOf(amount), description, timestamp);

        Account account = accountService.getAccountById(accountId);
        if (account == null || !account.isActive()) {
            logger.warning("Deposit failed: Account " + accountId + " not found or not active");
            return null;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update account balance
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance + ?, last_updated = NOW() WHERE account_id = ?"
                );
                updateStmt.setBigDecimal(1, BigDecimal.valueOf(amount));
                updateStmt.setString(2, accountId);
                int rows = updateStmt.executeUpdate();
                if (rows != 1) {
                    throw new SQLException("Account not found or update failed");
                }

                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO transactions (transaction_id, to_account_id, amount, type, status, timestamp, description) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                insertStmt.setString(1, transactionId);
                insertStmt.setString(2, accountId);
                insertStmt.setBigDecimal(3, BigDecimal.valueOf(amount));
                insertStmt.setString(4, Transaction.TransactionType.DEPOSIT.name());
                insertStmt.setString(5, Transaction.TransactionStatus.COMPLETED.name());
                insertStmt.setTimestamp(6, java.sql.Timestamp.valueOf(timestamp));
                insertStmt.setString(7, description);
                insertStmt.executeUpdate();

                conn.commit();
                return transaction;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Deposit failed: " + e.getMessage(), e);
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection error", e);
            return null;
        }
    }

    public Transaction withdrawFunds(String accountId, double amount, String description) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        Transaction transaction = new Transaction(transactionId, accountId, BigDecimal.valueOf(amount), description, timestamp);

        Account account = accountService.getAccountById(accountId);
        if (account == null || !account.isActive()) {
            logger.warning("Withdrawal failed: Account " + accountId + " not found or not active");
            return null;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check balance
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT balance FROM accounts WHERE account_id = ?"
                );
                checkStmt.setString(1, accountId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Account not found");
                    }
                    BigDecimal balance = rs.getBigDecimal("balance");
                    if (balance.compareTo(BigDecimal.valueOf(amount)) < 0) {
                        throw new SQLException("Insufficient funds");
                    }
                }

                // Update balance
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance - ?, last_updated = NOW() WHERE account_id = ?"
                );
                updateStmt.setBigDecimal(1, BigDecimal.valueOf(amount));
                updateStmt.setString(2, accountId);
                updateStmt.executeUpdate();

                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO transactions (transaction_id, from_account_id, amount, type, status, timestamp, description) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                insertStmt.setString(1, transactionId);
                insertStmt.setString(2, accountId);
                insertStmt.setBigDecimal(3, BigDecimal.valueOf(amount));
                insertStmt.setString(4, Transaction.TransactionType.WITHDRAWAL.name());
                insertStmt.setString(5, Transaction.TransactionStatus.COMPLETED.name());
                insertStmt.setTimestamp(6, java.sql.Timestamp.valueOf(timestamp));
                insertStmt.setString(7, description);
                insertStmt.executeUpdate();

                conn.commit();
                return transaction;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Withdrawal failed: " + e.getMessage(), e);
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection error", e);
            return null;
        }
    }

    public Transaction transferFunds(String fromAccountId, String toAccountId, double amount, String description) {
        String transactionId = UUID.randomUUID().toString();
        String type = Transaction.TransactionType.TRANSFER.name();
        String status = Transaction.TransactionStatus.COMPLETED.name();
        LocalDateTime timestamp = LocalDateTime.now();

        Account fromAccount = accountService.getAccountById(fromAccountId);
        Account toAccount = accountService.getAccountById(toAccountId);
        if (fromAccount == null || !fromAccount.isActive() || toAccount == null || !toAccount.isActive()) {
            logger.warning("Transfer failed: From account " + fromAccountId + " or to account " + toAccountId + " not found or not active");
            return null;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check from account balance
                PreparedStatement fromStmt = conn.prepareStatement(
                        "SELECT balance FROM accounts WHERE account_id = ?"
                );
                fromStmt.setString(1, fromAccountId);
                try (ResultSet rs = fromStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("From account not found");
                    }
                    BigDecimal fromBalance = rs.getBigDecimal("balance");
                    if (fromBalance.compareTo(BigDecimal.valueOf(amount)) < 0) {
                        throw new SQLException("Insufficient funds");
                    }
                }

                // Verify to account exists
                PreparedStatement toStmt = conn.prepareStatement(
                        "SELECT account_id FROM accounts WHERE account_id = ?"
                );
                toStmt.setString(1, toAccountId);
                try (ResultSet rs = toStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("To account not found");
                    }
                }

                // Update balances
                PreparedStatement withdrawStmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance - ?, last_updated = NOW() WHERE account_id = ?"
                );
                withdrawStmt.setBigDecimal(1, BigDecimal.valueOf(amount));
                withdrawStmt.setString(2, fromAccountId);
                withdrawStmt.executeUpdate();

                PreparedStatement depositStmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance + ?, last_updated = NOW() WHERE account_id = ?"
                );
                depositStmt.setBigDecimal(1, BigDecimal.valueOf(amount));
                depositStmt.setString(2, toAccountId);
                depositStmt.executeUpdate();

                // Insert transaction
                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO transactions (transaction_id, from_account_id, to_account_id, amount, type, status, timestamp, description) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                );
                insertStmt.setString(1, transactionId);
                insertStmt.setString(2, fromAccountId);
                insertStmt.setString(3, toAccountId);
                insertStmt.setBigDecimal(4, BigDecimal.valueOf(amount));
                insertStmt.setString(5, type);
                insertStmt.setString(6, status);
                insertStmt.setTimestamp(7, java.sql.Timestamp.valueOf(timestamp));
                insertStmt.setString(8, description);
                insertStmt.executeUpdate();

                conn.commit();
                return new Transaction(transactionId, fromAccountId, toAccountId, type, BigDecimal.valueOf(amount), status, timestamp, description);
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Transfer failed: " + e.getMessage(), e);
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection error", e);
            return null;
        }
    }

    public List<Transaction> getTransactionHistory(String userId) {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT t.transaction_id, t.from_account_id, t.to_account_id, t.amount, t.type, t.status, t.timestamp, t.description " +
                             "FROM transactions t " +
                             "JOIN accounts a ON t.from_account_id = a.account_id OR t.to_account_id = a.account_id " +
                             "WHERE a.username = ?")) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                Transaction transaction;
                if (type.equals(Transaction.TransactionType.PAYMENT.name())) {
                    transaction = new UPITransaction(
                            rs.getString("transaction_id"),
                            rs.getString("from_account_id"),
                            rs.getString("to_account_id"),
                            type,
                            rs.getString("status"),
                            rs.getBigDecimal("amount"),
                            Transaction.TransactionStatus.valueOf(rs.getString("status")),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("description")
                    );
                } else {
                    transaction = new Transaction(
                            rs.getString("transaction_id"),
                            rs.getString("from_account_id"),
                            rs.getString("to_account_id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("status"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("description")
                    );
                }
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error fetching transaction history", e);
        }
        return transactions;
    }
}
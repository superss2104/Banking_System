package Service;

import database.DatabaseConnector;
import model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportService {
    private static final Logger logger = Logger.getLogger(ReportService.class.getName());

    // Inner class to hold report data
    public static class TransactionReport {
        private final List<Transaction> transactions;
        private final BigDecimal totalDeposits;
        private final BigDecimal totalWithdrawals;

        public TransactionReport(List<Transaction> transactions, BigDecimal totalDeposits, BigDecimal totalWithdrawals) {
            this.transactions = transactions;
            this.totalDeposits = totalDeposits;
            this.totalWithdrawals = totalWithdrawals;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public BigDecimal getTotalDeposits() {
            return totalDeposits;
        }

        public BigDecimal getTotalWithdrawals() {
            return totalWithdrawals;
        }
    }

    public TransactionReport generateTransactionReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Transaction> transactions = getTransactions(null, startDateTime, endDateTime); // Null for all accounts
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (Transaction.TransactionType.DEPOSIT.name().equals(t.getType())) {
                totalDeposits = totalDeposits.add(t.getAmount());
            }
            if (Transaction.TransactionType.WITHDRAWAL.name().equals(t.getType())) {
                totalWithdrawals = totalWithdrawals.add(t.getAmount());
            }
        }
        logger.info("Generated Financial Summary: Total Transactions=" + transactions.size() +
                ", Deposits=$" + totalDeposits + ", Withdrawals=$" + totalWithdrawals);
        return new TransactionReport(transactions, totalDeposits, totalWithdrawals);
    }

    public void generateUserActivityReport(String username, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        logger.info("Generated User Activity Report for period " + startDateTime + " to " + endDateTime);
        // Can extend for PDF export later
    }

    private List<Transaction> getTransactions(String accountId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT transaction_id, from_account_id, to_account_id, amount, type, status, description, timestamp " +
                "FROM transactions WHERE (from_account_id = ? OR to_account_id = ? OR ? IS NULL) " +
                "AND timestamp BETWEEN ? AND ? ORDER BY timestamp";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountId);
            stmt.setString(2, accountId);
            stmt.setString(3, accountId);
            stmt.setTimestamp(4, Timestamp.valueOf(startDate));
            stmt.setTimestamp(5, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaction(
                            rs.getString("transaction_id"),
                            rs.getString("from_account_id"),
                            rs.getString("to_account_id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("status"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("description")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving transactions for account: " + accountId, e);
            throw new RuntimeException("Failed to retrieve transactions", e);
        } finally {
            DatabaseConnector.releaseConnection(); // Ensure connection is released
        }

        return transactions;
    }
}
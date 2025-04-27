package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseInitializer {
    private static final Logger logger = Logger.getLogger(DatabaseInitializer.class.getName());
    private final Connection connection;

    public DatabaseInitializer(Connection connection) {
        this.connection = connection;
    }

    public void initializeDatabase() {
        try {
            // Create tables if they don't exist
            createUsersTable();
            createAccountsTable();
            createTransactionsTable();
            createAuditLogsTable();
            createSystemLogsTable();
            createUpiTable();

            createDefaultAdmin();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createUpiTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS upi (" +
                "upi_id VARCHAR(100) PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL, " +
                "account_id VARCHAR(50), " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                "FOREIGN KEY (username) REFERENCES users(username), " +
                "FOREIGN KEY (account_id) REFERENCES accounts(account_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("UPI table created/verified successfully.");
        }
    }

    private void createUsersTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "username VARCHAR(50) PRIMARY KEY, " +
                "password VARCHAR(100) NOT NULL, " +
                "first_name VARCHAR(50), " +
                "last_name VARCHAR(50), " +
                "email VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "role VARCHAR(20) DEFAULT 'USER', " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("Users table created/verified successfully.");
        }
    }

    private void createAccountsTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "account_id VARCHAR(50) PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL, " +
                "account_type VARCHAR(20) NOT NULL, " +
                "balance DECIMAL(15,2) NOT NULL DEFAULT 0.00, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                "FOREIGN KEY (username) REFERENCES users(username)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("Accounts table created/verified successfully.");
        }
    }

    private void createTransactionsTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "transaction_id VARCHAR(50) PRIMARY KEY, " +
                "from_account_id VARCHAR(50), " +
                "to_account_id VARCHAR(50), " +
                "amount DECIMAL(15,2) NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "description TEXT, " +
                "FOREIGN KEY (from_account_id) REFERENCES accounts(account_id), " +
                "FOREIGN KEY (to_account_id) REFERENCES accounts(account_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("Transactions table created/verified successfully.");
        }
    }

    private void createAuditLogsTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "log_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50), " +
                "event_type VARCHAR(50) NOT NULL, " +
                "description TEXT, " +
                "timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (username) REFERENCES users(username)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("Audit logs table created/verified successfully.");
        }
    }

    private void createSystemLogsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS system_logs (" +
                "log_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "level VARCHAR(10) NOT NULL, " +
                "message TEXT, " +
                "timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("System logs table created/verified successfully.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating system logs table", e);
        }
    }

    private void createDefaultAdmin() throws SQLException {
        String deleteAdmin = "DELETE FROM users WHERE username = 'admin' AND role = 'ADMIN'";
        String defaultPassword = "admin";
        String hashedPassword = utils.EncryptionUtil.hashPassword(defaultPassword);
        String insertAdmin = "INSERT INTO users (username, password, first_name, last_name, email, role, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmtDelete = connection.prepareStatement(deleteAdmin);
             PreparedStatement stmtInsert = connection.prepareStatement(insertAdmin)) {
            stmtDelete.executeUpdate();
            stmtInsert.setString(1, "admin");
            stmtInsert.setString(2, hashedPassword);
            stmtInsert.setString(3, "System");
            stmtInsert.setString(4, "Administrator");
            stmtInsert.setString(5, "admin@system.com");
            stmtInsert.setString(6, "ADMIN");
            stmtInsert.executeUpdate();
            logger.info("Default admin user recreated with new hashed password: " + hashedPassword.substring(0, 10) + "...");
        }
    }
}
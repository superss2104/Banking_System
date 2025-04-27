package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnector {
    private static final Logger logger = Logger.getLogger(DatabaseConnector.class.getName());
    private static final String DB_URL = "jdbc:mysql://localhost:3306/banking_system";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "New2pass";
    private static final Object lock = new Object();
    private static final AtomicBoolean isInUse = new AtomicBoolean(false);
    private static volatile Connection connection;
    private static boolean isShuttingDown = false;

    private DatabaseConnector() {
        // Private constructor to prevent instantiation
    }

    public static Connection getConnection() throws SQLException {
        if (isShuttingDown) {
            throw new IllegalStateException("Cannot get connection during shutdown");
        }

        synchronized (lock) {
            if (connection == null || !isConnectionValid()) {
                try {
                    if (connection != null) {
                        connection.close();
                        connection = null;
                    }

                    Class.forName("com.mysql.cj.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    connection.setAutoCommit(true);
                    logger.info("Database connection established successfully.");
                } catch (ClassNotFoundException e) {
                    logger.log(Level.SEVERE, "MySQL JDBC driver not found", e);
                    throw new SQLException("Database driver not found", e);
                } catch (SQLException e) {
                    if (e.getMessage().contains("Unknown database")) {
                        createDatabase();
                        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                        connection.setAutoCommit(true);
                    } else {
                        logger.log(Level.SEVERE, "Error connecting to database", e);
                        throw e;
                    }
                }
            }
            isInUse.set(true); // Mark connection as in use
            return connection;
        }
    }

    private static boolean isConnectionValid() {
        if (connection == null) {
            return false;
        }
        try {
            return connection.isValid(3); // 3 second timeout
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error checking connection validity", e);
            return false;
        }
    }

    private static void createDatabase() throws SQLException {
        String createDbUrl = "jdbc:mysql://localhost:3306";
        try (Connection tempConnection = DriverManager.getConnection(createDbUrl, DB_USER, DB_PASSWORD);
             var stmt = tempConnection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS banking_system");
            logger.info("Database 'banking_system' created successfully");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating database", e);
            throw e;
        }
    }

    public static void releaseConnection() {
        synchronized (lock) {
            isInUse.set(false);
            if (isShuttingDown && !isInUse.get() && connection != null) {
                try {
                    connection.close();
                    connection = null;
                    logger.info("Database connection closed successfully.");
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error closing database connection", e);
                }
            }
        }
    }

    public static void closeConnection() {
        synchronized (lock) {
            isShuttingDown = true;
            if (connection != null && !isInUse.get()) {
                try {
                    connection.close();
                    connection = null;
                    logger.info("Database connection closed successfully.");
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error closing database connection", e);
                }
            } else if (isInUse.get()) {
                logger.warning("Connection in use, will close on next release");
            }
        }
    }
}
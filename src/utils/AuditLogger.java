package utils;

import database.DatabaseConnector;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class AuditLogger implements Closeable {
    public static final Logger logger = Logger.getLogger(AuditLogger.class.getName());
    private static AuditLogger instance;
    private final Connection connection;

    public AuditLogger() throws SQLException {
        this.connection = DatabaseConnector.getConnection();
    }

    public static AuditLogger getInstance() throws SQLException {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    public void logEvent(AuditEventType eventType, String message) {
        logger.info("[" + eventType + "] " + message);
    }

    public void logEvent(AuditEventType eventType, String message, String username) {
        String sql = "INSERT INTO audit_logs (username, event_type, description, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, eventType.name());
            statement.setString(3, message);

            statement.executeUpdate();

            // Also print to console for immediate visibility
            System.out.printf("[%s] %s: %s%n", eventType.name(),
                    username != null ? username : "SYSTEM", message);
        } catch (SQLException e) {
            System.err.println("Error logging audit event: " + e.getMessage());

            // Still print to console even if database logging fails
            System.out.printf("[%s] %s: %s (DB LOG FAILED)%n", eventType.name(),
                    username != null ? username : "SYSTEM", message);
        }
    }

    @Override
    public void close() {
        // Nothing to close specifically, connection is managed by DatabaseConnector
    }

    public void log(AuditEventType eventType, String message) {
        logEvent(eventType, message);
    }

    public enum AuditEventType {
        LOGIN("Login"),
        LOGIN_ADMIN("Admin Login"),
        LOGOUT("Logout"),
        LOGIN_FAILED("Login Failed"),
        USER_CREATION("User Created"),
        USER_UPDATE("User Updated"),
        USER_DELETION("User Deleted"),
        ACCOUNT_CREATION("Account Created"),
        ACCOUNT_UPDATE("Account Updated"),
        ACCOUNT_DELETION("Account Deleted"),
        TRANSACTION("Transaction"),
        UPI_REGISTRATION("UPI Registration"),
        UPI_TRANSACTION("UPI Transaction"),
        REPORT_GENERATION("Report Generated"),
        SYSTEM("System Event"),
        SYSTEM_INFO("System Information"),
        ADMIN_ACTION("Admin Action"), SYSTEM_ERROR("System Error"), SECURITY_EVENT("Security Event"), PROFILE_UPDATE("Profile Update"), PASSWORD_CHANGE("Password Change"), NAVIGATION("Navigation"),
        TRANSACTION_VIEW("Transaction View"),
        UPI_PAYMENT_ATTEMPT("UPI Payment Attempt"),
        ADMIN("Admin"), UNAUTHORIZED_ACCESS("Unauthorized Access"), ACCOUNT_LOCKED("Account Locked"), ACCOUNT_UNLOCKED("Account Unlocked"), PASSWORD_RESET("Password Reset"), PASSWORD_RESET_REQUEST("Password Reset Request"), USER_SUSPENDED("User Suspended"), USER_REACTIVATED("User Reactivated"), HELP("Help"), APPLICATION_SHUTDOWN(""), USER_LOGIN(""), NEW_USER("New User"), REGISTRATION_FAILED(""), UPI_PAYMENT("");

        private final String displayName;

        AuditEventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
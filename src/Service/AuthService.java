package Service;

import database.DatabaseConnector;
import model.Customer;
import utils.AuditLogger;
import utils.AuditLogger.AuditEventType;
import utils.EncryptionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final AuditLogger auditLogger;
    private final CustomerService customerService;

    public AuthService() throws SQLException {
        this.auditLogger = AuditLogger.getInstance();
        this.customerService = new CustomerService();
    }

    public Customer login(String username, String password) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT username, password, first_name, last_name, email, phone, role, created_at FROM users WHERE username = ?")) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {  // Only need one rs.next() call to move to the first row
                    String storedHash = rs.getString("password");
                    System.out.println("Entered password (raw): " + password);
                    System.out.println("Hashed input password: " + EncryptionUtil.hashPassword(password));
                    System.out.println("Stored hash from DB: " + storedHash);

                    if (EncryptionUtil.verifyPassword(password, storedHash)) {
                        System.out.println("✅ Password matched");
                        // Create Customer object with data from the ResultSet
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
                        auditLogger.logEvent(AuditEventType.LOGIN, "User logged in successfully: " + username);
                        return customer;
                    } else {
                        System.out.println("❌ Password verification failed");
                    }
                } else {
                    System.out.println("❌ User not found");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during authentication", e);
            throw new RuntimeException("Authentication failed", e);
        }

        auditLogger.logEvent(AuditEventType.LOGIN_FAILED, "Login failed for user: " + username);
        return null;
    }

    public Customer register(String username, String password, String firstName,
                             String lastName, String email, String phone) throws SQLException {
        if (!isValidUsername(username) || !isValidPassword(password) ||
                !isValidEmail(email) || !isValidName(firstName) || !isValidName(lastName)) {
            logger.warning("Invalid registration parameters");
            return null;
        }

        if (customerService.isUsernameTaken(username)) {
            logger.warning("Username already taken: " + username);
            return null;
        }
        Connection conn = DatabaseConnector.getConnection();
        try {
            logger.info("Connection isClosed before setAutoCommit: " + conn.isClosed());
            conn.setAutoCommit(false);


            String passwordHash = EncryptionUtil.hashPassword(password);
            LocalDateTime now = LocalDateTime.now();
            String sql = "INSERT INTO users (username, password, first_name, last_name, email, phone, role, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                logger.info("Connection isClosed before prepareStatement: " + conn.isClosed());
                stmt.setString(1, username);
                stmt.setString(2, passwordHash);
                stmt.setString(3, firstName);
                stmt.setString(4, lastName);
                stmt.setString(5, email);
                stmt.setString(6, phone);
                stmt.setString(7, "USER");
                stmt.setTimestamp(8, java.sql.Timestamp.valueOf(now));
                stmt.executeUpdate();
            }

            conn.commit();
            Customer customer = new Customer(
                    username,
                    passwordHash,
                    firstName,
                    lastName,
                    email,
                    phone,
                    "USER",
                    now
            );
            auditLogger.logEvent(AuditEventType.USER_CREATION, "New user registered: " + username);
            return customer;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Rollback failed", rollbackEx);
                }
            }
            logger.log(Level.SEVERE, "Error during registration", e);
            return null;
        } finally {
            DatabaseConnector.releaseConnection(); // Mark connection as available
        }
    }

    public boolean isAdmin(Customer customer) {
        if (customer == null) {
            logger.warning("Cannot check admin status for null customer");
            return false;
        }
        return "ADMIN".equals(customer.getRole());
    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidName(String name) {
        return name != null && name.matches("^[a-zA-Z\\s]{2,50}$");
    }

    public void showHelp(String customer) {
        String helpMessage = "Welcome to the Banking System! Here are some commands you can use:\n" +
                "1. login <username> <password> - Log in to your account\n" +
                "2. register <username> <password> <first_name> <last_name> <email> <phone> - Create a new account\n" +
                "3. logout - Log out of your account\n" +
                "4. help - Show this help message\n";
        auditLogger.logEvent(AuditEventType.HELP, "Help requested by user: " + customer);
        System.out.println(helpMessage);
    }
}
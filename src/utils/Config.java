package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Config {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();
    // Default configuration values
    private static final boolean DEFAULT_DEBUG_MODE = true;
    private static final String DEFAULT_DB_URL = "jdbc:h2:./bankingdb;DB_CLOSE_DELAY=-1";
    private static final String DEFAULT_DB_USER = "sa";
    private static final String DEFAULT_DB_PASSWORD = "";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final int DEFAULT_SESSION_TIMEOUT = 30; // minutes
    private static boolean initialized = false;

    public static void load() {
        if (initialized) {
            return;
        }

        // Set default values
        properties.setProperty("debug.mode", String.valueOf(DEFAULT_DEBUG_MODE));
        properties.setProperty("db.url", DEFAULT_DB_URL);
        properties.setProperty("db.user", DEFAULT_DB_USER);
        properties.setProperty("db.password", DEFAULT_DB_PASSWORD);
        properties.setProperty("log.level", DEFAULT_LOG_LEVEL);
        properties.setProperty("log.dir", DEFAULT_LOG_DIR);
        properties.setProperty("session.timeout", String.valueOf(DEFAULT_SESSION_TIMEOUT));

        // Try to load properties from file
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream input = new FileInputStream(CONFIG_FILE)) {
                properties.load(input);
                System.out.println("Configuration loaded from " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("Could not load configuration file: " + e.getMessage());
            }
        } else {
            System.out.println("Configuration file not found. Using default values.");
        }

        initialized = true;
    }

    public static String getProperty(String key) {
        if (!initialized) {
            load();
        }
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        if (!initialized) {
            load();
        }
        return properties.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        if (!initialized) {
            load();
        }
        properties.setProperty(key, value);
    }

    public static boolean isDebugMode() {
        if (!initialized) {
            load();
        }
        return Boolean.parseBoolean(properties.getProperty("debug.mode"));
    }

    public static String getDbUrl() {
        if (!initialized) {
            load();
        }
        return properties.getProperty("db.url");
    }

    public static String getDbUser() {
        if (!initialized) {
            load();
        }
        return properties.getProperty("db.user");
    }

    public static String getDbPassword() {
        if (!initialized) {
            load();
        }
        return properties.getProperty("db.password");
    }

    public static String getLogLevel() {
        if (!initialized) {
            load();
        }
        return properties.getProperty("log.level");
    }

    public static String getLogDir() {
        if (!initialized) {
            load();
        }
        return properties.getProperty("log.dir");
    }

    public static int getSessionTimeout() {
        if (!initialized) {
            load();
        }
        try {
            return Integer.parseInt(properties.getProperty("session.timeout"));
        } catch (NumberFormatException e) {
            return DEFAULT_SESSION_TIMEOUT;
        }
    }
}
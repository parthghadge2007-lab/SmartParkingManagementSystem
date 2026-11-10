package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Core module component managing database connectivity for Smart Parking.
 * Externalizes configuration to 'db.properties' or environment variables.
 */
public class DatabaseConnection {
    private static String url = "jdbc:mysql://localhost:3306/smart_parking?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static String user = "root";
    private static String password = "YOUR_NEW_PASSWORD";

    static {
        loadConfiguration();
    }

    public static void loadConfiguration() {
        File configFile = new File("db.properties");
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                if (props.containsKey("db.url")) {
                    url = props.getProperty("db.url").trim();
                }
                if (props.containsKey("db.user")) {
                    user = props.getProperty("db.user").trim();
                }
                if (props.containsKey("db.password")) {
                    password = props.getProperty("db.password").trim();
                }
            } catch (IOException e) {
                System.err.println("Note: Could not read db.properties, using fallback settings: " + e.getMessage());
            }
        }

        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            url = envUrl.trim();
        }
        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            user = envUser.trim();
        }
        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) {
            password = envPass.trim();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getSanitizedUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}

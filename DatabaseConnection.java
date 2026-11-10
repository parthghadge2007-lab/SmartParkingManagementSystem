import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connectivity for the Smart Parking Management System.
 * Supports configuration via 'db.properties', environment variables, or default fallbacks.
 */
public class DatabaseConnection {
    private static String url = "jdbc:mysql://localhost:3306/smart_parking?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static String user = "root";
    private static String password = "Parth2007";

    static {
        loadConfiguration();
    }

    /**
     * Loads credentials from db.properties or environment variables.
     */
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

        // Environment variables take highest precedence if set
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

    /**
     * Obtains a new database connection.
     * @return Connection object to smart_parking database
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Tests if the database connection is currently active.
     * @return true if successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns sanitized connection URL for display/debugging (without credentials).
     */
    public static String getSanitizedUrl() {
        return url;
    }

    /**
     * Returns current DB username.
     */
    public static String getUser() {
        return user;
    }
}

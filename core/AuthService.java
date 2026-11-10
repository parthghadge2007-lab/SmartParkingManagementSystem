package core;

/**
 * Authentication service handling operator credential verification.
 */
public class AuthService {

    private static final String DEMO_ADMIN_USER = "admin";
    private static final String DEMO_ADMIN_PASS = "admin123";

    public AuthResult authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new AuthResult(false, "Username cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            return new AuthResult(false, "Password cannot be empty.");
        }

        String trimmedUser = username.trim();
        if (DEMO_ADMIN_USER.equalsIgnoreCase(trimmedUser) && DEMO_ADMIN_PASS.equals(password)) {
            return new AuthResult(true, "Authentication successful.", trimmedUser, "Administrator");
        }

        return new AuthResult(false, "Invalid username or password. Please try again.");
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String username;
        private final String role;

        public AuthResult(boolean success, String message) {
            this(success, message, null, null);
        }

        public AuthResult(boolean success, String message, String username, String role) {
            this.success = success;
            this.message = message;
            this.username = username;
            this.role = role;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}

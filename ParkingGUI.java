import core.LoginFrame;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Main Application Launcher for the Smart Parking Management System.
 * Configures system look-and-feel and launches the Login Window.
 */
public class ParkingGUI {

    public ParkingGUI() {
        launch();
    }

    public static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Font standardFont = new Font("Segoe UI", Font.PLAIN, 13);
            Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof FontUIResource) {
                    UIManager.put(key, new FontUIResource(standardFont));
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}

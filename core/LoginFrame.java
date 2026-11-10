package core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Modern Login Window for the Smart Parking Management System.
 * Features credential validation, show/hide password, Enter key support, and DB status indicator.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;
    private JLabel errorLabel;
    private JButton loginButton;

    public LoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle("Smart Parking System - Secure Login");
        setSize(460, 560);
        setMinimumSize(new Dimension(420, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Root container with dark forest green background
        JPanel backgroundPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConstants.BRAND_FOREST_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(-100, -100, getWidth() + 200, 200);
                g2.dispose();
            }
        };

        // White elevated login card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeConstants.BORDER_LIGHT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(380, 480));
        card.setBorder(new EmptyBorder(25, 30, 25, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Brand Icon (Styled 'P' Badge)
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConstants.BRAND_TEAL_ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.drawString("P", 15, 29);
                g2.dispose();
            }
        };
        logoBadge.setOpaque(false);
        logoBadge.setPreferredSize(new Dimension(42, 40));
        logoBadge.setMaximumSize(new Dimension(42, 40));
        logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("SMART PARKING");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ThemeConstants.BRAND_FOREST_MED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Desktop Management Console");
        subtitleLabel.setFont(ThemeConstants.FONT_SMALL);
        subtitleLabel.setForeground(ThemeConstants.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeConstants.FONT_SMALL);
        errorLabel.setForeground(ThemeConstants.BADGE_OCC_TXT);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(ThemeConstants.FONT_BODY_BOLD);
        userLabel.setForeground(ThemeConstants.TEXT_PRIMARY);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new JTextField();
        styleInputField(usernameField);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(ThemeConstants.FONT_BODY_BOLD);
        passLabel.setForeground(ThemeConstants.TEXT_PRIMARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        styleInputField(passwordField);

        showPasswordCheck = new JCheckBox("Show password");
        showPasswordCheck.setFont(ThemeConstants.FONT_SMALL);
        showPasswordCheck.setForeground(ThemeConstants.TEXT_SECONDARY);
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setFocusPainted(false);
        showPasswordCheck.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showPasswordCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPasswordCheck.addActionListener(e -> {
            if (showPasswordCheck.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });

        loginButton = ThemeConstants.createStyledButton("LOG IN", ThemeConstants.BRAND_TEAL_ACCENT, Color.WHITE, true);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());

        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        usernameField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);

        JPanel hintBox = new JPanel(new BorderLayout());
        hintBox.setOpaque(false);
        hintBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 235, 233), 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        JLabel hintLabel = new JLabel("Demo Admin: admin  |  Password: admin123", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLabel.setForeground(ThemeConstants.TEXT_MUTED);
        hintBox.add(hintLabel, BorderLayout.CENTER);
        hintBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        card.add(logoBadge);
        card.add(Box.createVerticalStrut(8));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        card.add(wrapLeftAligned(userLabel));
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(12));

        card.add(wrapLeftAligned(passLabel));
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(6));

        card.add(showPasswordCheck);
        card.add(Box.createVerticalStrut(18));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(14));
        card.add(hintBox);

        backgroundPanel.add(card);
        getContentPane().add(backgroundPanel);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel wrapLeftAligned(Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        p.add(c, BorderLayout.WEST);
        return p;
    }

    private void styleInputField(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setPreferredSize(new Dimension(320, 36));
        field.setFont(ThemeConstants.FONT_BODY_REGULAR);
        field.setForeground(ThemeConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeConstants.BORDER_LIGHT, 1, true),
            new EmptyBorder(5, 10, 5, 10)
        ));
    }

    private void handleLogin() {
        errorLabel.setText(" ");
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        AuthService.AuthResult result = authService.authenticate(username, password);

        if (!result.isSuccess()) {
            errorLabel.setText(result.getMessage());
            JOptionPane.showMessageDialog(
                this,
                result.getMessage(),
                "Login Failed",
                JOptionPane.ERROR_MESSAGE
            );
            passwordField.requestFocusInWindow();
            return;
        }

        dispose();
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainDashboardFrame dashboard = new MainDashboardFrame(result.getUsername());
            dashboard.setVisible(true);
        });
    }
}

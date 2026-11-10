package core;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import parking.ParkingSlot;
import parking.ParkingSlotManagement;
import parking.ParkingSlotPanel;
import reservation.Reservation;
import reservation.ReservationManagement;
import reservation.ReservationPanel;

/**
 * Main Application Dashboard coordinating Module A (Parking Slots) and Module B (Reservations).
 */
public class MainDashboardFrame extends JFrame {

    private final String loggedInUser;
    private final ParkingSlotManagement slotService;
    private final ReservationManagement reservationService;

    // Sub-Module UI Panels
    private ParkingSlotPanel slotPanel;
    private ReservationPanel reservationPanel;

    // Navigation and Layout
    private CardLayout cardLayout;
    private JPanel contentContainer;
    private JButton navDashboardBtn;
    private JButton navSlotsBtn;
    private JButton navReservationsBtn;
    private JLabel sectionHeaderLabel;
    private JLabel dbStatusLabel;

    // Stat card labels
    private JLabel totalSlotsVal;
    private JLabel availSlotsVal;
    private JLabel occupiedSlotsVal;
    private JLabel activeReservationsVal;

    // Slot Grid Panel on Overview
    private JPanel slotGridPanel;
    private DefaultTableModel recentReservationsTableModel;
    private JTable recentReservationsTable;

    public MainDashboardFrame(String loggedInUser) {
        this.loggedInUser = loggedInUser != null ? loggedInUser : "admin";
        this.slotService = new ParkingSlotManagement();
        this.reservationService = new ReservationManagement();

        // Instantiate modular panels with callback to update global metrics
        this.slotPanel = new ParkingSlotPanel(slotService, this::refreshGlobalMetrics);
        this.reservationPanel = new ReservationPanel(reservationService, this::refreshGlobalMetrics);

        initUI();
        refreshAllData();
    }

    private void initUI() {
        setTitle("Smart Parking Management System - Desktop Console");
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ThemeConstants.BG_MAIN);

        JPanel sidebar = createSidebar();
        JPanel mainArea = createMainArea();

        root.add(sidebar, BorderLayout.WEST);
        root.add(mainArea, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(ThemeConstants.BRAND_FOREST_DARK);
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(new EmptyBorder(20, 16, 20, 16));

        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoRow.setOpaque(false);

        JPanel iconBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConstants.BRAND_TEAL_ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g2.drawString("P", 11, 23);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(32, 32));
        iconBadge.setOpaque(false);

        JLabel brandTitle = new JLabel("SMART PARKING");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        brandTitle.setForeground(Color.WHITE);

        logoRow.add(iconBadge);
        logoRow.add(brandTitle);

        JLabel subtitle = new JLabel("  Modular PBL Edition");
        subtitle.setFont(ThemeConstants.FONT_SMALL);
        subtitle.setForeground(ThemeConstants.TEXT_MUTED);

        brandPanel.add(logoRow);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(subtitle);
        brandPanel.add(Box.createVerticalStrut(20));

        JPanel userPill = new JPanel(new BorderLayout());
        userPill.setBackground(new Color(255, 255, 255, 15));
        userPill.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 30), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        JLabel userLabel = new JLabel("👤 " + loggedInUser.toUpperCase());
        userLabel.setFont(ThemeConstants.FONT_BODY_BOLD);
        userLabel.setForeground(ThemeConstants.TEXT_ON_DARK);
        JLabel roleLabel = new JLabel("Role: Administrator");
        roleLabel.setFont(ThemeConstants.FONT_SMALL);
        roleLabel.setForeground(ThemeConstants.TEXT_MUTED);

        userPill.add(userLabel, BorderLayout.NORTH);
        userPill.add(roleLabel, BorderLayout.SOUTH);
        brandPanel.add(userPill);
        brandPanel.add(Box.createVerticalStrut(24));

        JPanel navMenu = new JPanel();
        navMenu.setOpaque(false);
        navMenu.setLayout(new BoxLayout(navMenu, BoxLayout.Y_AXIS));

        navDashboardBtn = createNavButton("📊  Dashboard Overview", true);
        navSlotsBtn = createNavButton("🚗  Parking Slots (Mod A)", false);
        navReservationsBtn = createNavButton("📋  Reservations (Mod B)", false);

        navDashboardBtn.addActionListener(e -> switchView("DASHBOARD"));
        navSlotsBtn.addActionListener(e -> switchView("SLOTS"));
        navReservationsBtn.addActionListener(e -> switchView("RESERVATIONS"));

        navMenu.add(navDashboardBtn);
        navMenu.add(Box.createVerticalStrut(8));
        navMenu.add(navSlotsBtn);
        navMenu.add(Box.createVerticalStrut(8));
        navMenu.add(navReservationsBtn);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(navMenu, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JButton refreshBtn = ThemeConstants.createOutlineButton("🔄  Refresh Data", new Color(180, 210, 200));
        refreshBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        refreshBtn.addActionListener(e -> refreshAllData());

        JButton logoutBtn = ThemeConstants.createStyledButton("🚪  Log Out", new Color(160, 50, 40), Color.WHITE, false);
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        logoutBtn.addActionListener(e -> handleLogout());

        bottomPanel.add(refreshBtn);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(logoutBtn);

        sidebar.add(bottomPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton createNavButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected() || getModel().isPressed()) {
                    g2.setColor(ThemeConstants.BRAND_TEAL_ACCENT);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 25));
                } else {
                    g2.setColor(new Color(0, 0, 0, 0));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(ThemeConstants.FONT_BODY_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(208, 40));
        btn.setMargin(new Insets(8, 14, 8, 14));
        btn.setSelected(active);
        return btn;
    }

    private void updateNavSelection(String activeKey) {
        navDashboardBtn.setSelected("DASHBOARD".equals(activeKey));
        navSlotsBtn.setSelected("SLOTS".equals(activeKey));
        navReservationsBtn.setSelected("RESERVATIONS".equals(activeKey));

        navDashboardBtn.repaint();
        navSlotsBtn.repaint();
        navReservationsBtn.repaint();
    }

    private JPanel createMainArea() {
        JPanel mainArea = new JPanel(new BorderLayout(0, 0));
        mainArea.setBackground(ThemeConstants.BG_MAIN);

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(Color.WHITE);
        topHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConstants.BORDER_LIGHT),
            new EmptyBorder(14, 24, 14, 24)
        ));

        sectionHeaderLabel = new JLabel("DASHBOARD OVERVIEW");
        sectionHeaderLabel.setFont(ThemeConstants.FONT_HEADER_LARGE);
        sectionHeaderLabel.setForeground(ThemeConstants.BRAND_FOREST_MED);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);

        dbStatusLabel = new JLabel("● Database: Checking...");
        dbStatusLabel.setFont(ThemeConstants.FONT_BODY_BOLD);
        dbStatusLabel.setForeground(ThemeConstants.TEXT_MUTED);

        JButton quickReserveBtn = ThemeConstants.createStyledButton("+ Reserve Slot", ThemeConstants.BRAND_TEAL_ACCENT, Color.WHITE, true);
        quickReserveBtn.addActionListener(e -> slotPanel.showReserveDialog(-1));

        headerRight.add(dbStatusLabel);
        headerRight.add(quickReserveBtn);

        topHeader.add(sectionHeaderLabel, BorderLayout.WEST);
        topHeader.add(headerRight, BorderLayout.EAST);

        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setOpaque(false);
        contentContainer.setBorder(new EmptyBorder(18, 24, 20, 24));

        contentContainer.add(createDashboardView(), "DASHBOARD");
        contentContainer.add(slotPanel, "SLOTS");
        contentContainer.add(reservationPanel, "RESERVATIONS");

        mainArea.add(topHeader, BorderLayout.NORTH);
        mainArea.add(contentContainer, BorderLayout.CENTER);
        return mainArea;
    }

    private void switchView(String key) {
        cardLayout.show(contentContainer, key);
        updateNavSelection(key);

        switch (key) {
            case "DASHBOARD":
                sectionHeaderLabel.setText("DASHBOARD OVERVIEW");
                break;
            case "SLOTS":
                sectionHeaderLabel.setText("PARKING SLOT MANAGEMENT");
                break;
            case "RESERVATIONS":
                sectionHeaderLabel.setText("RESERVATION MANAGEMENT");
                break;
        }
    }

    private JPanel createDashboardView() {
        JPanel view = new JPanel(new BorderLayout(0, 16));
        view.setOpaque(false);

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);

        totalSlotsVal = new JLabel("0", SwingConstants.CENTER);
        availSlotsVal = new JLabel("0", SwingConstants.CENTER);
        occupiedSlotsVal = new JLabel("0", SwingConstants.CENTER);
        activeReservationsVal = new JLabel("0", SwingConstants.CENTER);

        statsPanel.add(createStatMetricCard("TOTAL SLOTS", totalSlotsVal, ThemeConstants.STAT_TOTAL_BG, "All designated bays"));
        statsPanel.add(createStatMetricCard("AVAILABLE SLOTS", availSlotsVal, ThemeConstants.STAT_AVAIL_BG, "Ready for parking"));
        statsPanel.add(createStatMetricCard("OCCUPIED SLOTS", occupiedSlotsVal, ThemeConstants.STAT_OCCUPIED_BG, "Currently in use"));
        statsPanel.add(createStatMetricCard("ACTIVE RESERVATIONS", activeReservationsVal, ThemeConstants.BRAND_FOREST_MED, "Ongoing bookings"));

        JPanel centerSplit = new JPanel(new GridLayout(1, 2, 16, 0));
        centerSplit.setOpaque(false);

        // Left: Visual Parking Lot Map
        JPanel mapCard = new JPanel(new BorderLayout(0, 10));
        mapCard.setBackground(ThemeConstants.BG_CARD);
        mapCard.setBorder(ThemeConstants.createCardBorder());

        JLabel mapTitle = new JLabel("Visual Parking Lot Layout");
        mapTitle.setFont(ThemeConstants.FONT_HEADER_MEDIUM);
        mapTitle.setForeground(ThemeConstants.TEXT_PRIMARY);

        JLabel mapSubtitle = new JLabel("Real-time visual occupancy status of parking bays");
        mapSubtitle.setFont(ThemeConstants.FONT_SMALL);
        mapSubtitle.setForeground(ThemeConstants.TEXT_SECONDARY);

        JPanel mapHeader = new JPanel(new BorderLayout());
        mapHeader.setOpaque(false);
        mapHeader.add(mapTitle, BorderLayout.NORTH);
        mapHeader.add(mapSubtitle, BorderLayout.SOUTH);

        slotGridPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        slotGridPanel.setOpaque(false);
        JScrollPane gridScroll = new JScrollPane(slotGridPanel);
        gridScroll.setBorder(null);
        gridScroll.setOpaque(false);
        gridScroll.getViewport().setOpaque(false);

        mapCard.add(mapHeader, BorderLayout.NORTH);
        mapCard.add(gridScroll, BorderLayout.CENTER);

        // Right: Recent Activity Table
        JPanel recentCard = new JPanel(new BorderLayout(0, 10));
        recentCard.setBackground(ThemeConstants.BG_CARD);
        recentCard.setBorder(ThemeConstants.createCardBorder());

        JLabel recentTitle = new JLabel("Recent Reservations");
        recentTitle.setFont(ThemeConstants.FONT_HEADER_MEDIUM);
        recentTitle.setForeground(ThemeConstants.TEXT_PRIMARY);

        recentReservationsTableModel = new DefaultTableModel(
            new String[]{"ID", "Customer", "Slot", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        recentReservationsTable = new JTable(recentReservationsTableModel);
        styleRecentTable(recentReservationsTable);

        JScrollPane recentScroll = new JScrollPane(recentReservationsTable);
        recentScroll.setBorder(BorderFactory.createLineBorder(ThemeConstants.BORDER_LIGHT, 1));

        recentCard.add(recentTitle, BorderLayout.NORTH);
        recentCard.add(recentScroll, BorderLayout.CENTER);

        centerSplit.add(mapCard);
        centerSplit.add(recentCard);

        view.add(statsPanel, BorderLayout.NORTH);
        view.add(centerSplit, BorderLayout.CENTER);
        return view;
    }

    private JPanel createStatMetricCard(String title, JLabel valueLabel, Color accentBg, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(ThemeConstants.BG_CARD);
        card.setBorder(ThemeConstants.createCardBorder());

        JPanel topStrip = new JPanel(new BorderLayout());
        topStrip.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(ThemeConstants.TEXT_SECONDARY);

        JPanel colorDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentBg);
                g2.fillOval(0, 0, 10, 10);
                g2.dispose();
            }
        };
        colorDot.setPreferredSize(new Dimension(10, 10));
        colorDot.setOpaque(false);

        topStrip.add(titleLbl, BorderLayout.WEST);
        topStrip.add(colorDot, BorderLayout.EAST);

        valueLabel.setFont(ThemeConstants.FONT_STAT_NUMBER);
        valueLabel.setForeground(ThemeConstants.TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(ThemeConstants.FONT_SMALL);
        subLbl.setForeground(ThemeConstants.TEXT_MUTED);

        card.add(topStrip, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    public void refreshAllData() {
        boolean connected = DatabaseConnection.testConnection();

        if (!connected) {
            dbStatusLabel.setText("● Database: Offline (Check db.properties)");
            dbStatusLabel.setForeground(ThemeConstants.BADGE_OCC_TXT);
            JOptionPane.showMessageDialog(
                this,
                "Unable to connect to the MySQL database at localhost:3306.\n\n" +
                "Please verify:\n" +
                "1. MySQL service is running.\n" +
                "2. 'db.properties' has the correct database password.\n" +
                "3. The 'smart_parking' database exists (run schema.sql).",
                "Database Connection Error",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        dbStatusLabel.setText("● Database: Online (MySQL 3306)");
        dbStatusLabel.setForeground(ThemeConstants.STAT_AVAIL_BG);

        // Refresh child module panels
        slotPanel.refreshData();
        reservationPanel.refreshData();

        refreshGlobalMetrics();
    }

    private void refreshGlobalMetrics() {
        if (!DatabaseConnection.testConnection()) {
            return;
        }

        try {
            int[] stats = slotService.getSlotStatistics();
            totalSlotsVal.setText(String.valueOf(stats[0]));
            availSlotsVal.setText(String.valueOf(stats[1]));
            occupiedSlotsVal.setText(String.valueOf(stats[2]));

            List<ParkingSlot> slots = slotService.getAllSlots();
            slotGridPanel.removeAll();
            for (ParkingSlot s : slots) {
                slotGridPanel.add(createSlotGridCard(s));
            }
            slotGridPanel.revalidate();
            slotGridPanel.repaint();

        } catch (SQLException ignored) {}

        try {
            List<Reservation> reservations = reservationService.getAllReservations();
            recentReservationsTableModel.setRowCount(0);
            int activeCount = 0;

            for (int i = 0; i < reservations.size(); i++) {
                Reservation r = reservations.get(i);
                if (r.isActive()) {
                    activeCount++;
                }
                if (i < 6) {
                    recentReservationsTableModel.addRow(new Object[]{
                        r.getReservationId(),
                        r.getCustomerName(),
                        r.getSlotName(),
                        r.getStatus().toUpperCase()
                    });
                }
            }
            activeReservationsVal.setText(String.valueOf(activeCount));

        } catch (SQLException ignored) {}
    }

    private JPanel createSlotGridCard(ParkingSlot slot) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(slot.isAvailable() ? ThemeConstants.BADGE_AVAIL_BG : ThemeConstants.BADGE_OCC_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(slot.isAvailable() ? new Color(160, 220, 190) : new Color(245, 185, 175), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel nameLabel = new JLabel(slot.getSlotName(), SwingConstants.CENTER);
        nameLabel.setFont(ThemeConstants.FONT_HEADER_SMALL);
        nameLabel.setForeground(slot.isAvailable() ? ThemeConstants.BADGE_AVAIL_TXT : ThemeConstants.BADGE_OCC_TXT);

        JLabel statusLabel = new JLabel(slot.isAvailable() ? "AVAILABLE" : "OCCUPIED", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusLabel.setForeground(slot.isAvailable() ? ThemeConstants.BADGE_AVAIL_TXT : ThemeConstants.BADGE_OCC_TXT);

        card.add(nameLabel, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);

        if (slot.isAvailable()) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    slotPanel.showReserveDialog(slot.getSlotId());
                }
            });
        }
        return card;
    }

    private void styleRecentTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(ThemeConstants.FONT_BODY_REGULAR);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 32));
        header.setFont(ThemeConstants.FONT_BODY_BOLD);
        header.setBackground(new Color(238, 243, 240));
        header.setForeground(ThemeConstants.BRAND_FOREST_MED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConstants.BORDER_LIGHT));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
                lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 252, 251));

                String text = val != null ? val.toString() : "";
                if ("ACTIVE".equalsIgnoreCase(text)) {
                    lbl.setForeground(ThemeConstants.BADGE_AVAIL_TXT);
                    lbl.setFont(ThemeConstants.FONT_BODY_BOLD);
                } else if ("CANCELLED".equalsIgnoreCase(text)) {
                    lbl.setForeground(ThemeConstants.BADGE_CANC_TXT);
                    lbl.setFont(ThemeConstants.FONT_BODY_BOLD);
                } else {
                    lbl.setForeground(ThemeConstants.TEXT_PRIMARY);
                    lbl.setFont(ThemeConstants.FONT_BODY_REGULAR);
                }
                return lbl;
            }
        });
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out of the console?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            javax.swing.SwingUtilities.invokeLater(() -> {
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            });
        }
    }
}

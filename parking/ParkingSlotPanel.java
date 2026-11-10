package parking;

import core.ThemeConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Module A — Parking Slot Management Graphical User Interface Panel.
 * Encapsulates table display, status badges, and reservation creation dialog.
 */
public class ParkingSlotPanel extends JPanel {

    private final ParkingSlotManagement slotService;
    private final Runnable onDataChanged;

    private DefaultTableModel slotsTableModel;
    private JTable slotsTable;

    public ParkingSlotPanel(ParkingSlotManagement slotService, Runnable onDataChanged) {
        this.slotService = slotService;
        this.onDataChanged = onDataChanged;

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        // Header Action Bar
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);
        JLabel desc = new JLabel("All parking bays registered in the smart parking database.");
        desc.setFont(ThemeConstants.FONT_BODY_REGULAR);
        desc.setForeground(ThemeConstants.TEXT_SECONDARY);
        leftActions.add(desc);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton reserveBtn = ThemeConstants.createStyledButton("+ Reserve Slot", ThemeConstants.BRAND_TEAL_ACCENT, Color.WHITE, true);
        reserveBtn.addActionListener(e -> showReserveDialog(-1));

        JButton refreshBtn = ThemeConstants.createOutlineButton("🔄 Refresh Slots", ThemeConstants.BRAND_FOREST_MED);
        refreshBtn.addActionListener(e -> refreshData());

        rightActions.add(reserveBtn);
        rightActions.add(refreshBtn);

        actionBar.add(leftActions, BorderLayout.WEST);
        actionBar.add(rightActions, BorderLayout.EAST);

        // Table Panel
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeConstants.BG_CARD);
        tableCard.setBorder(ThemeConstants.createCardBorder());

        slotsTableModel = new DefaultTableModel(
            new String[]{"Slot ID", "Slot Name", "Availability Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        slotsTable = new JTable(slotsTableModel);
        styleModernTable(slotsTable);

        JScrollPane scroll = new JScrollPane(slotsTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeConstants.BORDER_LIGHT, 1));
        tableCard.add(scroll, BorderLayout.CENTER);

        add(actionBar, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            List<ParkingSlot> slots = slotService.getAllSlots();
            slotsTableModel.setRowCount(0);

            for (ParkingSlot s : slots) {
                slotsTableModel.addRow(new Object[]{
                    s.getSlotId(),
                    s.getSlotName(),
                    s.isAvailable() ? "AVAILABLE" : "OCCUPIED"
                });
            }

            if (onDataChanged != null) {
                onDataChanged.run();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error retrieving parking slot data:\n" + e.getMessage(),
                "SQL Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void showReserveDialog(int preselectedSlotId) {
        List<ParkingSlot> availableSlots;
        try {
            availableSlots = slotService.getAvailableSlots();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to fetch available slots:\n" + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (availableSlots.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No parking slots are currently available. All bays are occupied.",
                "Parking Full",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JTextField nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(240, 32));

        JComboBox<ParkingSlot> slotCombo = new JComboBox<>(
            new DefaultComboBoxModel<>(availableSlots.toArray(new ParkingSlot[0]))
        );
        slotCombo.setPreferredSize(new Dimension(240, 32));

        if (preselectedSlotId > 0) {
            for (int i = 0; i < availableSlots.size(); i++) {
                if (availableSlots.get(i).getSlotId() == preselectedSlotId) {
                    slotCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 14));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel nameLbl = new JLabel("Customer Name:");
        nameLbl.setFont(ThemeConstants.FONT_BODY_BOLD);
        JLabel slotLbl = new JLabel("Select Available Slot:");
        slotLbl.setFont(ThemeConstants.FONT_BODY_BOLD);

        form.add(nameLbl);
        form.add(nameField);
        form.add(slotLbl);
        form.add(slotCombo);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Reserve a Parking Slot",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String customer = nameField.getText().trim();
        if (customer.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Customer name cannot be empty. Please provide a valid customer name.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ParkingSlot selectedSlot = (ParkingSlot) slotCombo.getSelectedItem();
        if (selectedSlot == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select an available parking slot.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            int newReservationId = slotService.reserveSlotTransactional(customer, selectedSlot.getSlotId());
            JOptionPane.showMessageDialog(
                this,
                "Reservation confirmed successfully!\n\n" +
                "• Reservation ID: #" + newReservationId + "\n" +
                "• Customer: " + customer + "\n" +
                "• Slot: " + selectedSlot.getSlotName() + "\n" +
                "• Status: ACTIVE",
                "Reservation Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
            refreshData();

        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Reservation Failed",
                JOptionPane.WARNING_MESSAGE
            );
            refreshData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Database error while saving reservation:\n" + e.getMessage(),
                "SQL Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void styleModernTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(ThemeConstants.FONT_BODY_REGULAR);
        table.setSelectionBackground(new Color(225, 240, 233));
        table.setSelectionForeground(ThemeConstants.TEXT_PRIMARY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 36));
        header.setFont(ThemeConstants.FONT_BODY_BOLD);
        header.setBackground(new Color(238, 243, 240));
        header.setForeground(ThemeConstants.BRAND_FOREST_MED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConstants.BORDER_LIGHT));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBorder(new EmptyBorder(0, 12, 0, 12));

                if (!sel) {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 252, 251));
                }

                String text = val != null ? val.toString() : "";
                if ("AVAILABLE".equalsIgnoreCase(text)) {
                    lbl.setForeground(ThemeConstants.BADGE_AVAIL_TXT);
                    lbl.setFont(ThemeConstants.FONT_BODY_BOLD);
                } else if ("OCCUPIED".equalsIgnoreCase(text)) {
                    lbl.setForeground(ThemeConstants.BADGE_OCC_TXT);
                    lbl.setFont(ThemeConstants.FONT_BODY_BOLD);
                } else {
                    lbl.setForeground(ThemeConstants.TEXT_PRIMARY);
                    lbl.setFont(ThemeConstants.FONT_BODY_REGULAR);
                }

                return lbl;
            }
        });
    }
}

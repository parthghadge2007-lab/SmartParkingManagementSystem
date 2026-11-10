package reservation;

import core.ThemeConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Module B — Reservation Management Graphical User Interface Panel.
 * Encapsulates table display, status badges, and reservation cancellation with confirmation.
 */
public class ReservationPanel extends JPanel {

    private final ReservationManagement reservationService;
    private final Runnable onDataChanged;

    private DefaultTableModel reservationsTableModel;
    private JTable reservationsTable;

    public ReservationPanel(ReservationManagement reservationService, Runnable onDataChanged) {
        this.reservationService = reservationService;
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
        JLabel desc = new JLabel("Inspect and manage customer parking reservations.");
        desc.setFont(ThemeConstants.FONT_BODY_REGULAR);
        desc.setForeground(ThemeConstants.TEXT_SECONDARY);
        leftActions.add(desc);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton cancelBtn = ThemeConstants.createStyledButton("✕ Cancel Reservation", ThemeConstants.BADGE_OCC_TXT, Color.WHITE, false);
        cancelBtn.addActionListener(e -> handleCancelReservation());

        JButton refreshBtn = ThemeConstants.createOutlineButton("🔄 Refresh", ThemeConstants.BRAND_FOREST_MED);
        refreshBtn.addActionListener(e -> refreshData());

        rightActions.add(cancelBtn);
        rightActions.add(refreshBtn);

        actionBar.add(leftActions, BorderLayout.WEST);
        actionBar.add(rightActions, BorderLayout.EAST);

        // Table Panel
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeConstants.BG_CARD);
        tableCard.setBorder(ThemeConstants.createCardBorder());

        reservationsTableModel = new DefaultTableModel(
            new String[]{"Reservation ID", "Customer Name", "Parking Slot", "Date & Time", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        reservationsTable = new JTable(reservationsTableModel);
        styleModernTable(reservationsTable);

        JScrollPane scroll = new JScrollPane(reservationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeConstants.BORDER_LIGHT, 1));
        tableCard.add(scroll, BorderLayout.CENTER);

        add(actionBar, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            List<Reservation> reservations = reservationService.getAllReservations();
            reservationsTableModel.setRowCount(0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (Reservation r : reservations) {
                String dateStr = r.getReservationDate() != null ? sdf.format(r.getReservationDate()) : "N/A";
                reservationsTableModel.addRow(new Object[]{
                    r.getReservationId(),
                    r.getCustomerName(),
                    r.getSlotName(),
                    dateStr,
                    r.getStatus().toUpperCase()
                });
            }

            if (onDataChanged != null) {
                onDataChanged.run();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error retrieving reservation data:\n" + e.getMessage(),
                "SQL Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void handleCancelReservation() {
        int selectedRow = reservationsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a reservation record from the table to cancel.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int reservationId = (int) reservationsTableModel.getValueAt(selectedRow, 0);
        String customerName = (String) reservationsTableModel.getValueAt(selectedRow, 1);
        String slotName = (String) reservationsTableModel.getValueAt(selectedRow, 2);
        String status = (String) reservationsTableModel.getValueAt(selectedRow, 4);

        if (!"ACTIVE".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(
                this,
                "Reservation #" + reservationId + " is already " + status + ".\nOnly ACTIVE reservations can be cancelled.",
                "Cancellation Rejected",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to cancel reservation #" + reservationId + "?\n\n" +
            "• Customer: " + customerName + "\n" +
            "• Slot: " + slotName + "\n\n" +
            "Cancelling will restore parking slot availability immediately.",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            reservationService.cancelReservationTransactional(reservationId);
            JOptionPane.showMessageDialog(
                this,
                "Reservation #" + reservationId + " has been successfully cancelled.\n" +
                "Parking slot " + slotName + " is now AVAILABLE again.",
                "Cancellation Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
            refreshData();

        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Cancellation Failed",
                JOptionPane.WARNING_MESSAGE
            );
            refreshData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Database error during cancellation:\n" + e.getMessage(),
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
}

package parking;

import core.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Module A — Parking Slot Management Business & Service Layer.
 * Handles slot queries, availability checking, and ACID reservation with row-level locks.
 */
public class ParkingSlotManagement {

    // =========================================================================
    // Programmatic / GUI API Methods
    // =========================================================================

    public List<ParkingSlot> getAllSlots() throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT slot_id, slot_name, is_available FROM parking_slots ORDER BY slot_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new ParkingSlot(
                    rs.getInt("slot_id"),
                    rs.getString("slot_name"),
                    rs.getBoolean("is_available")
                ));
            }
        }
        return list;
    }

    public List<ParkingSlot> getAvailableSlots() throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT slot_id, slot_name, is_available FROM parking_slots WHERE is_available = TRUE ORDER BY slot_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new ParkingSlot(
                    rs.getInt("slot_id"),
                    rs.getString("slot_name"),
                    true
                ));
            }
        }
        return list;
    }

    public int[] getSlotStatistics() throws SQLException {
        int total = 0;
        int available = 0;
        String sql = "SELECT is_available FROM parking_slots";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                total++;
                if (rs.getBoolean("is_available")) {
                    available++;
                }
            }
        }
        int occupied = total - available;
        return new int[]{total, available, occupied};
    }

    /**
     * Reserves a slot atomically with pessimistic row lock ('FOR UPDATE').
     */
    public int reserveSlotTransactional(String customerName, int slotId)
            throws SQLException, IllegalArgumentException, IllegalStateException {

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        if (slotId <= 0) {
            throw new IllegalArgumentException("A valid parking slot must be selected.");
        }

        String trimmedName = customerName.trim();

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // 1. Lock slot row and verify availability
                String checkSql = "SELECT is_available FROM parking_slots WHERE slot_id = ? FOR UPDATE";
                boolean isAvailable;

                try (PreparedStatement check = con.prepareStatement(checkSql)) {
                    check.setInt(1, slotId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            throw new IllegalStateException("Parking slot ID " + slotId + " does not exist.");
                        }
                        isAvailable = rs.getBoolean("is_available");
                    }
                }

                if (!isAvailable) {
                    con.rollback();
                    throw new IllegalStateException("Slot " + slotId + " is already occupied. Double-booking prevented.");
                }

                // 2. Mark slot occupied
                String updateSql = "UPDATE parking_slots SET is_available = FALSE WHERE slot_id = ?";
                try (PreparedStatement update = con.prepareStatement(updateSql)) {
                    update.setInt(1, slotId);
                    int updatedRows = update.executeUpdate();
                    if (updatedRows != 1) {
                        con.rollback();
                        throw new SQLException("Failed to update parking slot availability status.");
                    }
                }

                // 3. Create active reservation
                int generatedReservationId = -1;
                String insertSql = "INSERT INTO reservations (customer_name, slot_id, status) VALUES (?, ?, 'ACTIVE')";
                try (PreparedStatement insert = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, trimmedName);
                    insert.setInt(2, slotId);
                    insert.executeUpdate();

                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (keys.next()) {
                            generatedReservationId = keys.getInt(1);
                        } else {
                            con.rollback();
                            throw new SQLException("Failed to retrieve generated reservation ID.");
                        }
                    }
                }

                // 4. Commit transaction
                con.commit();
                return generatedReservationId;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // =========================================================================
    // Legacy CLI Methods
    // =========================================================================

    public void viewSlots() {
        String sql = "SELECT slot_id, slot_name, is_available FROM parking_slots ORDER BY slot_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n--- Parking Slots ---");

            while (rs.next()) {
                System.out.println(
                    "ID: " + rs.getInt("slot_id") +
                    " | Slot: " + rs.getString("slot_name") +
                    " | Status: " +
                    (rs.getBoolean("is_available") ? "Available" : "Occupied")
                );
            }

        } catch (SQLException e) {
            System.out.println("Unable to display slots: " + e.getMessage());
        }
    }

    public void reserveSlot(Scanner sc) {
        System.out.print("Enter customer name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Customer name cannot be empty.");
            return;
        }

        System.out.print("Enter slot ID: ");

        int slotId;
        try {
            slotId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid slot ID.");
            return;
        }

        try {
            int resId = reserveSlotTransactional(name, slotId);
            System.out.println("Reservation successful.");
            System.out.println("Reservation ID: " + resId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Reservation failed: " + e.getMessage());
        }
    }
}

package reservation;

import core.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Module B — Reservation Management Business & Service Layer.
 * Handles reservation records, queries, and ACID cancellation with row locking.
 */
public class ReservationManagement {

    // =========================================================================
    // Programmatic / GUI API Methods
    // =========================================================================

    public List<Reservation> getAllReservations() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql =
            "SELECT r.reservation_id, r.customer_name, r.slot_id, p.slot_name, " +
            "r.reservation_date, r.status " +
            "FROM reservations r " +
            "JOIN parking_slots p ON r.slot_id = p.slot_id " +
            "ORDER BY r.reservation_id DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Reservation(
                    rs.getInt("reservation_id"),
                    rs.getString("customer_name"),
                    rs.getInt("slot_id"),
                    rs.getString("slot_name"),
                    rs.getTimestamp("reservation_date"),
                    rs.getString("status")
                ));
            }
        }
        return list;
    }

    /**
     * Cancels an active reservation atomically with pessimistic row lock ('FOR UPDATE').
     */
    public void cancelReservationTransactional(int reservationId)
            throws SQLException, IllegalArgumentException, IllegalStateException {

        if (reservationId <= 0) {
            throw new IllegalArgumentException("A valid reservation ID must be specified.");
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int slotId;
                String currentStatus;

                // 1. Lock the reservation row and check status
                String checkSql = "SELECT slot_id, status FROM reservations WHERE reservation_id = ? FOR UPDATE";
                try (PreparedStatement check = con.prepareStatement(checkSql)) {
                    check.setInt(1, reservationId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            throw new IllegalStateException("Reservation #" + reservationId + " does not exist.");
                        }
                        slotId = rs.getInt("slot_id");
                        currentStatus = rs.getString("status");
                    }
                }

                if (!"ACTIVE".equalsIgnoreCase(currentStatus)) {
                    con.rollback();
                    throw new IllegalStateException("Reservation #" + reservationId + " is already cancelled. Repeated cancellation prevented.");
                }

                // 2. Mark reservation CANCELLED
                String cancelSql = "UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ? AND status = 'ACTIVE'";
                try (PreparedStatement cancel = con.prepareStatement(cancelSql)) {
                    cancel.setInt(1, reservationId);
                    int rows = cancel.executeUpdate();
                    if (rows != 1) {
                        con.rollback();
                        throw new SQLException("Failed to update reservation status.");
                    }
                }

                // 3. Mark the parking slot as available
                String releaseSql = "UPDATE parking_slots SET is_available = TRUE WHERE slot_id = ?";
                try (PreparedStatement release = con.prepareStatement(releaseSql)) {
                    release.setInt(1, slotId);
                    int rows = release.executeUpdate();
                    if (rows != 1) {
                        con.rollback();
                        throw new SQLException("Failed to release parking slot #" + slotId);
                    }
                }

                // 4. Commit transaction
                con.commit();

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

    public void viewReservations() {
        String sql =
            "SELECT r.reservation_id, r.customer_name, p.slot_name, " +
            "r.reservation_date, r.status " +
            "FROM reservations r " +
            "JOIN parking_slots p ON r.slot_id = p.slot_id " +
            "ORDER BY r.reservation_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n--- Reservations ---");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(
                    "Reservation ID: " + rs.getInt("reservation_id") +
                    " | Customer: " + rs.getString("customer_name") +
                    " | Slot: " + rs.getString("slot_name") +
                    " | Date: " + rs.getTimestamp("reservation_date") +
                    " | Status: " + rs.getString("status")
                );
            }

            if (!found) {
                System.out.println("No reservations found.");
            }

        } catch (SQLException e) {
            System.out.println("Unable to display reservations: " + e.getMessage());
        }
    }

    public void cancelReservation(Scanner sc) {
        System.out.print("Enter reservation ID to cancel: ");

        int reservationId;
        try {
            reservationId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid reservation ID.");
            return;
        }

        try {
            cancelReservationTransactional(reservationId);
            System.out.println("Reservation cancelled successfully.");
            System.out.println("Parking slot is now available.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Cancellation failed: " + e.getMessage());
        }
    }
}

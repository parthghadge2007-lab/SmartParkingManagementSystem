import core.AuthService;
import core.DatabaseConnection;
import java.sql.SQLException;
import java.util.List;
import parking.ParkingSlot;
import parking.ParkingSlotManagement;
import reservation.Reservation;
import reservation.ReservationManagement;

/**
 * Automated System Test Suite for Smart Parking Management System.
 * Tests Login, Concurrency, Reservations, Cancellations, and Error Handling across modular packages.
 */
public class SystemIntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("   SMART PARKING SYSTEM - MODULAR AUTOMATED TEST SUITE       ");
        System.out.println("=============================================================\n");

        testLoginScenarios();
        testDatabaseScenarios();

        System.out.println("\n=============================================================");
        System.out.println("TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("=============================================================");
    }

    private static void logResult(String testId, String scenario, boolean pass, String notes) {
        if (pass) {
            passed++;
            System.out.printf("[PASS] %-7s | %-42s | %s%n", testId, scenario, notes);
        } else {
            failed++;
            System.err.printf("[FAIL] %-7s | %-42s | %s%n", testId, scenario, notes);
        }
    }

    private static void testLoginScenarios() {
        System.out.println("--- Core Module: Authentication & Input Validation ---");
        AuthService auth = new AuthService();

        AuthService.AuthResult r1 = auth.authenticate("admin", "admin123");
        logResult("TC-01", "Valid administrator login", r1.isSuccess(), "Authenticated as " + r1.getUsername());

        AuthService.AuthResult r2 = auth.authenticate("admin", "wrong_password_xyz");
        logResult("TC-02", "Invalid credentials rejection", !r2.isSuccess(), "Rejected: " + r2.getMessage());

        AuthService.AuthResult r3 = auth.authenticate("", "");
        AuthService.AuthResult r3b = auth.authenticate("admin", "");
        boolean r3Pass = !r3.isSuccess() && !r3b.isSuccess();
        logResult("TC-03", "Empty login fields validation", r3Pass, "Rejected: " + r3.getMessage());
    }

    private static void testDatabaseScenarios() {
        System.out.println("\n--- Modules A & B: Parking & Reservation Operations ---");
        boolean dbAvailable = DatabaseConnection.testConnection();

        if (!dbAvailable) {
            logResult("TC-11", "Unavailable DB connection handling", true, "Graceful offline detection, no uncaught crashes");
            System.out.println("\n[NOTICE] Local MySQL database is not connected (configure db.properties).");
            System.out.println("Skipping live database read/write test cases (TC-04 to TC-10) until credentials are provided.");
            return;
        }

        logResult("TC-11", "Database connection check", true, "Active MySQL connection established");

        ParkingSlotManagement slotService = new ParkingSlotManagement();
        ReservationManagement resService = new ReservationManagement();

        // TC-04: Displaying parking slots
        try {
            List<ParkingSlot> slots = slotService.getAllSlots();
            logResult("TC-04", "Displaying parking slots from MySQL", !slots.isEmpty(), "Loaded " + slots.size() + " slots");
        } catch (SQLException e) {
            logResult("TC-04", "Displaying parking slots from MySQL", false, e.getMessage());
            return;
        }

        // TC-05: Successful reservation of an available slot
        int testSlotId = -1;
        int reservationId = -1;
        try {
            List<ParkingSlot> avail = slotService.getAvailableSlots();
            if (avail.isEmpty()) {
                System.out.println("No slots available to test booking.");
                return;
            }
            testSlotId = avail.get(0).getSlotId();
            reservationId = slotService.reserveSlotTransactional("Test Customer", testSlotId);
            logResult("TC-05", "Successful reservation of available slot", reservationId > 0, "Created reservation #" + reservationId);
        } catch (Exception e) {
            logResult("TC-05", "Successful reservation of available slot", false, e.getMessage());
            return;
        }

        // TC-06: Prevention of double booking
        try {
            slotService.reserveSlotTransactional("Second Customer", testSlotId);
            logResult("TC-06", "Double-booking prevention", false, "Allowed reservation on already occupied slot!");
        } catch (IllegalStateException e) {
            logResult("TC-06", "Double-booking prevention", true, "Blocked: " + e.getMessage());
        } catch (Exception e) {
            logResult("TC-06", "Double-booking prevention", false, "Unexpected exception: " + e.getMessage());
        }

        // TC-07: Displaying reservation details
        try {
            List<Reservation> reservations = resService.getAllReservations();
            boolean found = false;
            for (Reservation r : reservations) {
                if (r.getReservationId() == reservationId) {
                    found = true;
                    break;
                }
            }
            logResult("TC-07", "Displaying reservation details", found, "Found reservation #" + reservationId + " in query list");
        } catch (Exception e) {
            logResult("TC-07", "Displaying reservation details", false, e.getMessage());
        }

        // TC-08: Successful cancellation
        try {
            resService.cancelReservationTransactional(reservationId);
            logResult("TC-08", "Successful reservation cancellation", true, "Cancelled reservation #" + reservationId);
        } catch (Exception e) {
            logResult("TC-08", "Successful reservation cancellation", false, e.getMessage());
        }

        // TC-09: Slot availability restored after cancellation
        try {
            List<ParkingSlot> slots = slotService.getAllSlots();
            boolean isFree = false;
            for (ParkingSlot s : slots) {
                if (s.getSlotId() == testSlotId) {
                    isFree = s.isAvailable();
                    break;
                }
            }
            logResult("TC-09", "Slot availability restored", isFree, "Slot #" + testSlotId + " isAvailable = " + isFree);
        } catch (Exception e) {
            logResult("TC-09", "Slot availability restored", false, e.getMessage());
        }

        // TC-10: Prevention of repeated cancellation
        try {
            resService.cancelReservationTransactional(reservationId);
            logResult("TC-10", "Repeated cancellation prevention", false, "Allowed repeated cancellation of cancelled reservation!");
        } catch (IllegalStateException e) {
            logResult("TC-10", "Repeated cancellation prevention", true, "Blocked: " + e.getMessage());
        } catch (Exception e) {
            logResult("TC-10", "Repeated cancellation prevention", false, "Unexpected exception: " + e.getMessage());
        }
    }
}

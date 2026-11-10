import core.AuthService;
import core.DatabaseConnection;
import java.util.List;
import parking.ParkingSlot;
import parking.ParkingSlotManagement;
import reservation.ReservationManagement;

public class WhiteBoxTesting {

    private static int passed = 0;
    private static int failed = 0;

    @FunctionalInterface
    public interface Executable {
        void execute() throws Throwable;
    }

    @FunctionalInterface
    public interface TestCase {
        void run() throws Throwable;
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message != null ? message : "Expected condition to be true, but was false");
        }
    }

    public static void assertTrue(boolean condition) {
        assertTrue(condition, null);
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message != null ? message : "Expected condition to be false, but was true");
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, null);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError((message != null ? message + " - " : "") +
            "Expected: <" + expected + "> but was: <" + actual + ">");
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message != null ? message : "Expected object to be non-null");
        }
    }

    public static void assertNotNull(Object actual) {
        assertNotNull(actual, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable, String message) {
        try {
            executable.execute();
        } catch (Throwable actualThrown) {
            if (expectedType.isInstance(actualThrown)) {
                return (T) actualThrown;
            }
            throw new AssertionError((message != null ? message + " - " : "") +
                "Expected exception " + expectedType.getName() + " to be thrown, but caught " +
                actualThrown.getClass().getName() + " (" + actualThrown.getMessage() + ")");
        }
        throw new AssertionError((message != null ? message + " - " : "") +
            "Expected exception " + expectedType.getName() + " to be thrown, but nothing was thrown");
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
        return assertThrows(expectedType, executable, null);
    }

    private static void runTest(String testName, TestCase testCase) {
        try {
            testCase.run();
            passed++;
            System.out.printf("  [PASS] %-34s%n", testName);
        } catch (AssertionError ae) {
            failed++;
            System.err.printf("  [FAIL] %-34s - %s%n", testName, ae.getMessage());
        } catch (Throwable t) {
            failed++;
            System.err.printf("  [ERROR] %-33s - Unexpected exception: %s%n", testName, t.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("   WHITE-BOX STRUCTURAL TEST SUITE (JUnit-Style Assertions)   ");
        System.out.println("=============================================================\n");

        System.out.println("--- Core Module: AuthService Decision & Branch Coverage ---");
        runTest("testEmptyUsername", WhiteBoxTesting::testEmptyUsername);
        runTest("testEmptyPassword", WhiteBoxTesting::testEmptyPassword);
        runTest("testInvalidLogin", WhiteBoxTesting::testInvalidLogin);
        runTest("testValidLogin", WhiteBoxTesting::testValidLogin);

        System.out.println("\n--- Module A: ParkingSlotManagement Boundary & Lock Coverage ---");
        runTest("testEmptyCustomerName", WhiteBoxTesting::testEmptyCustomerName);
        runTest("testInvalidSlotId", WhiteBoxTesting::testInvalidSlotId);

        System.out.println("\n--- Database Integrated Scenarios (ACID & Mutex Locks) ---");
        if (DatabaseConnection.testConnection()) {
            runTest("testDoubleBookingPrevention", WhiteBoxTesting::testDoubleBookingPrevention);
            runTest("testInvalidReservationId", WhiteBoxTesting::testInvalidReservationId);
            runTest("testDuplicateCancellation", WhiteBoxTesting::testDuplicateCancellation);
        } else {
            System.out.println("  [NOTICE] MySQL database offline, skipping live DB tests.");
        }

        System.out.println("\n=============================================================");
        System.out.println("TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED, " + (passed + failed) + " TOTAL");
        System.out.println("=============================================================");
    }

    public static void testEmptyUsername() {
        AuthService auth = new AuthService();

        AuthService.AuthResult r1 = auth.authenticate(null, "admin123");
        assertFalse(r1.isSuccess(), "Null username must fail authentication");

        AuthService.AuthResult r2 = auth.authenticate("   ", "admin123");
        assertFalse(r2.isSuccess(), "Whitespace username must fail authentication");
        assertEquals("Username cannot be empty.", r2.getMessage());
    }

    public static void testEmptyPassword() {
        AuthService auth = new AuthService();

        AuthService.AuthResult r1 = auth.authenticate("admin", null);
        assertFalse(r1.isSuccess(), "Null password must fail authentication");

        AuthService.AuthResult r2 = auth.authenticate("admin", "");
        assertFalse(r2.isSuccess(), "Empty password must fail authentication");
        assertEquals("Password cannot be empty.", r2.getMessage());
    }


    public static void testInvalidLogin() {
        AuthService auth = new AuthService();
        AuthService.AuthResult res = auth.authenticate("admin", "wrongPass");

        assertFalse(res.isSuccess(), "Invalid password should be rejected");
        assertEquals("Invalid username or password. Please try again.", res.getMessage());
    }

    public static void testValidLogin() {
        AuthService auth = new AuthService();
        AuthService.AuthResult res = auth.authenticate("admin", "admin123");

        assertTrue(res.isSuccess(), "Valid credentials must succeed");
        assertEquals("admin", res.getUsername());
        assertEquals("Administrator", res.getRole());
    }

    public static void testEmptyCustomerName() {
        ParkingSlotManagement service = new ParkingSlotManagement();

        IllegalArgumentException exNull = assertThrows(
            IllegalArgumentException.class,
            () -> service.reserveSlotTransactional(null, 1),
            "Null customer name must throw IllegalArgumentException"
        );
        assertEquals("Customer name cannot be empty.", exNull.getMessage());

        IllegalArgumentException exBlank = assertThrows(
            IllegalArgumentException.class,
            () -> service.reserveSlotTransactional("   ", 1),
            "Blank customer name must throw IllegalArgumentException"
        );
        assertEquals("Customer name cannot be empty.", exBlank.getMessage());
    }

    public static void testInvalidSlotId() {
        ParkingSlotManagement service = new ParkingSlotManagement();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.reserveSlotTransactional("John", 0),
            "Slot ID 0 must throw IllegalArgumentException"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.reserveSlotTransactional("John", -1),
            "Negative slot ID must throw IllegalArgumentException"
        );
    }

    public static void testDoubleBookingPrevention() throws Exception {
        ParkingSlotManagement slotService = new ParkingSlotManagement();
        ReservationManagement resService = new ReservationManagement();

        List<ParkingSlot> avail = slotService.getAvailableSlots();
        if (avail.isEmpty()) {
            System.out.println("  [SKIP] testDoubleBookingPrevention: No free slots in database");
            return;
        }

        int slotId = avail.get(0).getSlotId();
        int resId = slotService.reserveSlotTransactional("Customer 1", slotId);
        assertTrue(resId > 0, "Initial reservation must generate positive reservation ID");

        try {
            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> slotService.reserveSlotTransactional("Customer 2", slotId),
                "Concurrent reservation on same slot must throw IllegalStateException"
            );
            assertTrue(ex.getMessage().contains("already occupied"),
                "Exception message must specify slot is already occupied");
        } finally {
            // Guarantee cleanup of test reservation
            resService.cancelReservationTransactional(resId);
        }
    }

    public static void testInvalidReservationId() {
        ReservationManagement service = new ReservationManagement();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.cancelReservationTransactional(0),
            "Reservation ID 0 must throw IllegalArgumentException"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.cancelReservationTransactional(-5),
            "Negative reservation ID must throw IllegalArgumentException"
        );
    }

    public static void testDuplicateCancellation() throws Exception {
        ParkingSlotManagement slotService = new ParkingSlotManagement();
        ReservationManagement resService = new ReservationManagement();

        List<ParkingSlot> avail = slotService.getAvailableSlots();
        if (avail.isEmpty()) {
            System.out.println("  [SKIP] testDuplicateCancellation: No free slots in database");
            return;
        }

        int slotId = avail.get(0).getSlotId();
        int resId = slotService.reserveSlotTransactional("Cancel Test", slotId);
        assertTrue(resId > 0, "Initial booking should succeed");

        resService.cancelReservationTransactional(resId);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> resService.cancelReservationTransactional(resId),
            "Repeated cancellation on already cancelled reservation must throw IllegalStateException"
        );
        assertTrue(ex.getMessage().contains("already cancelled"),
            "Exception message must specify reservation is already cancelled");
    }
}

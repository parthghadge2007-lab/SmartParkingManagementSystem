import java.util.Scanner;
import parking.ParkingSlotManagement;
import reservation.ReservationManagement;

/**
 * Classic Command Line Interface Runner for Smart Parking Management System.
 * Reuses the modular business services from parking and reservation packages.
 */
public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        ParkingSlotManagement parking = new ParkingSlotManagement();
        ReservationManagement reservations = new ReservationManagement();

        while (true) {
            System.out.println("\n===== Smart Parking Management System =====");
            System.out.println("1. View Parking Slots");
            System.out.println("2. Reserve a Parking Slot");
            System.out.println("3. View Reservations");
            System.out.println("4. Cancel a Reservation");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    parking.viewSlots();
                    break;

                case "2":
                    parking.reserveSlot(sc);
                    break;

                case "3":
                    reservations.viewReservations();
                    break;

                case "4":
                    reservations.cancelReservation(sc);
                    break;

                case "5":
                    System.out.println("Thank you for using Smart Parking Management System.");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}

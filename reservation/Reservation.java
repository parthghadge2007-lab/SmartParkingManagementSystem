package reservation;

import java.sql.Timestamp;

/**
 * Model representing a customer parking reservation.
 */
public class Reservation {
    private int reservationId;
    private String customerName;
    private int slotId;
    private String slotName;
    private Timestamp reservationDate;
    private String status;

    public Reservation(int reservationId, String customerName, int slotId, String slotName, Timestamp reservationDate, String status) {
        this.reservationId = reservationId;
        this.customerName = customerName;
        this.slotId = slotId;
        this.slotName = slotName;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public Timestamp getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Timestamp reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "#" + reservationId + " | " + customerName + " | Slot: " + slotName + " | Status: " + status;
    }
}

package parking;

/**
 * Model representing a parking slot in the Smart Parking system.
 */
public class ParkingSlot {
    private int slotId;
    private String slotName;
    private boolean isAvailable;

    public ParkingSlot(int slotId, String slotName, boolean isAvailable) {
        this.slotId = slotId;
        this.slotName = slotName;
        this.isAvailable = isAvailable;
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

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getStatus() {
        return isAvailable ? "Available" : "Occupied";
    }

    @Override
    public String toString() {
        return slotId + " - " + slotName + " (" + getStatus() + ")";
    }
}

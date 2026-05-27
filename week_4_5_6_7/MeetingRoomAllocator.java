import java.util.*;

// ═══════════════════════════════════════════
// MODEL CLASSES
// ═══════════════════════════════════════════

class Room {
    int id;
    String name;
    int capacity;

    Room(int id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "Room[" + id + ": " + name + ", cap=" + capacity + "]";
    }
}

class Booking {
    int bookingId;
    int roomId;
    int startTime; // in minutes from midnight e.g. 9:30 → 570
    int endTime;
    String bookedBy;

    Booking(int bookingId, int roomId, int startTime, int endTime, String bookedBy) {
        this.bookingId = bookingId;
        this.roomId    = roomId;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.bookedBy  = bookedBy;
    }

    // Core logic: does this booking overlap with given slot?
    boolean overlaps(int start, int end) {
        return start < this.endTime && end > this.startTime;
    }

    @Override
    public String toString() {
        return "Booking[id=" + bookingId + ", room=" + roomId
             + ", " + toHHMM(startTime) + "-" + toHHMM(endTime)
             + ", by=" + bookedBy + "]";
    }

    static String toHHMM(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}


// ═══════════════════════════════════════════
// CORE SERVICE
// ═══════════════════════════════════════════

class RoomAllocator {

    // roomId → list of bookings for that room
    private Map<Integer, Room>         rooms    = new HashMap<>();
    private Map<Integer, List<Booking>> bookings = new HashMap<>();
    private int bookingCounter = 1;

    // ── Add Room ──────────────────────────
    void addRoom(int id, String name, int capacity) {
        rooms.put(id, new Room(id, name, capacity));
        bookings.put(id, new ArrayList<>());
        System.out.println("Added: " + rooms.get(id));
    }

    // ── Book Room ─────────────────────────
    // Returns bookingId if success, -1 if slot taken
    int bookRoom(int roomId, String startStr, String endStr, String bookedBy) {
        if (!rooms.containsKey(roomId)) {
            System.out.println("Room " + roomId + " not found.");
            return -1;
        }

        int start = toMinutes(startStr);
        int end   = toMinutes(endStr);

        if (start >= end) {
            System.out.println("Invalid time slot.");
            return -1;
        }

        // Check overlap with existing bookings
        for (Booking b : bookings.get(roomId)) {
            if (b.overlaps(start, end)) {
                System.out.println("Room " + roomId + " already booked during " + startStr + "-" + endStr);
                return -1;
            }
        }

        Booking newBooking = new Booking(bookingCounter++, roomId, start, end, bookedBy);
        bookings.get(roomId).add(newBooking);
        System.out.println("Booked! " + newBooking);
        return newBooking.bookingId;
    }

    // ── Cancel Booking ────────────────────
    void cancelBooking(int bookingId) {
        for (List<Booking> list : bookings.values()) {
            Iterator<Booking> it = list.iterator();
            while (it.hasNext()) {
                Booking b = it.next();
                if (b.bookingId == bookingId) {
                    it.remove();
                    System.out.println("Cancelled: " + b);
                    return;
                }
            }
        }
        System.out.println("Booking " + bookingId + " not found.");
    }

    // ── Available Rooms at a Time Slot ────
    List<Room> getAvailableRooms(String startStr, String endStr) {
        int start = toMinutes(startStr);
        int end   = toMinutes(endStr);
        List<Room> available = new ArrayList<>();

        for (int roomId : rooms.keySet()) {
            boolean isFree = true;
            for (Booking b : bookings.get(roomId)) {
                if (b.overlaps(start, end)) {
                    isFree = false;
                    break;
                }
            }
            if (isFree) available.add(rooms.get(roomId));
        }
        return available;
    }

    // ── View All Bookings for a Room ──────
    void viewBookings(int roomId) {
        if (!bookings.containsKey(roomId)) {
            System.out.println("Room not found.");
            return;
        }
        List<Booking> list = bookings.get(roomId);
        if (list.isEmpty()) {
            System.out.println("No bookings for Room " + roomId);
            return;
        }
        list.forEach(System.out::println);
    }

    // ── Utility: "09:30" → 570 (minutes) ─
    private int toMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}


// ═══════════════════════════════════════════
// MAIN — DRIVER
// ═══════════════════════════════════════════

public class MeetingRoomAllocator {
    public static void main(String[] args) {

        RoomAllocator allocator = new RoomAllocator();

        // Add rooms
        allocator.addRoom(1, "Alpha",   10);
        allocator.addRoom(2, "Beta",    6);
        allocator.addRoom(3, "Gamma",   20);

        System.out.println("\n--- Booking Rooms ---");
        int b1 = allocator.bookRoom(1, "09:00", "10:00", "Pradeep");
        int b2 = allocator.bookRoom(1, "09:30", "10:30", "Ravi");   
        int b3 = allocator.bookRoom(1, "10:00", "11:00", "Sneha");  
        int b4 = allocator.bookRoom(2, "09:00", "10:00", "Amit");

        System.out.println("\n--- Available Rooms at 09:00-10:00 ---");
        List<Room> available = allocator.getAvailableRooms("09:00", "10:00");
        available.forEach(System.out::println);

        System.out.println("\n--- Bookings for Room 1 ---");
        allocator.viewBookings(1);

        System.out.println("\n--- Cancel Booking " + b1 + " ---");
        allocator.cancelBooking(b1);

        System.out.println("\n--- Available Rooms at 09:00-10:00 (after cancel) ---");
        available = allocator.getAvailableRooms("09:00", "10:00");
        available.forEach(System.out::println);
    }
}
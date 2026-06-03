package week_4_5_6_7;
import java.util.*;

// ================= ENUMS =================
enum SeatStatus {
    AVAILABLE, LOCKED, BOOKED
}

enum BookingStatus {
    CREATED, CONFIRMED, CANCELLED
}

// ================= ENTITIES =================
class Movie {
    int id;
    String name;

    public Movie(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

class Theatre {
    int id;
    String name;
    List<Screen> screens = new ArrayList<>();

    public Theatre(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

class Screen {
    int id;
    List<Seat> seats = new ArrayList<>();

    public Screen(int id) {
        this.id = id;
    }
}

class Seat {
    int id;
    int row;
    int col;

    public Seat(int id, int row, int col) {
        this.id = id;
        this.row = row;
        this.col = col;
    }
}

// ================= SHOW =================
class Show {
    int id;
    Movie movie;
    Screen screen;
    Map<Integer, ShowSeat> showSeats = new HashMap<>();

    public Show(int id, Movie movie, Screen screen) {
        this.id = id;
        this.movie = movie;
        this.screen = screen;

        for (Seat seat : screen.seats) {
            showSeats.put(seat.id, new ShowSeat(seat));
        }
    }
}

// ================= SHOW SEAT =================
class ShowSeat {
    Seat seat;
    SeatStatus status;

    public ShowSeat(Seat seat) {
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
    }
}

// ================= BOOKING =================
class Booking {
    int id;
    List<ShowSeat> seats;
    BookingStatus status;

    public Booking(int id, List<ShowSeat> seats) {
        this.id = id;
        this.seats = seats;
        this.status = BookingStatus.CREATED;
    }
}

// ================= REPOSITORY =================
class BookingRepository {
    private static BookingRepository instance = new BookingRepository();
    Map<Integer, Booking> bookings = new HashMap<>();

    private BookingRepository() {}

    public static BookingRepository getInstance() {
        return instance;
    }

    public void save(Booking booking) {
        bookings.put(booking.id, booking);
    }
}

// ================= SERVICE =================
class BookingService {

    private BookingRepository repo = BookingRepository.getInstance();
    private int bookingCounter = 1;

    public synchronized Booking createBooking(Show show, List<Integer> seatIds) {

        List<ShowSeat> selectedSeats = new ArrayList<>();

        // Step 1: Check availability
        for (int seatId : seatIds) {
            ShowSeat ss = show.showSeats.get(seatId);

            if (ss.status != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat not available: " + seatId);
            }
            selectedSeats.add(ss);
        }

        // Step 2: Lock seats
        for (ShowSeat ss : selectedSeats) {
            ss.status = SeatStatus.LOCKED;
        }
        
        // boolean payment = paymentGateway.makePayment();
        // if(payment){
        // }

        Booking booking = new Booking(bookingCounter++, selectedSeats);
        repo.save(booking);
    
        return booking;
    }

    public synchronized void confirmBooking(Booking booking) {
        for (ShowSeat ss : booking.seats) {
            ss.status = SeatStatus.BOOKED;
        }
        booking.status = BookingStatus.CONFIRMED;
    }

    public synchronized void cancelBooking(Booking booking) {
        for (ShowSeat ss : booking.seats) {
            ss.status = SeatStatus.AVAILABLE;
        }
        booking.status = BookingStatus.CANCELLED;
    }
}

// ================= MAIN =================
public class BookingSystem {
    public static void main(String[] args) {

        // Setup
        Movie movie = new Movie(1, "Avengers");

        Screen screen = new Screen(1);
        for (int i = 1; i <= 10; i++) {
            screen.seats.add(new Seat(i, 1, i));
        }

        Show show = new Show(1, movie, screen);

        BookingService service = new BookingService();

        // Booking flow
        Booking booking = service.createBooking(show, Arrays.asList(1, 2, 3));

        System.out.println("Booking created: " + booking.id);

        service.confirmBooking(booking);

        System.out.println("Booking confirmed!");
    }
}



// Seat
//     id
//     row
//     col
// ShowSeat 
//     seat
//     status
// Movie 
// Theatre
// Screen
//     List<Seat>
// Show
//     Movie
//     Screen 
//     List<ShowSeat>

// BookingService
//     createBooking 
//     confirmBooking
//     cancelBooking

// Booking
//     name
//     List<ShowSeat>
//     Status
// BookingRepository
//     save()

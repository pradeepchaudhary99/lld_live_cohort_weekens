package week_4_5_6_7;

import java.util.concurrent.ConcurrentHashMap;

/*
Functional Requirements:
    1. Search for movie by city
    2. view theatres/cinema showing a movie
    3. View available shows
    4. View seat layouts
    5. select the seats/ Book seats
    6. Make Payment 
    7. Cancel Booking 

Non Functional Requirements;
    avoid double booking 
    Idempotency to avoid double payment 
    Fast seat lookup 


Core Entitites:
    Movie 
    Theatre
    Screen
    Show 
    Seat
    ShowSeat
    SeatType 
    User
    Booking
    BookingStatus
    Payment 
    PaymentStatus

    MovieBookingService 



API's 
Search Movies 

List<Movie> searchMovies(String searchWord);
List<Show> getShows(String movieID);
List<ShowSeat> getAvailableSeats(String showId);






*/

class Movie{
    String movieId;
    String title;
    String metadata;
    int duration;
}

class City{
    List<Theatre> allTheatres;
}

class Theatre{
    String threatreId;
    String name;
    List<Screen> screens;
}

class Screen{
    String screenId;
    Map<SeatType, Seat> seatsLayout;
}

enum SeatType{
    REGULAR,
    PREMIUM,
    RECLINER
}

class Show{
    String showId;
    Movie movie;
    Screen screen;
    LocalDateTime startTime;
    LocalDateTime endTime;
    List<ShowSeat> showSeats; // to Show_id -- seatId
}

class ShowSeat{
    String showId;
    Seat seat;
    SeatStatus status;
    double price;
}

enum SeatStatus{
    AVAILABLE,
    LOCKED,
    BOOKED
}


class User{
    String userId;
    String name;
    String email;
}

class Booking{
    String bookingId;
    User user;
    Show show;
    List<ShowSeat> bookedSeats;
    BookingStatus status; 
    Payment payment;
}

enum BookingStatus{
    CREATED,
    CONFIRMED,
    Cancelled
}


class Payment{
    String paymentId;
    double Amount;
    PaymentStatus status;
}

class SeatLock{
    ShowSeat showseat; 
    long expiryTime; //TTL 
}
// Strategy
// --> PaymentStrategy 
// --> SeatPricingStrategy
// Factory ---> PaymentFactory 

// ServiceLayer 

class SeatLockService{                   
                        //SeatID+ShowID
    private final Map<String, SeatLock> locks = new ConcurrentHashMap<>();

    public boolean lockSeat(ShowSeat seat, String userId){
        synchronized(seat){
            SeatLock existingLock = locks.get(seat.seatId);
            if(existingLock != null){
                // return from here. 
            }
            SeatLock lock = new SeatLock(seat);
            locks.put(seatID+showId, lock);
            return true;
        }
    }

    public void unlockSeat(String key){
        locks.remove(key);
    }
}

class BookingService{
    SeatLockService seatLockService;
    PaymentService paymentService;
    



    public Booking bookseats(User user, Show show, List<ShowSeat> showSeat){
        try{
            for(ShowSeat seat : showSeat){
                seatLockService.lockSeat(seat, userId);
            }
            // Take Payment 
            // if Succeed

        }catch()

    }
}



public class MovieBookingSystem {
    
}

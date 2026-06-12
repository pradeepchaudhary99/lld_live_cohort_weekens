package week_4_5_6_7;

import java.sql.Driver;

import week_1.Factory_Design_Pattern;

/*
Functional Requirements
    Rider
        Search for rides
        Request ride
        Cancel ride
        Track ride
        Pay fare
    Driver
        Go online/offline
        Accept/reject ride
        Start trip
        End trip
    System
        Match rider with driver
        Calculate fare
        Track ride lifecycle
        Process payment
        Notify rider and driver


Now See Core Entities: 
User
 ├── Rider
 └── Driver

Ride

Location

Payment

FareStrategy

DriverMatchingStrategy

Managers:
    RideManager
    DriverManager
    PaymentProcessor

Everything starts from: System entry Point
    rideManager.requestRide(
            rider,
            pickupLocation,
            dropLocation
    );

Rider
   |
   v
RideManager
   |
   +---- DriverManager
   |
   +---- MatchingStrategy
   |
   +---- FareStrategy
   |
   +---- PaymentProcessor

Read this:

Rider requests a ride through RideManager.
 The manager calculates the fare using a FareStrategy, fetches available drivers from DriverManager, and uses a DriverMatchingStrategy to assign the best driver. 
 Once the driver accepts, the ride moves through DRIVER_ARRIVING and IN_PROGRESS states. 
 After reaching the destination, the ride is marked COMPLETED, payment is processed, and the ride lifecycle ends. 
 RideManager acts as the central orchestrator while strategies encapsulate fare calculation and driver matching logic.


REQUESTED
    |
    +-----> CANCELLED

REQUESTED
    |
    v
DRIVER_ASSIGNED
    |
    +-----> DRIVER_REJECTED

DRIVER_ASSIGNED
    |
    v
ACCEPTED
    |
    v
DRIVER_ARRIVING
    |
    v
IN_PROGRESS
    |
    v
COMPLETED
    |
    v
PAYMENT_COMPLETED


 */


import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * =========================================================
 * ENUMS
 * =========================================================
 */

enum DriverStatus {
    ONLINE,
    OFFLINE,
    BUSY
}

enum RideStatus {
    REQUESTED,
    DRIVER_ASSIGNED,
    ACCEPTED,
    DRIVER_ARRIVING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}

/*
 * =========================================================
 * LOCATION
 * =========================================================
 */

class Location {

    private final double x;
    private final double y;

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double distance(Location other) {

        double dx = x - other.x;
        double dy = y - other.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}

/*
 * =========================================================
 * RIDER
 * =========================================================
 */

class Rider {

    private final String riderId;
    private final String name;

    public Rider(String riderId, String name) {
        this.riderId = riderId;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

/*
 * =========================================================
 * DRIVER
 * =========================================================
 */

class Driver {

    private final String driverId;
    private final String name;
    private Location location;

    private DriverStatus status =
            DriverStatus.ONLINE;

    public Driver(
            String driverId,
            String name,
            Location location) {

        this.driverId = driverId;
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void assignRide() {
        status = DriverStatus.BUSY;
    }

    public void completeRide() {
        status = DriverStatus.ONLINE;
    }
}

/*
 * =========================================================
 * PAYMENT
 * =========================================================
 */

class Payment {

    private final String paymentId;
    private final double amount;

    private PaymentStatus status =
            PaymentStatus.PENDING;

    public Payment(
            String paymentId,
            double amount) {

        this.paymentId = paymentId;
        this.amount = amount;
    }

    public boolean process() {

        status = PaymentStatus.SUCCESS;

        System.out.println(
                "Payment Success : ₹" + amount
        );

        return true;
    }
}

/*
 * =========================================================
 * RIDE
 * =========================================================
 */

class Ride {

    private final String rideId;

    private final Rider rider;

    private Driver driver;

    private final Location pickup;

    private final Location drop;

    private final double fare;

    private RideStatus status;

    public Ride(
            String rideId,
            Rider rider,
            Location pickup,
            Location drop,
            double fare) {

        this.rideId = rideId;
        this.rider = rider;
        this.pickup = pickup;
        this.drop = drop;
        this.fare = fare;

        this.status =
                RideStatus.REQUESTED;
    }

    public synchronized void updateStatus(
            RideStatus status) {

        this.status = status;

        System.out.println(
                "Ride " + rideId +
                        " -> " + status
        );
    }

    public Location getPickup() {
        return pickup;
    }

    public double getFare() {
        return fare;
    }

    public void assignDriver(
            Driver driver) {

        this.driver = driver;
    }

    public Driver getDriver() {
        return driver;
    }
}

/*
 * =========================================================
 * FARE STRATEGY
 * =========================================================
 */

interface FareStrategy {

    double calculateFare(
            Location pickup,
            Location drop);
}

/*
 * =========================================================
 * NORMAL FARE STRATEGY
 * =========================================================
 */

class NormalFareStrategy
        implements FareStrategy {

    @Override
    public double calculateFare(
            Location pickup,
            Location drop) {

        double distance =
                pickup.distance(drop);

        return 50 + (distance * 10);
    }
}

/*
 * =========================================================
 * DRIVER MATCHING STRATEGY
 * =========================================================
 */

interface DriverMatchingStrategy {

    Driver matchDriver(
            Ride ride,
            List<Driver> drivers);
}

/*
 * =========================================================
 * NEAREST DRIVER STRATEGY
 * =========================================================
 */

class NearestDriverStrategy
        implements DriverMatchingStrategy {

    @Override
    public Driver matchDriver(
            Ride ride,
            List<Driver> drivers) {

        Driver bestDriver = null;

        double minDistance =
                Double.MAX_VALUE;

        for (Driver driver : drivers) {

            if (driver.getStatus()
                    != DriverStatus.ONLINE) {

                continue;
            }

            double distance =
                    driver.getLocation()
                            .distance(
                                    ride.getPickup()
                            );

            if (distance < minDistance) {

                minDistance = distance;
                bestDriver = driver;
            }
        }

        return bestDriver;
    }
}

/*
 * =========================================================
 * DRIVER MANAGER
 * =========================================================
 */

class DriverManager {

    private final List<Driver> drivers =
            new CopyOnWriteArrayList<>();

    public void addDriver(
            Driver driver) {

        drivers.add(driver);
    }

    public List<Driver> getDrivers() {
        return drivers;
    }
}

/*
 * =========================================================
 * RIDE MANAGER
 * =========================================================
 */


//RideManager is Uber/Ola and now visualize
class RideManager {

    private final DriverManager driverManager;

    private final FareStrategy fareStrategy;

    private final DriverMatchingStrategy
            matchingStrategy;

    private final AtomicInteger rideCounter =
            new AtomicInteger(1);

    public RideManager(
            DriverManager driverManager,
            FareStrategy fareStrategy,
            DriverMatchingStrategy matchingStrategy) {

        this.driverManager = driverManager;
        this.fareStrategy = fareStrategy;
        this.matchingStrategy =
                matchingStrategy;
    }

    public Ride requestRide(
            Rider rider,
            Location pickup,
            Location drop) {

        double fare =
                fareStrategy.calculateFare(
                        pickup,
                        drop
                );

        Ride ride =
                new Ride(
                        "RIDE-" +
                                rideCounter.getAndIncrement(),
                        rider,
                        pickup,
                        drop,
                        fare
                );

        System.out.println(
                "Estimated Fare : ₹" + fare
        );

        Driver driver =
                matchingStrategy.matchDriver(
                        ride,
                        driverManager.getDrivers()
                );

        if (driver == null) {

            System.out.println(
                    "No Driver Available"
            );

            ride.updateStatus(
                    RideStatus.CANCELLED
            );

            return ride;
        }

        driver.assignRide();

        ride.assignDriver(driver);

        System.out.println(
                "Assigned Driver : "
                        + driver.getName()
        );

        ride.updateStatus(
                RideStatus.DRIVER_ASSIGNED
        );

        return ride;
    }

    public void acceptRide(
            Ride ride) {

        ride.updateStatus(
                RideStatus.ACCEPTED
        );
    }

    public void driverArriving(
            Ride ride) {

        ride.updateStatus(
                RideStatus.DRIVER_ARRIVING
        );
    }

    public void startTrip(
            Ride ride) {

        ride.updateStatus(
                RideStatus.IN_PROGRESS
        );
    }

    public void endTrip(
            Ride ride) {

        ride.updateStatus(
                RideStatus.COMPLETED
        );

        Payment payment =
                new Payment(
                        UUID.randomUUID().toString(),
                        ride.getFare()
                );

        payment.process();

        ride.getDriver()
                .completeRide();
    }
}

/*
 * =========================================================
 * MAIN
 * =========================================================
 */

public class UberRideBookingSystem {

    public static void main(String[] args) {

        DriverManager driverManager =
                new DriverManager();

        driverManager.addDriver(
                new Driver(
                        "D1",
                        "Rahul",
                        new Location(1, 1)
                )
        );

        driverManager.addDriver(
                new Driver(
                        "D2",
                        "Amit",
                        new Location(10, 10)
                )
        );

        Rider rider =
                new Rider(
                        "R1",
                        "Pradeep"
                );

        RideManager rideManager =
                new RideManager(
                        driverManager,
                        new NormalFareStrategy(),
                        new NearestDriverStrategy()
                );

        Ride ride =
                rideManager.requestRide(
                        rider,
                        new Location(2, 2),
                        new Location(12, 12)
                );

        rideManager.acceptRide(ride);

        rideManager.driverArriving(ride);

        rideManager.startTrip(ride);

        rideManager.endTrip(ride);
    }
}
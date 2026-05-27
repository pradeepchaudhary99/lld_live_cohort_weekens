package week_4_5_6_7;

/*
Problem:
    ---> Parking Lot


    /*
    THINKING
        ENTRY EXIT GATES
        VECHICLE types
        MULTIPLE FLOORS WILL BE MANAGED BY THIS PL 
        ENTRY 
        GENERATE TICKET 
        ASSIGN SLOT WHEN VEHICLE ENTERS
        DIFFERENT TYPES OF PARKING SLOTS 
        PAYMENT STRATEGY
        SLOTSELECTION STRATEGY 
    */

/*
Step-0: Think what you are trying build and understand the system 
Step-1: F & N-F requirements 
Step-2: Core Entities 
Step-3: Understand the relationship and Design Patterns / 
        relationship
        and what all patterns u want to multiple
Step-4:



Step-1
Functional Requirement:
    1. should support multiple entry exit gates
    2. should allocate parking slot 
    3. should support multiple vehicle types 
    4. should generate ticket at the entry 
    5. should take the payment at the exit 
    6. should multiple floors with varible number of slots 
    7. multple slot type 
    8. should support multiple payment method 


Non Functional Requirement: 
    Extensible 
    Thread safety
        --> no double selection
    Follow Standard SOLID and Design Pattern


Step-2
Core Entities:
    ParkingLotManager
    ParkingLot 
    ParkingFloor
    ParkingSlot
    Ticket 
    Vehicle
    VehicleType{CAR, BIKE, TRUCK} 
    Payment 
    FeeStrategy
    SlotAllocationStrategy
    EntryGate
    ExitGate

    
Step-3:
    ParkingLot has-a EntryGate, ExitGate 
    ParkingLot has-a ParkingFloor, 
                            ---> Lis<ParkingSlot>




                        
*/

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

enum VehicleType { BIKE, CAR, TRUCK }

enum SlotType { 
    COMPACT,
    MEDIUM,
    LARGE
}

enum TicketStatus { ACTIVE, PAID, CLOSED }

class Vehicle {
    final String licensePlate;
    final VehicleType type;

    Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }
}

class ParkingSlot {
    final String slotId;
    final SlotType type;
    final int floorNumber;
    volatile boolean occupied = false;
    volatile Vehicle parkedVehicle = null;

    ParkingSlot(String slotId, SlotType type, int floorNumber) {
        this.slotId = slotId;
        this.type = type;
        this.floorNumber = floorNumber;
    }

    synchronized boolean parkVehicle(Vehicle v) {
        if (occupied) return false;
        this.parkedVehicle = v;
        this.occupied = true;
        return true;
    }

    synchronized boolean removeVehicle() {
        if (!occupied) return false;
        this.parkedVehicle = null;
        this.occupied = false;
        return true;
    }
}

class Ticket {
    final String ticketId;
    final Vehicle vehicle;
    final ParkingSlot slot;
    final LocalDateTime entryTime;
    LocalDateTime exitTime;
    double amount;
    TicketStatus status;

    Ticket(Vehicle vehicle, ParkingSlot slot) {
        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
    }

    long getDurationHours() {
        LocalDateTime exit = (exitTime != null) ? exitTime : LocalDateTime.now();
        long minutes = Duration.between(entryTime, exit).toMinutes();
        return (minutes / 60) + ((minutes % 60 > 0) ? 1 : 0);
    }
}

class Payment {
    final String paymentId;
    final Ticket ticket;
    final double amount;
    final LocalDateTime timestamp;

    Payment(Ticket ticket, double amount) {
        this.paymentId = UUID.randomUUID().toString();
        this.ticket = ticket;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
}

interface PricingStrategy {
    double calculate(Ticket ticket);
}

class HourlyPricing implements PricingStrategy {
    private final Map<VehicleType, Double> rates = Map.of(
        VehicleType.BIKE, 10.0,
        VehicleType.CAR, 20.0,
        VehicleType.TRUCK, 50.0
    );

    public double calculate(Ticket ticket) {
        long hours = ticket.getDurationHours();
        return hours * rates.get(ticket.vehicle.type);
    }
}

interface ParkingObserver {
    void update(int floor, Map<SlotType, Integer> availability);
}

class DisplayBoard implements ParkingObserver {
    private final String location;

    DisplayBoard(String location) {
        this.location = location;
    }

    public void update(int floor, Map<SlotType, Integer> availability) {
        System.out.println("\n[DISPLAY @ " + location + "] Floor " + floor + " Status:");
        availability.forEach((type, count) -> 
            System.out.println("  " + type + ": " + count + " available")
        );
    }
}

class ParkingFloor {
    final int floorNumber;
    private final Map<SlotType, List<ParkingSlot>> slotsByType = new ConcurrentHashMap<>();
    private final List<ParkingObserver> observers = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    ParkingFloor(int floorNumber, int compactSlots, int mediumSlots, int largeSlots) {
        this.floorNumber = floorNumber;
        
        slotsByType.put(SlotType.COMPACT, new CopyOnWriteArrayList<>());
        slotsByType.put(SlotType.MEDIUM, new CopyOnWriteArrayList<>());
        slotsByType.put(SlotType.LARGE, new CopyOnWriteArrayList<>());

        for (int i = 0; i < compactSlots; i++) {
            slotsByType.get(SlotType.COMPACT).add(
                new ParkingSlot("F" + floorNumber + "-C" + i, SlotType.COMPACT, floorNumber));
        }
        for (int i = 0; i < mediumSlots; i++) {
            slotsByType.get(SlotType.MEDIUM).add(
                new ParkingSlot("F" + floorNumber + "-M" + i, SlotType.MEDIUM, floorNumber));
        }
        for (int i = 0; i < largeSlots; i++) {
            slotsByType.get(SlotType.LARGE).add(
                new ParkingSlot("F" + floorNumber + "-L" + i, SlotType.LARGE, floorNumber));
        }
    }

    void addObserver(ParkingObserver observer) {
        observers.add(observer);
    }

    void removeObserver(ParkingObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        Map<SlotType, Integer> availability = new HashMap<>();
        slotsByType.forEach((type, slots) -> {
            long available = slots.stream().filter(s -> !s.occupied).count();
            availability.put(type, (int) available);
        });
        observers.forEach(observer -> observer.update(floorNumber, availability));
    }

    ParkingSlot findAvailableSlot(SlotType type) {
        lock.lock();
        try {
            List<ParkingSlot> slots = slotsByType.get(type);
            for (ParkingSlot slot : slots) {
                if (!slot.occupied) {
                    return slot;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    boolean parkVehicle(Vehicle v, ParkingSlot slot) {
        lock.lock();
        try {
            boolean success = slot.parkVehicle(v);
            if (success) notifyObservers();
            return success;
        } finally {
            lock.unlock();
        }
    }

    boolean removeVehicle(ParkingSlot slot) {
        lock.lock();
        try {
            boolean success = slot.removeVehicle();
            if (success) notifyObservers();
            return success;
        } finally {
            lock.unlock();
        }
    }

    Map<SlotType, Integer> getAvailability() {
        Map<SlotType, Integer> availability = new HashMap<>();
        slotsByType.forEach((type, slots) -> {
            long available = slots.stream().filter(s -> !s.occupied).count();
            availability.put(type, (int) available);
        });
        return availability;
    }
}

class EntryGate {
    final String gateId;
    private final ParkingLot parkingLot;

    EntryGate(String gateId, ParkingLot parkingLot) {
        this.gateId = gateId;
        this.parkingLot = parkingLot;
    }

    Ticket issueTicket(Vehicle vehicle) {
        System.out.println("\n[ENTRY GATE " + gateId + "] Vehicle " + vehicle.licensePlate 
            + " (" + vehicle.type + ") entering...");
        
        Ticket ticket = parkingLot.parkVehicle(vehicle);
        
        if (ticket != null) {
            System.out.println("✓ Ticket issued: " + ticket.ticketId);
            System.out.println("  Slot: " + ticket.slot.slotId 
                + " (Floor " + ticket.slot.floorNumber + ")");
        } else {
            System.out.println("✗ No available slot for " + vehicle.type);
        }
        
        return ticket;
    }
}

class ExitGate {
    final String gateId;
    private final ParkingLot parkingLot;

    ExitGate(String gateId, ParkingLot parkingLot) {
        this.gateId = gateId;
        this.parkingLot = parkingLot;
    }

    Payment processExit(Ticket ticket) {
        System.out.println("\n[EXIT GATE " + gateId + "] Processing ticket " + ticket.ticketId);
        
        ticket.exitTime = LocalDateTime.now();
        ticket.amount = parkingLot.calculateCharges(ticket);
        
        System.out.println("  Duration: " + ticket.getDurationHours() + " hours");
        System.out.println("  Amount: $" + ticket.amount);
        
        Payment payment = parkingLot.processPayment(ticket);
        
        if (payment != null) {
            parkingLot.releaseSlot(ticket);
            System.out.println("✓ Payment successful. Gate opening...");
        }
        
        return payment;
    }
}

class ParkingLot {
    private final List<ParkingFloor> floors = new ArrayList<>();
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();
    private final PricingStrategy pricingStrategy;
    
    private final Map<VehicleType, SlotType> vehicleToSlotMapping = Map.of(
        VehicleType.BIKE, SlotType.COMPACT,
        VehicleType.CAR, SlotType.MEDIUM,
        VehicleType.TRUCK, SlotType.LARGE
    );

    ParkingLot(int numFloors, int compactPerFloor, int mediumPerFloor, int largePerFloor) {
        this.pricingStrategy = new HourlyPricing();
        
        for (int i = 0; i < numFloors; i++) {
            floors.add(new ParkingFloor(i, compactPerFloor, mediumPerFloor, largePerFloor));
        }
    }

    void addDisplayBoard(int floorNumber, DisplayBoard board) {
        if (floorNumber < floors.size()) {
            floors.get(floorNumber).addObserver(board);
        }
    }

    Ticket parkVehicle(Vehicle vehicle) {
        SlotType requiredType = vehicleToSlotMapping.get(vehicle.type);
        
        for (ParkingFloor floor : floors) {
            ParkingSlot slot = floor.findAvailableSlot(requiredType);
            if (slot != null) {
                if (floor.parkVehicle(vehicle, slot)) {
                    Ticket ticket = new Ticket(vehicle, slot);
                    activeTickets.put(ticket.ticketId, ticket);
                    return ticket;
                }
            }
        }
        return null;
    }

    double calculateCharges(Ticket ticket) {
        return pricingStrategy.calculate(ticket);
    }

    Payment processPayment(Ticket ticket) {
        ticket.status = TicketStatus.PAID;
        return new Payment(ticket, ticket.amount);
    }

    void releaseSlot(Ticket ticket) {
        ParkingFloor floor = floors.get(ticket.slot.floorNumber);
        floor.removeVehicle(ticket.slot);
        ticket.status = TicketStatus.CLOSED;
        activeTickets.remove(ticket.ticketId);
    }

    void displayStatus() {
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("PARKING LOT STATUS");
        System.out.println("═══════════════════════════════════════════");
        for (ParkingFloor floor : floors) {
            System.out.println("\nFloor " + floor.floorNumber + ":");
            floor.getAvailability().forEach((type, count) -> 
                System.out.println("  " + type + ": " + count + " available")
            );
        }
        System.out.println("\nActive Tickets: " + activeTickets.size());
    }
}

public class ParkingLotLLD {
    public static void main(String[] args) throws InterruptedException {
        ParkingLot parkingLot = new ParkingLot(3, 5, 10, 3);

        parkingLot.addDisplayBoard(0, new DisplayBoard("Entry Gate 1"));
        parkingLot.addDisplayBoard(1, new DisplayBoard("Entry Gate 2"));
        parkingLot.addDisplayBoard(2, new DisplayBoard("Exit Gate 1"));

        EntryGate entryGate1 = new EntryGate("E1", parkingLot);
        EntryGate entryGate2 = new EntryGate("E2", parkingLot);
        ExitGate exitGate1 = new ExitGate("X1", parkingLot);

        parkingLot.displayStatus();

        System.out.println("\n\n═══ SCENARIO 1: Vehicle Entry ═══");
        
        Vehicle car1 = new Vehicle("ABC-123", VehicleType.CAR);
        Ticket ticket1 = entryGate1.issueTicket(car1);
        
        Vehicle bike1 = new Vehicle("XYZ-789", VehicleType.BIKE);
        Ticket ticket2 = entryGate2.issueTicket(bike1);
        
        Vehicle truck1 = new Vehicle("TRK-456", VehicleType.TRUCK);
        Ticket ticket3 = entryGate1.issueTicket(truck1);

        Thread.sleep(500);

        System.out.println("\n\n═══ SCENARIO 2: More Vehicles ═══");
        
        for (int i = 0; i < 3; i++) {
            Vehicle car = new Vehicle("CAR-" + i, VehicleType.CAR);
            entryGate1.issueTicket(car);
        }

        parkingLot.displayStatus();

        System.out.println("\n\n═══ SCENARIO 3: Vehicle Exit ═══");
        
        Thread.sleep(1000);
        
        if (ticket1 != null) {
            Payment payment1 = exitGate1.processExit(ticket1);
        }

        Thread.sleep(500);

        if (ticket2 != null) {
            Payment payment2 = exitGate1.processExit(ticket2);
        }

        parkingLot.displayStatus();

        System.out.println("\n\n═══ SCENARIO 4: Fill Up Parking Lot ═══");
        
        for (int i = 0; i < 25; i++) {
            Vehicle car = new Vehicle("FILL-" + i, VehicleType.CAR);
            entryGate1.issueTicket(car);
        }

        Vehicle extraCar = new Vehicle("EXTRA-999", VehicleType.CAR);
        entryGate1.issueTicket(extraCar);

        parkingLot.displayStatus();
    }
}

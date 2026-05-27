package week_4_5_6_7;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.transform.SourceLocator;

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

enum SlotType{
    COMPACT,
    MEDIUM,
    LARGE
}

enum TicketStatus{
    ACTIVE, PAID, CLOSED
}

enum VehicleType{
    CAR,
    BIKE,
    TRUCK
}

//Entities 
class Vehicle{
    String licensePlate;
    VehicleType vehicleType;
}

class ParkingSlot{
    String slotId;
    SlotType type;
    boolean occupied = false;
    Vehicle parkedVehicle = null;

    boolean parkVehicle(Vehicle v){
        lock.lock();
        try{
            boolean success = slot.parkVehicle(v);
            if(success) notifyObserver();
            return success;
        }finally{
            lock.unlock();
        }
    }

    boolean unparkVehicle(){
        lock.lock();
        try{
            boolean success = slot.unparkVehicle();
            if(success) notifyObserver();
            return success;
        }finally{
            lock.unlock();
        }
    }
}

class Ticket{
    String ticketId;
    Vehicle vechile;
    ParkingSlot slot;
    LocalDateTime entryTime;
    LocalDateTime exitTime;
    double amount;
    TicketStatus status;

    long getDurationHours() {
        //
    }
}


interface IPricingStrategy{
    double calculatePrice(Ticket ticket){

    }
}

class HourlyPricing implements IPricingStrategy{

    @Override
    public double calculatePrice(Ticket ticket) {

    }
}

interface ParkingObserver{
    void update(int floor, Map<SlotType, Integer> availability);
}

class DisplayBoard implements ParkingObserver{
    @Override
    public void update(int floor, Map<SlotType, Integer> availability) {

    }
}
//interface Observable

class ParkingFloor{
    int floorId;
    Map<SlotType, List<ParkingSlot>> slotByType = new ConcurrentHashMap<>();
    List<ParkingObserver> observers = new CopyOnWriteArrayList<>();
    void addObserver(ParkingObserver o){
        observers.add(o);
    }
    void deleteObserver(ParkingObserver o){

    }

    void notifyObserver(){

    }

    ParkingSlot getFreeSlot(VechicleType type{
        
    }

}

class EntryGate{

    String gateId;
    ParkingLot parkinglot;

    Ticket generateTicket(Vehicle vehicle){
        Ticket ticket = parkingLot.parkVechile(vehicle);
        // validation logic 
        return ticket;
    }
}

interface IPayment{

}
class EntryGate{

    String gateId;
    ParkingLot parkinglot;

    Ticket processExit(Ticket ticket){
        // validation logic 

        // payment logic process the payment..

        return ticket;
    }
}


interface slotSelectionStrategy{
    ParkingSlot getSlot(List<ParkingFloor> floors);
}

class FirstFit implements slotSelectionStrategy{

    @Override
    public ParkingSlot getSlot(List<ParkingFloor> floors) {
       //
    }
    
}
class ParkingLot{
    List<ParkingFloors> floors;
    /*

    */
   Ticket parkVehicle(Vehicle v){

    ParkingSlot slot = slotSelectionStrategy.getSlot(floors);
    slot.parkedVehicle(v);
    
   }
}





public class ParkingLotClass_Code {
    
}

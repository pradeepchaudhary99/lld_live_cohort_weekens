package week_4_5_6_7;

/*

Step-0:
Elevator System
    THINKING:
        --> ElevatorSystem  
                --> Elevator 
        --> press button 
            --> System will assign u the elevator 
            --> entered the elevator 
                        --> press the button 
        
        --> 
---> 
Functional & Non functional requirements;

1. Functional Requirement:
    -- User should be request elevator from a floor 
    -- Elevator can move up/down 
    -- Open/Close doors 
    -- handle multiple Elevator
    -- Assign Best Elevator 
    -- Handle multiple request
    -- Enter destination from elevator System 
    -- Handle Capacity Limits 
    



*/

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

enum Direction { UP, DOWN, IDLE }

class Request {
    final int floor;
    final Direction direction;

    Request(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    Request(int floor) {
        this.floor = floor;
        this.direction = null;
    }
}

interface ElevatorObserver {
    void update(int elevatorId, int floor, Direction direction);
}

class FloorDisplay implements ElevatorObserver {
    private final int floor;

    FloorDisplay(int floor) {
        this.floor = floor;
    }

    public void update(int elevatorId, int currentFloor, Direction direction) {
        if (currentFloor == this.floor) {
            System.out.println("[DISPLAY Floor " + floor + "] Elevator-" + elevatorId 
                + " | " + direction);
        }
    }
}

class Elevator implements Runnable {
    final int id;
    private int currentFloor = 0;
    private Direction direction = Direction.IDLE;
    
    private final PriorityQueue<Integer> upStops = new PriorityQueue<>();
    private final PriorityQueue<Integer> downStops = new PriorityQueue<>(Collections.reverseOrder());
    
    private final ReentrantLock lock = new ReentrantLock();
    private final List<ElevatorObserver> observers = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;

    Elevator(int id) {
        this.id = id;
    }

    void addObserver(ElevatorObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        observers.forEach(obs -> obs.update(id, currentFloor, direction));
    }

    void addRequest(int floor, Direction dir) {
        lock.lock();
        try {
            if (dir == Direction.UP || (dir == null && floor > currentFloor)) {
                upStops.offer(floor);
            } else if (dir == Direction.DOWN || (dir == null && floor < currentFloor)) {
                downStops.offer(floor);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        System.out.println("Elevator-" + id + " started");
        
        while (running) {
            lock.lock();
            try {
                if (direction == Direction.UP || direction == Direction.IDLE) {
                    if (!upStops.isEmpty()) {
                        direction = Direction.UP;
                        int target = upStops.peek();
                        
                        if (currentFloor < target) {
                            currentFloor++;
                            notifyObservers();
                        } else if (currentFloor == target) {
                            upStops.poll();
                            System.out.println("Elevator-" + id + " STOPPED at floor " + currentFloor);
                            notifyObservers();
                            Thread.sleep(1000);
                        }
                    } else if (!downStops.isEmpty()) {
                        direction = Direction.DOWN;
                    } else {
                        direction = Direction.IDLE;
                    }
                }

                if (direction == Direction.DOWN) {
                    if (!downStops.isEmpty()) {
                        int target = downStops.peek();
                        
                        if (currentFloor > target) {
                            currentFloor--;
                            notifyObservers();
                        } else if (currentFloor == target) {
                            downStops.poll();
                            System.out.println("Elevator-" + id + " STOPPED at floor " + currentFloor);
                            notifyObservers();
                            Thread.sleep(1000);
                        }
                    } else if (!upStops.isEmpty()) {
                        direction = Direction.UP;
                    } else {
                        direction = Direction.IDLE;
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void stop() {
        running = false;
    }

    int getCurrentFloor() {
        lock.lock();
        try { return currentFloor; }
        finally { lock.unlock(); }
    }

    Direction getDirection() {
        lock.lock();
        try { return direction; }
        finally { lock.unlock(); }
    }

    boolean isIdle() {
        lock.lock();
        try { return direction == Direction.IDLE && upStops.isEmpty() && downStops.isEmpty(); }
        finally { lock.unlock(); }
    }

    void displayState() {
        lock.lock();
        try {
            System.out.println("  Elevator-" + id + " | Floor: " + currentFloor 
                + " | Dir: " + direction 
                + " | UpStops: " + upStops + " | DownStops: " + downStops);
        } finally {
            lock.unlock();
        }
    }
}

interface SelectionStrategy {
    Elevator select(List<Elevator> elevators, int floor);
}

class NearestIdleStrategy implements SelectionStrategy {
    public Elevator select(List<Elevator> elevators, int floor) {
        return elevators.stream()
            .filter(Elevator::isIdle)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)))
            .orElse(elevators.get(0));
    }
}

class SameDirectionStrategy implements SelectionStrategy {
    public Elevator select(List<Elevator> elevators, int floor) {
        for (Elevator e : elevators) {
            if (e.getDirection() == Direction.UP && e.getCurrentFloor() < floor) return e;
            if (e.getDirection() == Direction.DOWN && e.getCurrentFloor() > floor) return e;
        }
        return new NearestIdleStrategy().select(elevators, floor);
    }
}

class ElevatorController {
    private final List<Elevator> elevators = new ArrayList<>();
    private final ExecutorService executor;
    private SelectionStrategy strategy;

    ElevatorController(int numElevators, SelectionStrategy strategy) {
        this.strategy = strategy;
        this.executor = Executors.newFixedThreadPool(numElevators);

        for (int i = 0; i < numElevators; i++) {
            Elevator elevator = new Elevator(i);
            elevators.add(elevator);
            executor.submit(elevator);
        }
    }

    void addDisplayToFloor(int floor) {
        FloorDisplay display = new FloorDisplay(floor);
        elevators.forEach(e -> e.addObserver(display));
    }

    void requestPickup(int floor, Direction direction) {
        System.out.println("\n[PICKUP] Floor " + floor + " | Direction: " + direction);
        Elevator selected = strategy.select(elevators, floor);
        System.out.println("  → Assigned to Elevator-" + selected.id);
        selected.addRequest(floor, direction);
    }

    void requestDestination(int elevatorId, int floor) {
        System.out.println("\n[INSIDE Elevator-" + elevatorId + "] Destination: Floor " + floor);
        elevators.get(elevatorId).addRequest(floor, null);
    }

    void displayStatus() {
        System.out.println("\n═══ ELEVATOR STATUS ═══");
        elevators.forEach(Elevator::displayState);
    }

    void shutdown() {
        elevators.forEach(Elevator::stop);
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}

public class ElevatorSystemLLD {
    public static void main(String[] args) throws InterruptedException {
        ElevatorController controller = new ElevatorController(3, new SameDirectionStrategy());

        controller.addDisplayToFloor(0);
        controller.addDisplayToFloor(5);
        controller.addDisplayToFloor(10);

        controller.displayStatus();

        System.out.println("\n\n═══ SCENARIO 1: Pickup Requests ═══");
        
        controller.requestPickup(5, Direction.UP);
        Thread.sleep(500);
        
        controller.requestPickup(3, Direction.DOWN);
        Thread.sleep(500);
        
        controller.requestPickup(8, Direction.UP);
        
        Thread.sleep(5000);
        controller.displayStatus();

        System.out.println("\n\n═══ SCENARIO 2: Inside Destinations ═══");
        
        controller.requestDestination(0, 7);
        controller.requestDestination(0, 9);
        controller.requestDestination(1, 2);
        
        Thread.sleep(6000);
        controller.displayStatus();

        System.out.println("\n\n═══ SCENARIO 3: SCAN (UP then DOWN) ═══");
        
        controller.requestDestination(0, 3);
        controller.requestDestination(0, 5);
        controller.requestDestination(0, 7);
        Thread.sleep(500);
        controller.requestDestination(0, 4);
        controller.requestDestination(0, 2);
        
        System.out.println("\nWatch Elevator-0 go UP [3,5,7], then DOWN [4,2]");
        
        Thread.sleep(10000);
        controller.displayStatus();

        controller.shutdown();
    }
}
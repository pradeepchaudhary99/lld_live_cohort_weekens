package week_4_5_6_7;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/*
Functional Requirement:
Request
    --> Direction
    --> FloorID
Direction
    UP, DOWN, IDLE 
SHOULD be Able to assign the best elevator
should use different elevator selection strategy 
Should have multiple elevators to manage

it should follow SCAN algorithm 

Non-Functional Requirement:
    // Thread Safety
    // No duplicate request
    //Extensibility
    // Follow SOLID Principles
*/

/*
Entities and Relationship:

Request 
Elevator 
ElevatorSystem 
ElevatorSelectionStrategy
DisplayBoard
Direction Enum


*/

class Elevator implements Runnable{
    int id;
    int currentFloor = 0;

    PriorityQueue<Integer> upStops = new PriorityQueue<>(); // minHeap...
    PriorityQueue<Integer> downStops = new PriorityQueue<>(Collections.ReverseOrder());

    ReentrantLock lock = new ReentrantLock();
    List<ObserverDisplay> observers = new ArrayList<>();

    boolean running = true;
    
    // 

    void addRequest(int floor, Direction dir){
        lock.lock();
        try{
            if(floor >= currentFloor){
                upStops.offer(floor);
            }
            else{
                downStops.offer(floor);
            }

        }
        finally{
            lock.unlock();
        }
    }





    @Override
    public void run() {
        System.out.println("Elevator" + id + " Started");
    
        while(running){
            lock.lock();
                try{
                    while(!upStops.isEmpty()){
                        int target = upStops.peek();
                        if(currentFloor < target){
                            currentFloor++;
                        }

                        if(currentFloor == target){
                            // 
                        }
                    }
                }
        }

    }
    
}

class ElevatorSystem{
    List<Elevator> elevators;
    List<Thread> threads;

    public ElevatorSytstem(int numberOfElevators){
        for(int i = 0; i < numberOfElevators; i++){
            Elevator e = new Elevator();
            Thread t = new Thread(e);
            t.start();
            threads.add(t);
            elevators.add(e);
        }
    }
}




public class ElevatorSystemClassDemo {
    
}

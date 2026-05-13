package week_1;


class ElevatorSystem{
    ElevatorSchedular schedular;
    public ElevatorSystem(ElevatorSchedular schedular){
        this.schedular = schedular;
    }
    void findElevator(){
        schedular.getElevator(null);
    }
}

interface ElevatorSchedular{
    void getElevator(int[] lists);
}

class Approach1 implements ElevatorSchedular {

    @Override
    public void getElevator(int[] lists) {

    }
    public void Approach1Helper(){
        System.out.println("helper");
    }

}

class Approach2 implements ElevatorSchedular {

    @Override
    public void getElevator(int[] lists) {

    }
}

class Approach3 implements ElevatorSchedular {

    @Override
    public void getElevator(int[] lists) {

    }
}


public class Polymorphism {
    
}

package week_4;

/*
Request 
    Priority
        < 5 
        Level1
        < 10
        Level2
        < 20
        Level3

    Level1 ---> Level2 --> Level3 


*/

class Request{
    int priority;
    String work;
    public Request(int priority, String work){
        this.priority = priority;
        this.work = work;
    }
}
abstract class LevelHandler{
    LevelHandler nextHandler;
    public LevelHandler(LevelHandler nextHandler){
        this.nextHandler = nextHandler;
    }
    abstract boolean canHandler(Request request);
    abstract void processRequest(Request request);
}

class Level1Handler extends LevelHandler{

    public Level1Handler(LevelHandler nextHandler){
        super(nextHandler);
    }
    @Override
    boolean canHandler(Request request) {
        if(request.priority < 5){
            return true;
        }else{
            return false;
        }
    }

    @Override
    void processRequest(Request request) {
        if(canHandler(request)){
            System.out.println("Level1 Processing the requests " + request.work);
        }else{
            nextHandler.processRequest(request);
        }
    }
}

class Level2Handler extends LevelHandler{

    public Level2Handler(LevelHandler nextHandler){
        super(nextHandler);
    }
    @Override
    boolean canHandler(Request request) {
        if(request.priority <= 10){
            return true;
        }else{
            return false;
        }
    }

    @Override
    void processRequest(Request request) {
        if(canHandler(request)){
            System.out.println("Level2 Processing the requests " + request.work);
        }else{
            nextHandler.processRequest(request);
        }
    }
}

class Level3Handler extends LevelHandler{

    public Level3Handler(LevelHandler nextHandler){
        super(nextHandler);
    }
    @Override
    boolean canHandler(Request request) {
        if(request.priority < 15){
            return true;
        }else{
            return false;
        }
    }

    @Override
    void processRequest(Request request) {
        if(canHandler(request)){
            System.out.println("Level3 Processing the requests " + request.work);
        }else{
            if(nextHandler != null){
                nextHandler.processRequest(request);
            }
            System.out.println("Last Handler so no futher handlers");
        }
    }
}

public class  SupportTicketSystem {
    public static void main(String[] args) {
        //Build the chain

        LevelHandler level3 = new Level3Handler(null);
        LevelHandler level2 = new Level2Handler(level3);
        LevelHandler level1 = new Level1Handler(level2);

        Request request = new Request(12,"work1");
        level1.processRequest(request);

    }
}

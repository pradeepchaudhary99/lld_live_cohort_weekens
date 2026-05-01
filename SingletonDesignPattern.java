
class Logger{
    // only 1 object is needed
    private static Logger instance;
    private Logger(){

    }
    public static Logger getInstance(){
        if(instance == null){
            instance = new Logger();
        }
        return instance;
    }
}

public class SingletonDesignPattern {
    public static void main(String[] args) {
        Logger logger = Logger.getInstance();
    }
}

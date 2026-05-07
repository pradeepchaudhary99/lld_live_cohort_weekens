package week_2;

interface ILogger{
    void log(String message);
}

class ConsoleLogger implements ILogger{
    @Override
    public void log(String message) {
        System.out.println("Console Logger "+message);
    }
}

abstract class LoggerDecorator implements ILogger{
    protected ILogger logger;
    public LoggerDecorator(ILogger logger){
        this.logger = logger;
    }
}

class TimeStampDecorator extends LoggerDecorator{
    public TimeStampDecorator(ILogger logger){
        super(logger);
    }
    @Override
    public void log(String message) {
        // what you want to add extra, this is the code for decoration
        System.out.println("TimeStampAdded  "+System.currentTimeMillis());
        logger.log(message);
    }
}

class JsonDecorator extends LoggerDecorator{
    public JsonDecorator(ILogger logger){
        super(logger);
    }
    @Override
    public void log(String message) {
        System.out.println("Json Parsed log: ");
        logger.log(message);
    }
}

public class Decorator_Design_Pattern {
    public static void main(String[] args) {
        ILogger logger = new ConsoleLogger();   //base is ready
        String message = "pradeep is teaching lld";
        // logger.log("pradeep is teaching lld");
        logger = new TimeStampDecorator(new JsonDecorator(logger));
        logger.log(message);
    }
}

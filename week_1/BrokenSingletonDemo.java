package week_1;



//Lazy initialization
class BrokenSingleton {
    private static BrokenSingleton instance;

    private BrokenSingleton() {
        System.out.println("Instance created by: " + Thread.currentThread().getName());
    }

    public static BrokenSingleton getInstance() {
        // t1,t2,t3,t4,t5,t
        if (instance == null) {
            synchronized(BrokenSingleton.class){ 
                // NOT thread-safe
                if(instance == null)
                    instance = new BrokenSingleton(); // multiple threads enter here
            } 
        }
        return instance;
    }
}

public class BrokenSingletonDemo {

    public static void main(String[] args) throws InterruptedException {
        
        Runnable task = () -> {
          BrokenSingleton.getInstance();
        };

        // Thread t1 = new Thread(task, "thread-1");
        // Thread t2 = new Thread(task, "thread-2");
        // Thread t3 = new Thread(task, "thread-3");

        int THREAD_COUNT = 20;
        Thread[] threads = new Thread[THREAD_COUNT];

 
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(task, "Thread-" + (i + 1));
            threads[i].start();
        }

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].join();
        }
        
        System.out.println("Main thread finished after all threads completed");

       
    }
}

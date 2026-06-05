

/*


ThreadPool
FR:
    1. User should be able to submit the Task
    2. Workers should be able to process those task
*/

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

interface Task{
    void execute();
}

class Worker implements Runnable{
    BlockingQueue<Task> queue;
    boolean running = true;

    public Worker(BlockingQueue<Task> queue){
        this.queue = queue;
    }
    @Override
    public void run() {

        while(running || !Thread.currentThread().interrupted()){
            try{
                Task task = queue.take();
                System.out.println("Executing the task");
                task.execute();
            }catch(InterruptedException e){
                System.out.println("handling the exception grafeful");
            }
        }
    }

    public void shutdown(){
        running = false;
    }
}

class  ThreadPool{
    BlockingQueue<Task> queue;
    List<Worker> workers;
    List<Thread> threads;

    public ThreadPool(int size){
        queue = new LinkedBlockingDeque<>();
        workers = new ArrayList<>();
        
        for(int i = 1;  i <= size; i++){
            Worker worker = new Worker(queue);
            workers.add(worker);
            Thread thread = new Thread(worker);
            threads.add(thread);
            thread.start();
        }
    }

    public void submit(Task task){
        queue.offer(task);
    }

    public void shutdown(){
        for(Worker worker : workers){
            worker.shutdown();
        }
        for(Thread t : threads){
            t.interrupt();
        }
    }
}


class ThreadPool{



}
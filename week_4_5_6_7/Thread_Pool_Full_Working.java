package week_4_5_6_7;

import java.util.*;
import java.util.concurrent.*;

// ══════════════════════════════════════════════════════════════════════════════
// TASK — What workers execute
// ══════════════════════════════════════════════════════════════════════════════

interface Task extends Runnable {
    int getId();
}

class SimpleTask implements Task {
    final int id;
    final String name;

    SimpleTask(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            System.out.println("[TASK-" + id + "] " + name + " STARTED on " 
                + Thread.currentThread().getName());
            Thread.sleep(1000); // simulate work
            System.out.println("[TASK-" + id + "] " + name + " COMPLETED");
        } catch (InterruptedException e) {
            System.out.println("[TASK-" + id + "] INTERRUPTED");
        }
    }

    @Override
    public int getId() { return id; }
}

// ══════════════════════════════════════════════════════════════════════════════
// WORKER — Executes tasks from queue
// ══════════════════════════════════════════════════════════════════════════════

class Worker implements Runnable {
    final int id;
    final BlockingQueue<Task> taskQueue;
    volatile boolean running = true;
    int tasksCompleted = 0;

    Worker(int id, BlockingQueue<Task> taskQueue) {
        this.id = id;
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        System.out.println("Worker-" + id + " STARTED");

        while (running) {
            try {
                // ① WAIT for task (blocks if empty)
                Task task = taskQueue.take();

                // ② EXECUTE task
                task.run();

                // ③ TRACK stats
                tasksCompleted++;

            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("Worker-" + id + " FINISHED (completed: " + tasksCompleted + ")");
    }

    void stop() { running = false; }
}

// ══════════════════════════════════════════════════════════════════════════════
// THREADPOOL — Manages workers and queue
// ══════════════════════════════════════════════════════════════════════════════

class ThreadPool {
    final int poolSize;
    final BlockingQueue<Task> taskQueue;
    final List<Worker> workers;
    final List<Thread> threads;
    volatile boolean shutdown = false;
    int taskCounter = 0;

    ThreadPool(int poolSize, int queueCapacity) {
        this.poolSize = poolSize;
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.workers = new ArrayList<>();
        this.threads = new ArrayList<>();

        // Create workers and threads
        for (int i = 1; i <= poolSize; i++) {
            Worker worker = new Worker(i, taskQueue);
            workers.add(worker);

            Thread thread = new Thread(worker, "Worker-" + i);
            threads.add(thread);
            thread.start();
        }

        System.out.println("═══ ThreadPool created: " + poolSize + " workers, " 
            + queueCapacity + " queue capacity ═══\n");
    }

    // Submit task
    void submit(String taskName) {
        if (shutdown) {
            System.out.println("[ERROR] Pool is shutdown");
            return;
        }

        taskCounter++;
        Task task = new SimpleTask(taskCounter, taskName);

        try {
            taskQueue.put(task);
            System.out.println("[SUBMITTED] Task-" + taskCounter + " (" + taskName 
                + ") | Queue size: " + taskQueue.size());
        } catch (InterruptedException e) {
            System.out.println("[ERROR] Failed to submit task");
        }
    }

    // Shutdown pool
    void shutdown() {
        System.out.println("\n[SHUTDOWN] Stopping all workers...");
        shutdown = true;

        // Signal workers to stop
        workers.forEach(Worker::stop);

        // Interrupt all threads
        threads.forEach(Thread::interrupt);

        // Wait for all to finish
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[SHUTDOWN] All workers stopped\n");
    }

    // Display stats
    void displayStats() {
        System.out.println("\n═══ ThreadPool Stats ═══");
        System.out.println("Pool Size: " + poolSize);
        System.out.println("Queue Size: " + taskQueue.size());
        
        int totalCompleted = 0;
        for (int i = 0; i < workers.size(); i++) {
            int completed = workers.get(i).tasksCompleted;
            totalCompleted += completed;
            System.out.println("  Worker-" + (i + 1) + ": " + completed + " tasks");
        }
        
        System.out.println("Total Completed: " + totalCompleted + "\n");
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN — Demonstration
// ══════════════════════════════════════════════════════════════════════════════

public class Thread_Pool_Full_Working {
    public static void main(String[] args) throws InterruptedException {

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 1: Basic usage (2 workers, 5 tasks)
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 1: 2 Workers, 5 Tasks ═══\n");

        ThreadPool pool1 = new ThreadPool(2, 10);

        for (int i = 1; i <= 5; i++) {
            pool1.submit("Task-" + i);
            Thread.sleep(200);
        }

        Thread.sleep(6000); // wait for completion
        pool1.displayStats();
        pool1.shutdown();

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 2: More workers than tasks
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ SCENARIO 2: 5 Workers, 3 Tasks ═══\n");

        ThreadPool pool2 = new ThreadPool(5, 10);

        pool2.submit("QuickTask-1");
        pool2.submit("QuickTask-2");
        pool2.submit("QuickTask-3");

        Thread.sleep(3000);
        pool2.displayStats();
        pool2.shutdown();

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 3: Queue overload (bounded queue)
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ SCENARIO 3: Queue Overload (Small Queue) ═══\n");

        ThreadPool pool3 = new ThreadPool(2, 3); // small queue

        System.out.println("Submitting 8 tasks to small queue...\n");
        for (int i = 1; i <= 8; i++) {
            pool3.submit("BusyTask-" + i);
        }

        Thread.sleep(10000);
        pool3.displayStats();
        pool3.shutdown();

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 4: Heavy load test
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ SCENARIO 4: Heavy Load (10 Tasks) ═══\n");

        ThreadPool pool4 = new ThreadPool(3, 10);

        for (int i = 1; i <= 10; i++) {
            pool4.submit("HeavyTask-" + i);
        }

        Thread.sleep(12000);
        pool4.displayStats();
        pool4.shutdown();
    }
}

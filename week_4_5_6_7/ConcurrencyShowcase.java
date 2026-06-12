import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * ConcurrencyShowcase.java
 *
 * Single-file reference covering the core concurrency concepts used in LLD interviews:
 *
 *   1. Thread basics       - Runnable vs Callable
 *   2. synchronized        - protecting a shared counter
 *   3. Pessimistic locking - ReentrantLock + tryLock, seat booking
 *   4. Optimistic locking  - AtomicInteger CAS retry loop, wallet debit
 *   5. ReadWriteLock       - concurrent reads, exclusive writes
 *   6. ExecutorService     - thread pool, submit/Future, invokeAll, shutdown
 *
 * Theme: a tiny "movie ticket booking + wallet" system.
 *
 * Run with:
 *   javac ConcurrencyShowcase.java
 *   java ConcurrencyShowcase
 */
public class ConcurrencyShowcase {

    public static void main(String[] args) throws Exception {
        System.out.println("===== 1. THREADS: Runnable vs Callable =====");
        threadBasicsDemo();

        System.out.println("\n===== 2. SYNCHRONIZED: protecting a shared counter =====");
        synchronizedDemo();

        System.out.println("\n===== 3. PESSIMISTIC LOCKING: ReentrantLock seat booking =====");
        pessimisticLockingDemo();

        System.out.println("\n===== 4. OPTIMISTIC LOCKING: CAS wallet debit =====");
        optimisticLockingDemo();

        System.out.println("\n===== 5. READ-WRITE LOCK: concurrent reads, exclusive writes =====");
        readWriteLockDemo();

        System.out.println("\n===== 6. EXECUTORSERVICE: thread pool + Future =====");
        executorServiceDemo();
    }

    // ---------------------------------------------------------------
    // 1. THREADS
    // ---------------------------------------------------------------
    static void threadBasicsDemo() throws InterruptedException, ExecutionException {
        // Runnable - fire and forget, no return value
        Runnable task = () ->
                System.out.println("[Runnable] running on " + Thread.currentThread().getName());
        Thread t1 = new Thread(task, "worker-1");
        t1.start();
        t1.join(); // wait for it to finish

        // Callable - returns a result and can throw checked exceptions
        Callable<Integer> calc = () -> {
            Thread.sleep(50);
            return 21 * 2;
        };
        FutureTask<Integer> futureTask = new FutureTask<>(calc);
        Thread t2 = new Thread(futureTask, "worker-2");
        t2.start();
        System.out.println("[Callable] result = " + futureTask.get()); // blocks until done
    }

    // ---------------------------------------------------------------
    // 2. SYNCHRONIZED
    // ---------------------------------------------------------------
    static class Counter {
        private int count = 0;

        // method-level synchronized -> intrinsic lock on 'this'
        public synchronized void increment() {
            count++;
        }

        public int get() {
            return count;
        }
    }

    static void synchronizedDemo() throws InterruptedException {
        Counter counter = new Counter();
        int threads = 10;
        int incrementsPerThread = 1000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        int expected = threads * incrementsPerThread;
        System.out.println("expected = " + expected + ", actual = " + counter.get());
        System.out.println("synchronized increment() guarantees these always match");
        System.out.println("(remove 'synchronized' and re-run -> actual will drift below expected)");
    }

    // ---------------------------------------------------------------
    // 3. PESSIMISTIC LOCKING (ReentrantLock) - seat booking
    // ---------------------------------------------------------------
    static class Seat {
        final String seatId;
        boolean available = true;
        String bookedBy;

        Seat(String seatId) {
            this.seatId = seatId;
        }
    }

    static class SeatInventory {
        private final Map<String, Seat> seats = new HashMap<>();
        private final Map<String, ReentrantLock> seatLocks = new HashMap<>();

        SeatInventory(String... seatIds) {
            for (String id : seatIds) {
                seats.put(id, new Seat(id));
                seatLocks.put(id, new ReentrantLock());
            }
        }

        /**
         * Pessimistic: acquire the lock BEFORE reading. Whoever gets the
         * lock first wins; everyone else simply blocks - no retries needed,
         * but throughput drops under contention.
         */
        boolean bookSeat(String seatId, String userId) {
            ReentrantLock lock = seatLocks.get(seatId);

            // tryLock with timeout avoids waiting forever if something is stuck
            boolean acquired;
            try {
                acquired = lock.tryLock(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (!acquired) {
                System.out.println(userId + " could not get lock for " + seatId + " (timeout)");
                return false;
            }

            try {
                Seat seat = seats.get(seatId);
                if (!seat.available) {
                    System.out.println(userId + " FAILED  - " + seatId
                            + " already booked by " + seat.bookedBy);
                    return false;
                }
                // simulate some processing time inside the critical section
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}

                seat.available = false;
                seat.bookedBy = userId;
                System.out.println(userId + " SUCCESS - booked " + seatId);
                return true;
            } finally {
                lock.unlock(); // ALWAYS release in finally
            }
        }
    }

    static void pessimisticLockingDemo() throws InterruptedException {
        SeatInventory inventory = new SeatInventory("A1");
        ExecutorService pool = Executors.newFixedThreadPool(5);

        // 5 users race for the same seat "A1" - only one should win
        for (int i = 1; i <= 5; i++) {
            String userId = "user-" + i;
            pool.submit(() -> inventory.bookSeat("A1", userId));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ---------------------------------------------------------------
    // 4. OPTIMISTIC LOCKING (CAS) - wallet debit
    // ---------------------------------------------------------------
    static class Wallet {
        // AtomicInteger gives us compareAndSet (CAS) - the building block of
        // optimistic locking. No lock is ever held while reading.
        private final AtomicInteger balance;

        Wallet(int initialBalance) {
            this.balance = new AtomicInteger(initialBalance);
        }

        /**
         * Optimistic: read current value, compute new value, then try to
         * write it back only if nobody changed it in between. If they did,
         * retry from scratch.
         */
        boolean debit(int amount, String who) {
            int retries = 0;
            while (true) {
                int current = balance.get();             // 1. READ (no lock)
                if (current < amount) {
                    System.out.println(who + " FAILED  - insufficient balance (" + current + ")");
                    return false;
                }
                int updated = current - amount;           // 2. COMPUTE

                // 3. WRITE only if balance is still 'current' (CAS)
                if (balance.compareAndSet(current, updated)) {
                    System.out.println(who + " SUCCESS - debited " + amount
                            + ", new balance = " + updated + " (retries=" + retries + ")");
                    return true;
                }
                // someone else updated balance between our read and write -> retry
                retries++;
            }
        }
    }

    static void optimisticLockingDemo() throws InterruptedException {
        Wallet wallet = new Wallet(100);
        ExecutorService pool = Executors.newFixedThreadPool(5);

        // 5 threads each try to debit 30 from a wallet that only has 100
        // -> only 3 can succeed, the rest fail on insufficient balance
        for (int i = 1; i <= 5; i++) {
            String who = "txn-" + i;
            pool.submit(() -> wallet.debit(30, who));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ---------------------------------------------------------------
    // 5. READ-WRITE LOCK
    // ---------------------------------------------------------------
    static class PriceCache {
        private final Map<String, Double> prices = new HashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        PriceCache() {
            prices.put("AAPL", 190.0);
        }

        // many threads can call this concurrently - readers don't block each other
        double getPrice(String symbol) {
            rwLock.readLock().lock();
            try {
                double price = prices.get(symbol);
                System.out.println(Thread.currentThread().getName() + " reading price = " + price);
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                return price;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        // exclusive - blocks all readers and writers until this completes
        void updatePrice(String symbol, double newPrice) {
            rwLock.writeLock().lock();
            try {
                System.out.println(Thread.currentThread().getName() + " WRITING new price = " + newPrice);
                prices.put(symbol, newPrice);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    static void readWriteLockDemo() throws InterruptedException {
        PriceCache cache = new PriceCache();
        ExecutorService pool = Executors.newFixedThreadPool(6);

        // 5 readers run mostly in parallel
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> cache.getPrice("AAPL"));
        }
        // 1 writer - waits for in-flight readers, then blocks new readers until done
        pool.submit(() -> cache.updatePrice("AAPL", 195.5));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ---------------------------------------------------------------
    // 6. EXECUTORSERVICE
    // ---------------------------------------------------------------
    static void executorServiceDemo() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            int taskId = i;
            tasks.add(() -> {
                Thread.sleep(50);
                return taskId * taskId;
            });
        }

        // submit() + Future - track a single task's result
        Future<Integer> future = pool.submit(tasks.get(0));
        System.out.println("submit() result = " + future.get());

        // invokeAll() - waits for ALL tasks to complete, returns Futures
        List<Future<Integer>> results = pool.invokeAll(tasks);
        for (Future<Integer> f : results) {
            System.out.println("invokeAll() result = " + f.get());
        }

        // graceful shutdown pattern
        pool.shutdown();
        if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
            pool.shutdownNow();
        }
        System.out.println("pool shut down: " + pool.isShutdown());
    }
}
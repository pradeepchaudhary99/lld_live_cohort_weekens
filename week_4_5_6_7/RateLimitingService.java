package week_4_5_6_7;

import java.util.concurrent.ConcurrentHashMap;

interface RateLimitingStrategy{
    boolean isAllowed(Request request);
        // Request  // UserID, IP, Email, 
        // 
}


class TokenBucket {

    // Bucket object per user
    private static class Bucket {
        double tokens;
        long lastRefillTime;

        Bucket(double tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }

    private final double capacity;
    private final double refillRatePerSec;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucket(double capacity, double refillRatePerSec) {
        this.capacity = capacity;
        this.refillRatePerSec = refillRatePerSec;
    }

    public synchronized boolean allowRequest(String userId) {
        Bucket bucket = buckets.computeIfAbsent(userId,
                k -> new Bucket(capacity, System.currentTimeMillis()));

        long now = System.currentTimeMillis();
        double elapsed = (now - bucket.lastRefillTime) / 1000.0;

        // Refill tokens
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * refillRatePerSec);
        bucket.lastRefillTime = now;

        if (bucket.tokens >= 1.0) {
            bucket.tokens -= 1.0;
            return true;  // allowed
        }

        return false; // rate limited
    }
}

class LeakyBucket {

    private static class Bucket {
        double queue;           // current number of requests waiting
        long lastLeakTime;      // last time we drained the queue

        Bucket(double queue, long lastLeakTime) {
            this.queue = queue;
            this.lastLeakTime = lastLeakTime;
        }
    }

    private final double capacity;
    private final double leakRatePerSec;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LeakyBucket(double capacity, double leakRatePerSec) {
        this.capacity = capacity;
        this.leakRatePerSec = leakRatePerSec;
    }

    public synchronized boolean allowRequest(String userId) {
        Bucket bucket = buckets.computeIfAbsent(userId,
                k -> new Bucket(0, System.currentTimeMillis()));

        long now = System.currentTimeMillis();
        double elapsed = (now - bucket.lastLeakTime) / 1000.0;

        // Drain the bucket based on elapsed time
        bucket.queue = Math.max(0, bucket.queue - elapsed * leakRatePerSec);
        bucket.lastLeakTime = now;

        if (bucket.queue < capacity) {
            bucket.queue += 1.0;  // enqueue request
            return true;          // allowed
        }

        return false; // bucket full → drop request
    }
}




public class RateLimitingService {
    
}
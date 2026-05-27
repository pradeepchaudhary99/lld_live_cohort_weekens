package week_4_5_6_7;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

interface IRateLimiter{
      boolean isAllowed(String userId);
}



class TokenBucket implements IRateLimiter{

        private static class Bucket{
            int tokens;
            long lastRefillTime;

            Bucket(int capacity){
                this.tokens = capacity;
                lastRefillTime = System.currentTimeMillis();
            }
        }

        ConcurrentHashMap<String,Bucket> buckets = new ConcurrentHashMap<>();
        private int capacity;
        private int refillRate;

        public TokenBucket(int capacity, int refillRate){
            this.capacity = capacity;
            this.refillRate = refillRate;
        }
    

        @Override
        public boolean isAllowed(String userId) {

            synchronized(this){
                Bucket bucket = buckets.computeIfAbsent(userId, k -> new Bucket(capacity));
                long now = System.currentTimeMillis();
                long elapsed = (now - bucket.lastRefillTime)/1000;

                bucket.tokens = Math.min(capacity, bucket.tokens + (int)elapsed * refillRate);
                bucket.lastRefillTime = System.currentTimeMillis();

                // check if we have sufficient tokens or not
                if(bucket.tokens > 0){
                    bucket.tokens--;
                    return true;
                }
                else{
                    return false;
                }
        }

        }

}


class SlidingWindow implements IRateLimiter{

    private static class Window{
        Deque<Long> timestamps = new ArrayDeque<>();
    }

    int limit;
    long windowSizeMs;   

    public SlidingWindow(int limit, long windowSizeMs){{
        this.limit = limit;
        this.windowSizeMs = windowSizeMs;
    }}

    ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();


    @Override
    public boolean isAllowed(String userId) {
        
        long now = System.currentTimeMillis();
        long windowStart = now - windowSizeMs;

        Window window = windows.computeIfAbsent(userId, k -> new Window());

        // Evict all the invalid timestamps present in the window 
        while(!window.timestamps.isEmpty() && window.timestamps.peekFirst() <= windowStart){
            window.timestamps.pollFirst();
        }

        if(window.timestamps.size() < limit){
            now = System.currentTimeMillis();
            window.timestamps.addLast(now);
            return true;
        }else{
            return false;
        }
    }
    
}


public class RateLimiter_class {
    public static void main(String[] args) {
        IRateLimiter rateLimiter = new TokenBucket(10, 1);
        for(int i = 1; i <= 50; i++){
            if(rateLimiter.isAllowed("1")){
                System.out.println("Request Number: "+i +" Allowed");
            }else{
                System.out.println("Request Number: "+i + " Droppped");
            }
        }
    }
}

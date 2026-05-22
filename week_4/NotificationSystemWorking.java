import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// ── Enums ─────────────────────────────────────────────────────────────────────
enum NotificationType { ORDER_PLACED, ORDER_SHIPPED, PAYMENT_SUCCESS, PROMOTION }
enum Channel { EMAIL, SMS, PUSH }

// ── Notification — value object ───────────────────────────────────────────────
class Notification {
    final String userId;
    final NotificationType type;
    final String message;
    final long timestamp;

    Notification(String userId, NotificationType type, String message) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}

// ── NotificationChannel — base interface ──────────────────────────────────────
interface NotificationChannel {
    boolean send(String userId, String message);
}

// ── Base Channels ─────────────────────────────────────────────────────────────
class EmailChannel implements NotificationChannel {
    private static final Random random = new Random();
    
    public boolean send(String userId, String message) {
        // Simulate occasional failure (20% failure rate)
        if (random.nextInt(100) < 20) {
            System.out.println("[EMAIL → " + userId + "] FAILED");
            return false;
        }
        System.out.println("[EMAIL → " + userId + "] " + message);
        return true;
    }
}

class SmsChannel implements NotificationChannel {
    public boolean send(String userId, String message) {
        System.out.println("[SMS → " + userId + "] " + message);
        return true;
    }
}

class PushChannel implements NotificationChannel {
    public boolean send(String userId, String message) {
        System.out.println("[PUSH → " + userId + "] " + message);
        return true;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DECORATOR PATTERN — Wrap channels with cross-cutting concerns
// ══════════════════════════════════════════════════════════════════════════════

// ── RetryDecorator — retries failed sends with exponential backoff ────────────
class RetryDecorator implements NotificationChannel {
    private final NotificationChannel wrapped;
    private final int maxRetries;
    private final long initialBackoffMs;

    RetryDecorator(NotificationChannel wrapped, int maxRetries, long initialBackoffMs) {
        this.wrapped = wrapped;
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
    }

    public boolean send(String userId, String message) {
        int attempt = 0;
        while (attempt < maxRetries) {
            boolean success = wrapped.send(userId, message);
            if (success) return true;

            attempt++;
            if (attempt < maxRetries) {
                long backoff = initialBackoffMs * (1L << (attempt - 1)); // exponential: 100, 200, 400
                System.out.println("  ↻ Retry " + attempt + " after " + backoff + "ms");
                try { Thread.sleep(backoff); } catch (InterruptedException e) { }
            }
        }
        System.out.println("  ✗ Failed after " + maxRetries + " retries");
        return false;
    }
}

// ── RateLimitDecorator — throttles sends per user ─────────────────────────────
class RateLimitDecorator implements NotificationChannel {
    private final NotificationChannel wrapped;
    private final int maxPerMinute;
    
    // userId → count in current minute
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    RateLimitDecorator(NotificationChannel wrapped, int maxPerMinute) {
        this.wrapped = wrapped;
        this.maxPerMinute = maxPerMinute;
    }

    public boolean send(String userId, String message) {
        AtomicInteger count = counts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        
        if (count.incrementAndGet() > maxPerMinute) {
            System.out.println("[RATE LIMIT] Blocked notification to " + userId);
            return false;
        }
        
        return wrapped.send(userId, message);
    }

    // Reset counts every minute (simplified — production would use sliding window)
    public void resetCounts() {
        counts.clear();
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// User Preferences
// ══════════════════════════════════════════════════════════════════════════════

class UserPreferenceService {
    private final Map<String, Map<NotificationType, Set<Channel>>> preferences = new ConcurrentHashMap<>();

    public void setPreference(String userId, NotificationType type, Set<Channel> channels) {
        preferences
            .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
            .put(type, new HashSet<>(channels));
    }

    public Set<Channel> getPreferences(String userId, NotificationType type) {
        Map<NotificationType, Set<Channel>> userPrefs = preferences.get(userId);
        if (userPrefs == null) return getDefaultChannels(type);
        
        Set<Channel> channels = userPrefs.get(type);
        return (channels != null) ? channels : getDefaultChannels(type);
    }

    private Set<Channel> getDefaultChannels(NotificationType type) {
        switch (type) {
            case ORDER_PLACED:
            case ORDER_SHIPPED:
                return Set.of(Channel.EMAIL, Channel.PUSH);
            case PAYMENT_SUCCESS:
                return Set.of(Channel.EMAIL, Channel.SMS);
            case PROMOTION:
                return Set.of(Channel.EMAIL);
            default:
                return Set.of(Channel.EMAIL);
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// NotificationService — ASYNC with decorators
// ══════════════════════════════════════════════════════════════════════════════

class NotificationService {
    private final Map<Channel, NotificationChannel> channels = new HashMap<>();
    private final UserPreferenceService preferenceService;
    private final ExecutorService executor;

    NotificationService(UserPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
        this.executor = Executors.newFixedThreadPool(4); // async thread pool

        // Build decorated channels — Retry(RateLimit(BaseChannel))
        NotificationChannel email = new EmailChannel();
        email = new RateLimitDecorator(email, 5); // max 5 per minute
        email = new RetryDecorator(email, 3, 100); // 3 retries, 100ms initial backoff
        channels.put(Channel.EMAIL, email);

        NotificationChannel sms = new SmsChannel();
        sms = new RateLimitDecorator(sms, 3); // max 3 per minute
        sms = new RetryDecorator(sms, 2, 200); // 2 retries, 200ms backoff
        channels.put(Channel.SMS, sms);

        NotificationChannel push = new PushChannel();
        push = new RateLimitDecorator(push, 10); // max 10 per minute
        channels.put(Channel.PUSH, push); // no retry for push (best-effort)
    }

    // Send async — returns immediately, actual send happens on worker thread
    public void send(Notification notification) {
        Set<Channel> userChannels = preferenceService.getPreferences(
            notification.userId, 
            notification.type
        );

        System.out.println("\n[ASYNC] Queuing " + notification.type + " to " + notification.userId 
            + " via " + userChannels);

        // Submit to thread pool for each channel
        for (Channel ch : userChannels) {
            NotificationChannel channel = channels.get(ch);
            if (channel != null) {
                executor.submit(() -> {
                    try {
                        channel.send(notification.userId, notification.message);
                    } catch (Exception e) {
                        System.err.println("Channel failed: " + e.getMessage());
                    }
                });
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Main — Demonstration
// ══════════════════════════════════════════════════════════════════════════════

public class NotificationSystemWorking {
    public static void main(String[] args) throws InterruptedException {
        UserPreferenceService prefService = new UserPreferenceService();
        NotificationService notificationService = new NotificationService(prefService);

        // ═══════════════════════════════════════════════════════════════════
        // DEMO 1: Async Sending — returns immediately
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("═══ DEMO 1: Async Sending ═══");
        
        Notification order1 = new Notification("alice", NotificationType.ORDER_PLACED, 
            "Order #001 placed");
        notificationService.send(order1);
        
        System.out.println("Main thread continues immediately...\n");
        
        Thread.sleep(1000); // wait for async sends to complete

        // ═══════════════════════════════════════════════════════════════════
        // DEMO 2: Retry Logic — email has 20% failure rate, will retry
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ DEMO 2: Retry Logic (watch for retries) ═══");
        
        for (int i = 0; i < 5; i++) {
            Notification n = new Notification("bob", NotificationType.PAYMENT_SUCCESS, 
                "Payment #" + i + " processed");
            notificationService.send(n);
        }
        
        Thread.sleep(3000); // wait for retries to complete

        // ═══════════════════════════════════════════════════════════════════
        // DEMO 3: Rate Limiting — 6th email blocked (limit = 5/min)
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ DEMO 3: Rate Limiting (6th notification blocked) ═══");
        
        prefService.setPreference("charlie", NotificationType.PROMOTION, Set.of(Channel.EMAIL));
        
        for (int i = 1; i <= 6; i++) {
            Notification promo = new Notification("charlie", NotificationType.PROMOTION, 
                "Promo #" + i);
            notificationService.send(promo);
        }
        
        Thread.sleep(2000);

        // ═══════════════════════════════════════════════════════════════════
        // DEMO 4: Multiple channels with different decorators
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ DEMO 4: Multiple Channels (EMAIL retry, SMS no retry) ═══");
        
        prefService.setPreference("david", NotificationType.ORDER_SHIPPED, 
            Set.of(Channel.EMAIL, Channel.SMS, Channel.PUSH));
        
        Notification shipped = new Notification("david", NotificationType.ORDER_SHIPPED, 
            "Your order has shipped!");
        notificationService.send(shipped);
        
        Thread.sleep(2000);

        // ═══════════════════════════════════════════════════════════════════
        // Cleanup
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ Shutting down ═══");
        notificationService.shutdown();
    }
}
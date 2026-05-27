package week_4_5_6_7;
import java.util.*;

/*
2. Functional & Non-Functional Requirements
Functional Requirements
    Generate a short URL for a given long URL
    Redirect user from short URL → original URL
    Optional: Custom alias support (user-defined short URL)
    Optional: Expiry time for links
    Optional: Track number of clicks

Non-Functional Requirements
    High availability (reads >> writes)
    Low latency redirection
    Scalability (millions of URLs)
    Uniqueness of short URLs
    Fault tolerance
    Storage optimization

3. Core Entities
URL
id
longUrl
shortCode
createdAt
expiryAt
accessCount
URLRepository (in-memory for now)
URLService
ShortCodeGenerator
*/
// Entity
class URL {
    private String longUrl;
    private String shortCode;
    private long createdAt;
    private long expiryAt;
    private int accessCount;

    public URL(String longUrl, String shortCode, long expiryAt) {
        this.longUrl = longUrl;
        this.shortCode = shortCode;
        this.createdAt = System.currentTimeMillis();
        this.expiryAt = expiryAt;
        this.accessCount = 0;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public long getExpiryAt() {
        return expiryAt;
    }

    public void incrementAccessCount() {
        accessCount++;
    }

    public int getAccessCount() {
        return accessCount;
    }
}

// Strategy Pattern
interface ShortCodeGenerator {
    String generate(String longUrl);
}

// Base62 Generator
class Base62Generator implements ShortCodeGenerator {
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private Random random = new Random();

    public String generate(String longUrl) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}

// Singleton Repository
class URLRepository {
    private static URLRepository instance;
    private Map<String, URL> map = new HashMap<>();

    private URLRepository() {}

    public static URLRepository getInstance() {
        if (instance == null) {
            instance = new URLRepository();
        }
        return instance;
    }

    public void save(URL url) {
        map.put(url.getShortCode(), url);
    }

    public URL find(String shortCode) {
        return map.get(shortCode);
    }

    public boolean exists(String shortCode) {
        return map.containsKey(shortCode);
    }
}

/*
URLService
ShortenURL
resolveURL

*/

// Service
class URLService {
    private URLRepository repo = URLRepository.getInstance();
    private ShortCodeGenerator generator;

    public URLService(ShortCodeGenerator generator) {
        this.generator = generator;
    }

    public String shortenURL(String longUrl, long expiryMillis) {
        String shortCode;

        // Ensure uniqueness
        do {
            shortCode = generator.generate(longUrl);
        } while (repo.exists(shortCode));

        URL url = new URL(longUrl, shortCode, expiryMillis);
        repo.save(url);

        return shortCode;
    }

    public String resolveURL(String shortCode) {
        URL url = repo.find(shortCode);

        if (url == null) {
            return "URL not found";
        }

        if (url.getExpiryAt() != -1 && System.currentTimeMillis() > url.getExpiryAt()) {
            return "URL expired";
        }

        url.incrementAccessCount();
        return url.getLongUrl();
    }
}

// Demo
public class URLSHORTNER_DEMO {
    public static void main(String[] args) {
        URLService service = new URLService(new Base62Generator());

        String shortUrl = service.shortenURL("https://example.com/very/long/url", -1);
        System.out.println("Short URL: " + shortUrl);

        String original = service.resolveURL(shortUrl);
        System.out.println("Original URL: " + original);
    }
}
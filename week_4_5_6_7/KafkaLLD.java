import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ============================================================
 *  KAFKA - LOW LEVEL DESIGN (Single File, Simplified MVP)
 * ============================================================
 *
 * Core Entities:
 *   - Message        : unit of data (key, value, offset, timestamp)
 *   - Partition      : ordered, append-only log (thread-safe)
 *   - Topic          : logical stream made of N partitions
 *   - Partitioner    : STRATEGY pattern - decides target partition
 *   - Broker         : SINGLETON - owns all topics
 *   - Producer       : publishes messages
 *   - ConsumerGroup  : manages partition assignment + rebalancing
 *   - Consumer       : pulls messages, tracks its OWN offsets locally
 *

 * If interviewer pushes on "what if consumer crashes and restarts?":
 *   -> say you'd reintroduce a small OffsetStore (could literally be a
 *      Map<groupId, Map<topic#partition, offset>>) and persist it
 *      (DB / compacted Kafka topic in real Kafka, called __consumer_offsets).
 *
 * Design Patterns Used:
 *   - Strategy   -> Partitioner (Hash / RoundRobin / pluggable custom)
 *   - Singleton  -> Broker
 */
public class KafkaLLD {

    public static void main(String[] args) {
        Broker broker = Broker.getInstance();
        broker.createTopic("orders", 3, new HashPartitioner());

        Producer producer = new Producer(broker);
        ConsumerGroup group = new ConsumerGroup(broker.getTopic("orders"));

        Consumer c1 = new Consumer("consumer-1", group);
        Consumer c2 = new Consumer("consumer-2", group);
        group.addConsumer(c1);
        group.addConsumer(c2);

        for (int i = 0; i < 10; i++) {
            producer.send("orders", "user-" + (i % 3), "order-event-" + i);
        }

        System.out.println("c1 -> " + c1.poll(10));
        System.out.println("c2 -> " + c2.poll(10));

        // second poll returns nothing new, since offsets already advanced
        System.out.println("c1 again -> " + c1.poll(10));
    }
}

// ================= MESSAGE =================
class Message {
    private final String key;   //used to find the partition id 
    private final String value;
    private final long timestamp;
    private long offset = -1; //index of this message from the producer 
    private int partitionId = -1;

    public Message(String key, String value) {
        this.key = key;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public long getTimestamp() { return timestamp; }
    public long getOffset() { return offset; }
    void setOffset(long offset) { this.offset = offset; }
    public int getPartitionId() { return partitionId; }
    void setPartitionId(int partitionId) { this.partitionId = partitionId; }

    @Override
    public String toString() {
        return String.format("Msg{p=%d, off=%d, key=%s, val=%s}", partitionId, offset, key, value);
    }
}

// ================= PARTITIONER (STRATEGY) =================
interface Partitioner {
    int partition(String key, int numPartitions);
}

class HashPartitioner implements Partitioner {
    @Override
    public int partition(String key, int numPartitions) {
        if (key == null) {
            return ThreadLocalRandom.current().nextInt(numPartitions);
        }
        return Math.abs(key.hashCode()) % numPartitions;
    }
}

class RoundRobinPartitioner implements Partitioner {
    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public int partition(String key, int numPartitions) {
        return (int) (counter.getAndIncrement() % numPartitions);
    }
}

// ================= PARTITION (append-only log) =================
class Partition {
    private final int id;
    // In-memory stand-in for a segmented, file-backed Write-Ahead Log (WAL).
    private final List<Message> log = new CopyOnWriteArrayList<>();
    private final AtomicLong nextOffset = new AtomicLong(0);
    /*
    acts as the write pointer for that partition's log — it tells you what offset the next message appended will get.
    Why AtomicLong and not just long? Because multiple producers could call append() concurrently.
    */
    public Partition(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    public synchronized long append(Message message) {
        long offset = nextOffset.getAndIncrement();
        message.setOffset(offset);
        message.setPartitionId(id);
        log.add(message);
        return offset;
    }

    public List<Message> readFrom(long fromOffset, int maxMessages) {
        List<Message> result = new ArrayList<>();
        for (int i = (int) fromOffset; i < log.size() && result.size() < maxMessages; i++) {
            result.add(log.get(i));
        }
        return result;
    }

    public long getLatestOffset() {
        return nextOffset.get();
    }
}

// ================= TOPIC =================
class Topic {
    private final String name;
    private final List<Partition> partitions;
    private final Partitioner partitioner;

    public Topic(String name, int numPartitions, Partitioner partitioner) {
        this.name = name;
        this.partitioner = partitioner;
        this.partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new Partition(i));
        }
    }

    public String getName() { return name; }
    public int getNumPartitions() { return partitions.size(); }
    public Partition getPartition(int id) { return partitions.get(id); }

    public Message publish(Message message) {
        int partitionId = partitioner.partition(message.getKey(), partitions.size());
        partitions.get(partitionId).append(message);
        return message;
    }
}

// ================= BROKER (SINGLETON) =================
class Broker {
    private static final Broker INSTANCE = new Broker();
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();

    private Broker() {}

    public static Broker getInstance() {
        return INSTANCE;
    }

    public void createTopic(String name, int numPartitions, Partitioner partitioner) {
        topics.putIfAbsent(name, new Topic(name, numPartitions, partitioner));
    }

    public Topic getTopic(String name) {
        Topic topic = topics.get(name);
        if (topic == null) throw new IllegalArgumentException("Unknown topic: " + name);
        return topic;
    }
}

// ================= PRODUCER =================
class Producer {
    private final Broker broker;

    public Producer(Broker broker) {
        this.broker = broker;
    }

    public Message send(String topicName, String key, String value) {
        Topic topic = broker.getTopic(topicName);
        return topic.publish(new Message(key, value));
    }
}

// ================= CONSUMER GROUP =================

/*
    ConsumerGroup is the thing that coordinates multiple consumers 
    reading the same topic together so they split the work instead of 
    each reading everything.
*/
class ConsumerGroup {
    private final Topic topic;
    private final List<Consumer> consumers = new ArrayList<>();
    // consumerId -> assigned partition ids
    private final Map<String, List<Integer>> assignment = new HashMap<>();

    public ConsumerGroup(Topic topic) {
        this.topic = topic;
    }

    public Topic getTopic() { return topic; }

    public synchronized void addConsumer(Consumer consumer) {
        consumers.add(consumer);
        rebalance();
    }

    public synchronized void removeConsumer(Consumer consumer) {
        consumers.remove(consumer);
        rebalance();
    }

    // Simple range-based rebalance: split partitions as evenly as possible.
    // Real Kafka offers Range, RoundRobin, Sticky, CooperativeSticky assignors.
    //Rebalancing is what happens whenever membership changes (someone joins or leaves):

    private void rebalance() {
        assignment.clear();
        if (consumers.isEmpty()) return;

        int numPartitions = topic.getNumPartitions();
        int numConsumers = consumers.size();

        for (Consumer c : consumers) {
            assignment.put(c.getId(), new ArrayList<>());
        }
        for (int p = 0; p < numPartitions; p++) {
            String consumerId = consumers.get(p % numConsumers).getId();
            assignment.get(consumerId).add(p);
        }
    }

    public synchronized List<Integer> getAssignedPartitions(String consumerId) {
        return assignment.getOrDefault(consumerId, Collections.emptyList());
    }
}

// ================= CONSUMER =================


/*
Step 1 — find out what I own. group.getAssignedPartitions(id) asks the ConsumerGroup which partitions belong to this consumer. From the earlier rebalance trace, consumer-1 owns [0, 2] and consumer-2 owns [1].
Step 2 — for each owned partition, find my bookmark. nextOffsetByPartition.getOrDefault(partitionId, 0L) checks "where did I leave off?" On the first poll ever, this map is empty, so every partition starts at offset 0.
Step 3 — fetch messages from that bookmark onward. partition.readFrom(fromOffset, maxMessagesPerPartition) reads up to maxMessagesPerPartition messages starting at fromOffset. This is just slicing the partition's in-memory log from index fromOffset onward.
Step 4 — collect everything across all owned partitions. Everything gets appended into result — so a single poll() call can return messages from multiple partitions at once, all merged into one list.
Step 5 — advance my bookmark. If we got any messages, the bookmark moves to (last message's offset) + 1 — i.e., "the next message I haven't read yet."

*/
class Consumer {
    private final String id;
    private final ConsumerGroup group;
    // Local, in-memory offset tracking: partitionId -> next offset to read.
    // This replaces OffsetManager. Simple, but not durable across restarts.
    private final Map<Integer, Long> nextOffsetByPartition = new HashMap<>();

    public Consumer(String id, ConsumerGroup group) {
        this.id = id;
        this.group = group;
    }

    public String getId() { return id; }

    // Pull-based: consumer decides when and how much to fetch.
    public List<Message> poll(int maxMessagesPerPartition) {
        List<Message> result = new ArrayList<>();
        Topic topic = group.getTopic();

        for (int partitionId : group.getAssignedPartitions(id)) {
            Partition partition = topic.getPartition(partitionId);
            long fromOffset = nextOffsetByPartition.getOrDefault(partitionId, 0L);

            List<Message> messages = partition.readFrom(fromOffset, maxMessagesPerPartition);
            result.addAll(messages);

            if (!messages.isEmpty()) {
                long newOffset = messages.get(messages.size() - 1).getOffset() + 1;
                nextOffsetByPartition.put(partitionId, newOffset);
            }
        }
        return result;
    }
}
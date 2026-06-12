package week_4_5_6_7;


/*
Producer features 
- producer can publish messages to a topic 
- messages are appended sequentially

Consumer features: 
    consumer consumes messages from a topic 
    multiple consumers can exist 
    consumer consumes from a specific offset 

Topic Features: 
 - create topics 
 - topic contains multiple partitions 
 - messages are stored inside the partition 
 

Producer 
Consumer 
Topic 
Partition 
Message 

ConsumerGroup 
Consumer 

OffsetManager 

Broke




 Broker 
    List<Topic>
    







*/



class Message{
    String topic; 
    String value;
    String user_id;
}

class Partition{
    int partitionId;
    List<Message> messages;

}

class Topic{
    List<Partition> partions; 
}



class Broker{
    Map<String,Topic> topics;
    createTopic();
    getDataFromTopic(topicName);
    putDataToATopic();
    deleteTopic();
}





public class Kafka_lld_class {
    public static void main(String[] args) {
        
    }
}


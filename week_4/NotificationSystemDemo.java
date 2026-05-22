package week_4;

public 

/*
Notification System
    ---> 
Functional Requirements:
1. should be able to send notification
2. should allow multiple channels (Email, SMS, Push)
3. should respect the UserPreference
4. Retry Logic, Rate Limiter for better reliability, and stopping the Abuse of the service.


Non-Functional Requirments:
    1. Thread-safety
    2. Async dispatching the notification
    3. extensibility
    4. logging and observability

*/

/*

Notification --> message
NotificationType  --> Enum [ORDER_PLACED, TRANSACTION]
Channel ---> Interface
UserPreferenceService
    --> Map<UserId, Map<NotificationType, List<Channel>>>
EmailChannel
SMSChannel 
PushChannel
RetryDecorator
RateLimiterDecorator
NotificationService 

---- 10-15 minute 

Relationship:

    NotificationService 
        --> Owns  ---> UserPreference
        --> Map<Channel, NotificationChannel>
    
    //


*/

enum NotificationType{ORDER_PLACED, TRANSACTION};

class Notification{
    String userId;  //private 
    NotificationType type; // private 
    String message;
    long timestamp;
}



interface NotifcationChannel{
    boolean send(Notification notification);
}

class EmailNotification implements NotifcationChannel{
    @Override
    public boolean send(Notification notification) {

    }
}

class SMSNotification implements NotifcationChannel{
    @Override
    public boolean send(Notification notification) {

    }
}

class SMSNotification implements NotifcationChannel{
    @Override
    public boolean send(Notification notification) {

    }
}

class RetryDecorator implements NotificationChannel{

    NotificationChannel wrapped; 

    RetryDecorator(NotificationChannel wrapped){
        this.wrapped = wrapped;
    }

    boolean send(Notification notification){


        for(int i = 0; i < retryCount; i++){
            wrapped.send(notification);
        }
    }
}


public class NotificationSystemDemo {
    
}

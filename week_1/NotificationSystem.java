package week_1;
// import javax.management.Notification;

interface NotificationChannel{
    void sendNotification(String message);
}

class Whatsapp implements NotificationChannel{
    @Override
    public void sendNotification(String message) {
        System.out.println("Whatsapp");
    }
}

class SMS implements NotificationChannel{
    @Override
    public void sendNotification(String message) {
        System.out.println("SMS is sent");
    }
}

class SendNotificationService{
    NotificationChannel channel;
    public SendNotificationService(NotificationChannel channel){
        this.channel = channel;
    }
    void setChannel(NotificationChannel channel){
        this.channel = channel;
    }

    void sendNotification(String message){
        channel.sendNotification(message);
    }
}


public class NotificationSystem {
    public static void main(String[] args) {
        SendNotificationService service = new SendNotificationService(new SMS());
        service.sendNotification("this is a sample sms");
        service.changeChannel(new Whatsapp());
        service.sendNotification("this is a sample sms");
    }
}

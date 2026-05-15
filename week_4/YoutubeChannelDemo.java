package week_4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

interface IObserver{
    public void notify(int value);
}

class Tanuj implements IObserver{
    public void notify(int value){
        System.out.println("tanuj Value is changed" + value);
    }
}

class Kamal implements IObserver{
    public void notify(int value){
        System.out.println("Kamal Value is changed" + value);
    }
}

interface IObservable{
    void addObserver(IObserver o);
    void deleteObserver(IObserver o);
    void notifyObserver(int value);
}

class YoutubeChannel implements IObservable{
    List<IObserver> subscribers;
    public YoutubeChannel(){
        subscribers = new ArrayList<>();
    }
    void setState(){
        notifyObserver(1232131);
    }
    void videoUpload(int videoId){
        System.out.println("Video uploaded");
        notifyObserver(videoId);
    }
    @Override
    public void addObserver(IObserver o) {
        subscribers.add(o);
    }

    @Override
    public void deleteObserver(IObserver o) {
        subscribers.remove(o);
    }

    @Override
    public void notifyObserver(int value) {
        for(IObserver observer : subscribers){
            observer.notify(value);
        }
    }

}






public class YoutubeChannelDemo {
    public static void main(String[] args) {
        IObserver observer1 = new Tanuj();
        IObserver observer2 = new Kamal();

        YoutubeChannel youtubeChannel = new YoutubeChannel();
        youtubeChannel.addObserver(observer1);
        youtubeChannel.addObserver(observer2);

        youtubeChannel.videoUpload(122324);

        
    }
}

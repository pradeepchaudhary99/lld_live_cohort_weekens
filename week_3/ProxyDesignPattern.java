package week_3;
// package week4;

import java.util.HashMap;
import java.util.Map;

//Step1: Subject Interface
interface VideoService{
    public String getVideo(String videoId);
}

// step2: Real Object [Expensive Operation]

class RealVideoService implements VideoService{
    @Override
    public String getVideo(String videoId){
        //Simulate the latency
        System.out.println("Request --> "+ videoId);
        simulateNetworkLatency();
        return "Video for:" + videoId;
    }

    private void simulateNetworkLatency(){
        try{
            System.out.println("Fetching from remote server ..... ");
            Thread.sleep(2000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}


// Step3: Proxy with Cache

class VideoServiceProxy implements VideoService{
    private RealVideoService realService;
    private Map<String, String> cache;

    public VideoServiceProxy(){
        this.realService = new RealVideoService();
        this.cache = new HashMap<>();
    }

    @Override
    public String getVideo(String videoId){

        if(cache.containsKey(videoId)){
            System.out.println("Returning from the cache");
            return cache.get(videoId);
        }

        // if not in cache --> call real service
        String videoData = realService.getVideo(videoId);
        cache.put(videoId, videoData);
        return videoData;
    }
    
}




public class ProxyDesignPattern {
    public static void main(String[] args) {
        VideoService service = new VideoServiceProxy();
        //First call --> slow
        System.out.println(service.getVideo("abc"));

        // Second call --> fast
        for(int i = 1; i <= 10; i++)
            System.out.println(service.getVideo("abc"));

        // New Video --> slow Again
        System.out.println(service.getVideo("xyz"));

    }
}

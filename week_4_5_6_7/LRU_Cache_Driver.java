package week_4_5_6_7;

import java.util.HashMap;

// DLL 


class Node{
    int key;
    int value;
    Node prev;
    Node next;  
    
    public Node(int key, int value){
        this.key = key;
        this.value = value;
    }
}


class DLL{
    Node head;
    Node tail;
    int size;

    public DLL(){
        head = new Node(-1,-1);
        tail = new Node(-1,-1); 
        head.next = tail;
        tail.prev = head;
        this.size = 0;
    }

    int getSize(){
        return size;
    }

    void increaseSize(){
        size++;
    }

    // LRU --> TAIL 
    // MRU ---> HEAD 

    // MoveToHead
    void removeNode(Node node){
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.next = null;
        node.prev = null;
    }

    void addToHead(Node node){
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
        node.prev = head;
    }

    void moveToHead(Node node){
        removeNode(node);
        addToHead(node);
    }

    void removeLRU(){
        removeNode(tail.prev);
    }

    void printCache(){
        if(head.next == tail){
            System.out.println("Cache is Empty");
            return;
        }
        Node current =  head.next;
        while(current != tail){
            System.out.print("[" + current.key + " " + current.value + "]");
            current = current.next;
        }
        System.out.println();
    }

    //move to HEAD 
    // MRU ---> HEAD 
    // LRU ---> TAIL 

}



class LRU_Cache{
    int capacity;
    DLL cache;
    HashMap<Integer, Node> mapping;
    public LRU_Cache(int capacity){
        this.capacity = capacity;
        cache = new DLL();
        mapping = new HashMap<>();
    }
    int get(int key){
        if(!mapping.containsKey(key)){
            System.out.println("Not present in cache");
            return -1; //
        }else{
            Node current = mapping.get(key);
            int val = current.value;
            cache.moveToHead(current);
            cache.printCache();
            return val;
        }
        
    }

    void put(int key, int value){
        if(mapping.containsKey(key)){
            Node current = mapping.get(key);
            current.value = value;
            cache.moveToHead(current);
        }else{
            Node newNode = new Node(key,value);
            if(capacity == cache.getSize()){
                cache.removeLRU();
                mapping.remove(key);
            }
            cache.addToHead(newNode);
            mapping.put(key, newNode);
            cache.increaseSize();   
        }
        cache.printCache();
    }
}



// get(key)
// put(key, value)


public class LRU_Cache_Driver {
    public static void main(String[] args) {
        LRU_Cache cache = new LRU_Cache(3);
        cache.put(1, 2);
        cache.put(2, 3);
        cache.put(3, 4);
        cache.put(1, 100);

        cache.get(3);

        cache.put(5,123);

        
    }
}

package week_4_5_6_7;

import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

/*
BUY/SELL orders 
Order Book 
Price-Time Priority  // PQ : 
Partial fills, Full Fills 
Trade Execution 
Matching Engine 

TradingSystemDesign
 - User 
 - Order 
 - OrderType 
 - OrderStatus 
 - MatchingEngine 
 - OrderBook
 - TradingSystem




*/


enum OrderType{
    BUY,
    SELL
}

enum OrderStatus{
    OPEN,
    PARIALLY_FILLED,
    FILLED,
    CANCELLED
}

class Order{
    String orderId;
    String userId;
    String stockId;
    OrderType type;
    double price;
    int quantity;
    LocalDateTime time;
    // List<Trade> trades; //
}

class Trade{
    String tradeId;
    String buyerId;
    String sellerId;
    double executionPrice;
    int quantity;
    LocalDateTime time;
}

class OrderBook{
    //MaxHeap for Buyers 
    PriorityQueue<Order> buyOrder = new PrioirtyQueue<>((a,b)-> {
        if(Double.compare(b.price, a.price) != 0){
            return Double.compare(b.price, a.price);    // 
        }else{
            return Long.compare(a.time, b.time);
        }
    }); 

    // MinHeap for Sellers
    PriorityQueue<Order> sellOrder = new PrioirtyQueue<>((a,b)-> {
        if(Double.compare(a.price, b.price) != 0){
            return Double.compare(b.price, a.price);    // 
        }else{
            return Long.compare(a.time, b.time);
        }
    }); 

    void addOrder(Order order){
        if(order.type == BUY){
            buyOrder.add(order);
        }
    }
}

class MatchingEngine{
    OrderBoook orderBook;

    void matchOrders() {
        List<Trade> trades = new ArrayList<>();
        while(!orderBook.buyOrder().isEmpty() && !orderBook.sellOrder().isEmpty()){
                
            Order buyOrder  = orderBook.getBuyOrders().peek();  // 50 , 100

            Order sellOrder = orderBook.SellOrders().peek(); // 100, 50

            if(buyOrder.getPrice() < sellOrder.getPrice()){
                break;
            }

            int executeQty = Math.min(buyOrder.getRemainingQuanity(), sellOrder.egtRemainingQunaity());

            Trade trade = new Trade(executeQty);

            trades.add(trade);

            //
            if(buyOrder.getRemainingQuantity() == 0){
                buyOrder.setStatus(Order.FILLED);
                orderBook.buyOrder().poll();
            }
            if(sellOrder.getRemainingQuantity() == 0){
                sellOrder.setStatus(Order.FILLED);
                orderBook.buyOrder().poll();
            }
        }
    }
}


class TradingSystem{




}


public class TradingSystem {
    
    OrderBook 
    MatchingEngine 



    placeOrder(Order order){

        orderBook.addOrder(order);
    }





}
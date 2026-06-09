package week_4_5_6_7;


/*
Functional Requirements
    User can place BUY order.
    User can place SELL order.
    Match orders based on price-time priority.
    Execute trade when matching found.
    Maintain order book.
    Cancel order.



+----------------+
| TradingSystem  |
+----------------+
        |
        v
+----------------+
| MatchingEngine |
+----------------+
        |
        v
+----------------+
| OrderBook      |
+----------------+
        |
        v
+----------------+
| Order          |
+----------------+


TradingSystemDemo
    │
    ├── User
    ├── Order
    ├── Trade
    ├── OrderType
    ├── OrderStatus
    ├── OrderBook
    ├── MatchingEngine
    └── TradingSystem

*/

import java.util.*;

/*
 * ===========================
 * ENUMS
 * ===========================
 */

enum OrderType {
    BUY,
    SELL
}

enum OrderStatus {
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED
}

/*
 * ===========================
 * USER
 * ===========================
 */

class User {
    private final String userId;
    private final String name;

    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}

/*
 * ===========================
 * ORDER
 * ===========================
 */

class Order {

    private final String orderId;
    private final String userId;
    private final OrderType type;
    private final double price;
    private final int quantity;

    private int remainingQuantity;

    private final long timestamp;

    private OrderStatus status;

    public Order(
            String orderId,
            String userId,
            OrderType type,
            double price,
            int quantity) {

        this.orderId = orderId;
        this.userId = userId;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.timestamp = System.nanoTime();
        this.status = OrderStatus.OPEN;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public OrderType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return orderId +
                " [" + type + "]" +
                " price=" + price +
                " remaining=" + remainingQuantity +
                " status=" + status;
    }
}

/*
 * ===========================
 * TRADE
 * ===========================
 */

class Trade {

    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final double executionPrice;
    private final int quantity;

    public Trade(
            String tradeId,
            String buyOrderId,
            String sellOrderId,
            double executionPrice,
            int quantity) {

        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.executionPrice = executionPrice;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "buy=" + buyOrderId +
                ", sell=" + sellOrderId +
                ", price=" + executionPrice +
                ", qty=" + quantity +
                '}';
    }
}

/*
 * ===========================
 * ORDER BOOK
 * ===========================
 */

class OrderBook {

    private final PriorityQueue<Order> buyOrders =
            new PriorityQueue<>((o1, o2) -> {

                if (Double.compare(o1.getPrice(), o2.getPrice()) != 0) {
                    return Double.compare(
                            o2.getPrice(),
                            o1.getPrice());
                }

                return Long.compare(
                        o1.getTimestamp(),
                        o2.getTimestamp());
            });

    private final PriorityQueue<Order> sellOrders =
            new PriorityQueue<>((o1, o2) -> {

                if (Double.compare(o1.getPrice(), o2.getPrice()) != 0) {
                    return Double.compare(
                            o1.getPrice(),
                            o2.getPrice());
                }

                return Long.compare(
                        o1.getTimestamp(),
                        o2.getTimestamp());
            });

    public void addOrder(Order order) {

        if (order.getType() == OrderType.BUY) {
            buyOrders.offer(order);
        } else {
            sellOrders.offer(order);
        }
    }

    public PriorityQueue<Order> getBuyOrders() {
        return buyOrders;
    }

    public PriorityQueue<Order> getSellOrders() {
        return sellOrders;
    }
}

/*
 * ===========================
 * MATCHING ENGINE
 * ===========================
 */

class MatchingEngine {

    private final OrderBook orderBook;

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public List<Trade> matchOrders() {

        List<Trade> trades = new ArrayList<>();

        while (!orderBook.getBuyOrders().isEmpty()
                && !orderBook.getSellOrders().isEmpty()) {

            Order buyOrder =
                    orderBook.getBuyOrders().peek();

            Order sellOrder =
                    orderBook.getSellOrders().peek();

            if (buyOrder.getPrice() < sellOrder.getPrice()) {
                break;
            }

            int executedQty =
                    Math.min(
                            buyOrder.getRemainingQuantity(),
                            sellOrder.getRemainingQuantity());

            Trade trade =
                    new Trade(
                            UUID.randomUUID().toString(),
                            buyOrder.getOrderId(),
                            sellOrder.getOrderId(),
                            sellOrder.getPrice(),
                            executedQty);

            trades.add(trade);

            buyOrder.setRemainingQuantity(
                    buyOrder.getRemainingQuantity() - executedQty);

            sellOrder.setRemainingQuantity(
                    sellOrder.getRemainingQuantity() - executedQty);

            if (buyOrder.getRemainingQuantity() == 0) {
                buyOrder.setStatus(OrderStatus.FILLED);
                orderBook.getBuyOrders().poll();
            } else {
                buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            if (sellOrder.getRemainingQuantity() == 0) {
                sellOrder.setStatus(OrderStatus.FILLED);
                orderBook.getSellOrders().poll();
            } else {
                sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }
        }

        return trades;
    }
}

/*
 * ===========================
 * TRADING SYSTEM
 * ===========================
 */

class TradingSystem {

    private final OrderBook orderBook =
            new OrderBook();

    private final MatchingEngine matchingEngine =
            new MatchingEngine(orderBook);

    public void placeOrder(Order order) {

        System.out.println("\nPlacing -> " + order);

        orderBook.addOrder(order);

        List<Trade> trades =
                matchingEngine.matchOrders();

        for (Trade trade : trades) {
            System.out.println("EXECUTED -> " + trade);
        }
    }

    public void displayOrderBook() {

        System.out.println("\n===== BUY ORDERS =====");

        for (Order order : orderBook.getBuyOrders()) {
            System.out.println(order);
        }

        System.out.println("\n===== SELL ORDERS =====");

        for (Order order : orderBook.getSellOrders()) {
            System.out.println(order);
        }
    }
}


public class TradingSystem_Advance {

    public static void main(String[] args) {

        TradingSystem tradingSystem =
                new TradingSystem();

        tradingSystem.placeOrder(
                new Order(
                        "B1",
                        "U1",
                        OrderType.BUY,
                        120,
                        100));

        tradingSystem.placeOrder(
                new Order(
                        "S1",
                        "U2",
                        OrderType.SELL,
                        115,
                        50));

        tradingSystem.placeOrder(
                new Order(
                        "S2",
                        "U3",
                        OrderType.SELL,
                        118,
                        70));

        tradingSystem.placeOrder(
                new Order(
                        "B2",
                        "U4",
                        OrderType.BUY,
                        119,
                        30));

        tradingSystem.displayOrderBook();
    }
}



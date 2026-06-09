import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

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
                " qty=" + quantity +
                " remaining=" + remainingQuantity +
                " status=" + status;
    }
}

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
                "buyOrderId='" + buyOrderId + '\'' +
                ", sellOrderId='" + sellOrderId + '\'' +
                ", executionPrice=" + executionPrice +
                ", quantity=" + quantity +
                '}';
    }
}

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

class MatchingEngine {

    private final OrderBook orderBook;
    private final ReentrantLock lock = new ReentrantLock();

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public List<Trade> processOrder(Order order) {

        lock.lock();

        try {

            orderBook.addOrder(order);

            return matchOrders();

        } finally {
            lock.unlock();
        }
    }

    private List<Trade> matchOrders() {

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

            int executedQuantity =
                    Math.min(
                            buyOrder.getRemainingQuantity(),
                            sellOrder.getRemainingQuantity());

            Trade trade =
                    new Trade(
                            UUID.randomUUID().toString(),
                            buyOrder.getOrderId(),
                            sellOrder.getOrderId(),
                            sellOrder.getPrice(),
                            executedQuantity);

            trades.add(trade);

            buyOrder.setRemainingQuantity(
                    buyOrder.getRemainingQuantity()
                            - executedQuantity);

            sellOrder.setRemainingQuantity(
                    sellOrder.getRemainingQuantity()
                            - executedQuantity);

            if (buyOrder.getRemainingQuantity() == 0) {

                buyOrder.setStatus(
                        OrderStatus.FILLED);

                orderBook.getBuyOrders().poll();

            } else {

                buyOrder.setStatus(
                        OrderStatus.PARTIALLY_FILLED);
            }

            if (sellOrder.getRemainingQuantity() == 0) {

                sellOrder.setStatus(
                        OrderStatus.FILLED);

                orderBook.getSellOrders().poll();

            } else {

                sellOrder.setStatus(
                        OrderStatus.PARTIALLY_FILLED);
            }
        }

        return trades;
    }

    public void printOrderBook() {

        lock.lock();

        try {

            System.out.println("\n===== BUY ORDERS =====");

            for (Order order : orderBook.getBuyOrders()) {
                System.out.println(order);
            }

            System.out.println("\n===== SELL ORDERS =====");

            for (Order order : orderBook.getSellOrders()) {
                System.out.println(order);
            }

        } finally {
            lock.unlock();
        }
    }
}

class TradingSystem {

    private final OrderBook orderBook = new OrderBook();
    private final MatchingEngine matchingEngine =
            new MatchingEngine(orderBook);

    public void placeOrder(Order order) {

        List<Trade> trades =
                matchingEngine.processOrder(order);

        System.out.println(
                Thread.currentThread().getName()
                        + " -> Placed: "
                        + order);

        for (Trade trade : trades) {
            System.out.println("EXECUTED -> " + trade);
        }
    }

    public void displayOrderBook() {
        matchingEngine.printOrderBook();
    }
}

public class TradingSystemThread_Safe {

    public static void main(String[] args)
            throws InterruptedException {

        TradingSystem tradingSystem =
                new TradingSystem();

        Thread t1 = new Thread(() ->
                tradingSystem.placeOrder(
                        new Order(
                                "B1",
                                "U1",
                                OrderType.BUY,
                                120,
                                100)));

        Thread t2 = new Thread(() ->
                tradingSystem.placeOrder(
                        new Order(
                                "S1",
                                "U2",
                                OrderType.SELL,
                                115,
                                50)));

        Thread t3 = new Thread(() ->
                tradingSystem.placeOrder(
                        new Order(
                                "S2",
                                "U3",
                                OrderType.SELL,
                                118,
                                70)));

        Thread t4 = new Thread(() ->
                tradingSystem.placeOrder(
                        new Order(
                                "B2",
                                "U4",
                                OrderType.BUY,
                                119,
                                30)));

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();

        tradingSystem.displayOrderBook();
    }
}
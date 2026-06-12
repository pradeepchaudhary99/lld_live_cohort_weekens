package week_4_5_6_7;

/*

Customer:
    - Browse Restraunts/food items 
    - View Menu 
    - Add Items to cart 
    - Place Order
    - Track Order


Restaurant:
    - Manage menu 
    - Accept/Reject orders 
    - Prepare food 
    - Status offline/online



Delivery Partner:
    Accept/reject delivery 
    pickup order 
    deliver order 

System:
    Assign Delivery Partner
    Payments
    Showing nearby restraunts 
    RestrauntManager
    DeliveryPartnerManager 
    Track_order Lifecycle 

Read this: 
Customer adds items to the cart and places an order. 
OrderManager creates the order and processes payment. 
After successful payment, the restaurant accepts the order and prepares the food. Once the food is ready, 
OrderManager uses an AssignmentStrategy to assign the best available delivery partner. 
The partner picks up the order, delivers it to the customer, 
and the order status transitions through CREATED → PAYMENT_COMPLETED → RESTAURANT_ACCEPTED → PREPARING → READY_FOR_PICKUP → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED. 
OrderManager acts as the central orchestrator throughout the lifecycle.

Customer
   |
   | Add Items
   v
Cart
   |
   | Place Order
   v
OrderManager
   |
   | Process Payment
   v
Payment
   |
   | Success
   v
Restaurant
   |
   | Accept Order
   v
PREPARING
   |
   v
READY_FOR_PICKUP
   |
   | Assign Partner
   v
AssignmentStrategy
   |
   v
DeliveryPartner
   |
   | Pickup Food
   v
PICKED_UP
   |
   | Deliver Food
   v
DELIVERED

    
*/

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * =====================================================
 * ENUMS
 * =====================================================
 */

enum RestaurantStatus {
    ONLINE,
    OFFLINE
}

enum OrderStatus {
    CREATED,
    PAYMENT_COMPLETED,
    RESTAURANT_ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}

enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}

/*
 * =====================================================
 * LOCATION
 * =====================================================
 */

class Location {
    private final double x;
    private final double y;

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double distance(Location other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

/*
 * =====================================================
 * MENU ITEM
 * =====================================================
 */

class MenuItem {
    private final String id;
    private final String name;
    private final double price;

    public MenuItem(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }
}

/*
 * =====================================================
 * RESTAURANT
 * =====================================================
 */

class Restaurant {

    private final String id;
    private final String name;
    private final Location location;

    private RestaurantStatus status;

    private final List<MenuItem> menu = new CopyOnWriteArrayList<>();

    public Restaurant(
            String id,
            String name,
            Location location) {

        this.id = id;
        this.name = name;
        this.location = location;
        this.status = RestaurantStatus.ONLINE;
    }

    public void addMenuItem(MenuItem item) {
        menu.add(item);
    }

    public List<MenuItem> getMenu() {
        return menu;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public void acceptOrder(Order order) {
        order.updateStatus(
                OrderStatus.RESTAURANT_ACCEPTED
        );
    }
}

/*
 * =====================================================
 * CUSTOMER
 * =====================================================
 */

class Customer {

    private final String id;
    private final String name;

    private final Cart cart = new Cart();

    public Customer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Cart getCart() {
        return cart;
    }

    public String getName() {
        return name;
    }
}

/*
 * =====================================================
 * CART
 * =====================================================
 */

class Cart {

    private final List<MenuItem> items =
            new ArrayList<>();

    public void addItem(MenuItem item) {
        items.add(item);
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public double getTotalAmount() {

        return items.stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
    }
}

/*
 * =====================================================
 * PAYMENT
 * =====================================================
 */

class Payment {

    private final String paymentId;
    private final double amount;

    private PaymentStatus status =
            PaymentStatus.PENDING;

    public Payment(
            String paymentId,
            double amount) {

        this.paymentId = paymentId;
        this.amount = amount;
    }

    public boolean process() {

        status = PaymentStatus.SUCCESS;

        System.out.println(
                "Payment Successful : " + amount
        );

        return true;
    }
}

/*
 * =====================================================
 * DELIVERY PARTNER
 * =====================================================
 */

class DeliveryPartner {

    private final String id;
    private final String name;
    private final Location location;

    private volatile boolean available = true;

    public DeliveryPartner(
            String id,
            String name,
            Location location) {

        this.id = id;
        this.name = name;
        this.location = location;
    }

    public boolean isAvailable() {
        return available;
    }

    public void assign() {
        available = false;
    }

    public void free() {
        available = true;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}

/*
 * =====================================================
 * ORDER
 * =====================================================
 */

class Order {

    private final String orderId;

    private final Customer customer;

    private final Restaurant restaurant;

    private final List<MenuItem> items;

    private volatile OrderStatus status;

    private DeliveryPartner deliveryPartner;

    public Order(
            String orderId,
            Customer customer,
            Restaurant restaurant,
            List<MenuItem> items) {

        this.orderId = orderId;
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = items;

        this.status = OrderStatus.CREATED;
    }

    public synchronized void updateStatus(
            OrderStatus status) {

        this.status = status;

        System.out.println(
                "Order " + orderId +
                        " -> " + status
        );
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void assignPartner(
            DeliveryPartner partner) {

        this.deliveryPartner = partner;
    }

    public DeliveryPartner getDeliveryPartner() {
        return deliveryPartner;
    }

    public String getOrderId() {
        return orderId;
    }
}

/*
 * =====================================================
 * ASSIGNMENT STRATEGY
 * =====================================================
 */

interface AssignmentStrategy {

    DeliveryPartner assignPartner(
            Order order,
            List<DeliveryPartner> partners);
}

/*
 * =====================================================
 * NEAREST PARTNER STRATEGY
 * =====================================================
 */

class NearestPartnerStrategy
        implements AssignmentStrategy {

    @Override
    public DeliveryPartner assignPartner(
            Order order,
            List<DeliveryPartner> partners) {

        Restaurant restaurant =
                order.getRestaurant();

        DeliveryPartner best = null;

        double minDistance =
                Double.MAX_VALUE;

        for (DeliveryPartner partner : partners) {

            if (!partner.isAvailable()) {
                continue;
            }

            double distance =
                    partner.getLocation()
                            .distance(
                                    restaurant.getLocation()
                            );

            if (distance < minDistance) {

                minDistance = distance;
                best = partner;
            }
        }

        return best;
    }
}

/*
 * =====================================================
 * DELIVERY PARTNER MANAGER
 * =====================================================
 */

class DeliveryPartnerManager {

    private final List<DeliveryPartner>
            partners =
            new CopyOnWriteArrayList<>();

    public void addPartner(
            DeliveryPartner partner) {

        partners.add(partner);
    }

    public List<DeliveryPartner>
    getAvailablePartners() {

        List<DeliveryPartner> result =
                new ArrayList<>();

        for (DeliveryPartner p : partners) {

            if (p.isAvailable()) {
                result.add(p);
            }
        }

        return result;
    }
}

/*
 * =====================================================
 * ORDER MANAGER
 * =====================================================
 */

class OrderManager {

    private final AssignmentStrategy strategy;

    private final DeliveryPartnerManager
            partnerManager;

    private final AtomicInteger orderCounter =
            new AtomicInteger(1);

    public OrderManager(
            AssignmentStrategy strategy,
            DeliveryPartnerManager partnerManager) {

        this.strategy = strategy;
        this.partnerManager = partnerManager;
    }

    public Order placeOrder(
            Customer customer,
            Restaurant restaurant) {

        String orderId =
                "ORD-" +
                        orderCounter.getAndIncrement();

        Order order =
                new Order(
                        orderId,
                        customer,
                        restaurant,
                        customer.getCart().getItems()
                );

        Payment payment =
                new Payment(
                        UUID.randomUUID().toString(),
                        customer.getCart()
                                .getTotalAmount()
                );

        if (!payment.process()) {

            order.updateStatus(
                    OrderStatus.CANCELLED
            );

            return order;
        }

        order.updateStatus(
                OrderStatus.PAYMENT_COMPLETED
        );

        restaurant.acceptOrder(order);

        order.updateStatus(
                OrderStatus.PREPARING
        );

        order.updateStatus(
                OrderStatus.READY_FOR_PICKUP
        );

        DeliveryPartner partner =
                strategy.assignPartner(
                        order,
                        partnerManager
                                .getAvailablePartners()
                );

        if (partner == null) {

            System.out.println(
                    "No delivery partner available"
            );

            return order;
        }

        partner.assign();

        order.assignPartner(partner);

        System.out.println(
                "Assigned Partner : "
                        + partner.getName()
        );

        order.updateStatus(
                OrderStatus.PICKED_UP
        );

        order.updateStatus(
                OrderStatus.DELIVERED
        );

        partner.free();

        return order;
    }
}

/*
 * =====================================================
 * MAIN
 * =====================================================
 */

public class ZomatoFoodDelivery {

    public static void main(String[] args) {

        Restaurant restaurant =
                new Restaurant(
                        "R1",
                        "Dominos",
                        new Location(10, 10)
                );

        MenuItem pizza =
                new MenuItem(
                        "M1",
                        "Farmhouse Pizza",
                        350
                );

        MenuItem coke =
                new MenuItem(
                        "M2",
                        "Coke",
                        50
                );

        restaurant.addMenuItem(pizza);
        restaurant.addMenuItem(coke);

        Customer customer =
                new Customer(
                        "C1",
                        "Pradeep"
                );

        customer.getCart().addItem(pizza);
        customer.getCart().addItem(coke);

        DeliveryPartnerManager partnerManager =
                new DeliveryPartnerManager();

        partnerManager.addPartner(
                new DeliveryPartner(
                        "D1",
                        "Rahul",
                        new Location(11, 10)
                )
        );

        partnerManager.addPartner(
                new DeliveryPartner(
                        "D2",
                        "Amit",
                        new Location(30, 30)
                )
        );

        OrderManager orderManager =
                new OrderManager(
                        new NearestPartnerStrategy(),
                        partnerManager
                );

        Order order =
                orderManager.placeOrder(
                        customer,
                        restaurant
                );

        System.out.println(
                "\nOrder Placed Successfully : "
                        + order.getOrderId()
        );
    }
}
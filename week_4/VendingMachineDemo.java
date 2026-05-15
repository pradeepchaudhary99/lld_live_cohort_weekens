```java
import java.util.*;

/*
    Vending Machine LLD
    Single File Example
*/

public class VendingMachineDemo {

    public static void main(String[] args) {

        VendingMachine machine = new VendingMachine();

        machine.addItem("A1", new Item("Coke", 40), 5);
        machine.addItem("B1", new Item("Pepsi", 30), 3);
        machine.addItem("C1", new Item("Water", 20), 2);

        machine.insertCoin(Coin.TWENTY);
        machine.insertCoin(Coin.TWENTY);

        machine.selectProduct("A1");

        machine.dispenseProduct();
    }
}

/* =========================================================
                        ITEM
   ========================================================= */

class Item {

    private String name;
    private int price;

    public Item(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }
}

/* =========================================================
                        SLOT
   ========================================================= */

class Slot {

    private Item item;
    private int quantity;

    public Slot(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public boolean isAvailable() {
        return quantity > 0;
    }

    public void dispenseItem() {
        if (quantity <= 0) {
            throw new RuntimeException("Item out of stock");
        }

        quantity--;
    }

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }
}

/* =========================================================
                        INVENTORY
   ========================================================= */

class Inventory {

    private Map<String, Slot> slots = new HashMap<>();

    public void addItem(String code, Item item, int quantity) {
        slots.put(code, new Slot(item, quantity));
    }

    public Slot getSlot(String code) {
        return slots.get(code);
    }
}

/* =========================================================
                        COIN
   ========================================================= */

enum Coin {

    ONE(1),
    TWO(2),
    FIVE(5),
    TEN(10),
    TWENTY(20);

    private final int value;

    Coin(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

/* =========================================================
                        STATE
   ========================================================= */

interface State {

    void insertCoin(Coin coin);

    void selectProduct(String code);

    void dispenseProduct();

    void cancel();
}

/* =========================================================
                    VENDING MACHINE
   ========================================================= */

class VendingMachine {

    private State idleState;
    private State hasMoneyState;
    private State dispenseState;

    private State currentState;

    private Inventory inventory;

    private List<Coin> insertedCoins;

    private int totalPayment;

    private Slot selectedSlot;

    public VendingMachine() {

        inventory = new Inventory();

        insertedCoins = new ArrayList<>();

        idleState = new IdleState(this);
        hasMoneyState = new HasMoneyState(this);
        dispenseState = new DispenseState(this);

        currentState = idleState;
    }

    public void insertCoin(Coin coin) {
        currentState.insertCoin(coin);
    }

    public void selectProduct(String code) {
        currentState.selectProduct(code);
    }

    public void dispenseProduct() {
        currentState.dispenseProduct();
    }

    public void cancel() {
        currentState.cancel();
    }

    public void addItem(String code, Item item, int quantity) {
        inventory.addItem(code, item, quantity);
    }

    public void addCoin(Coin coin) {
        insertedCoins.add(coin);
        totalPayment += coin.getValue();

        System.out.println("Inserted Coin: " + coin +
                ", Total = " + totalPayment);
    }

    public void refund() {

        System.out.println("Refunding Amount: " + totalPayment);

        insertedCoins.clear();
        totalPayment = 0;
    }

    public void returnChange(int change) {

        if (change > 0) {
            System.out.println("Returning Change: " + change);
        }
    }

    public void resetTransaction() {

        insertedCoins.clear();
        totalPayment = 0;
        selectedSlot = null;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getTotalPayment() {
        return totalPayment;
    }

    public void setSelectedSlot(Slot selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    public Slot getSelectedSlot() {
        return selectedSlot;
    }

    public void setState(State state) {
        this.currentState = state;
    }

    public State getIdleState() {
        return idleState;
    }

    public State getHasMoneyState() {
        return hasMoneyState;
    }

    public State getDispenseState() {
        return dispenseState;
    }
}

/* =========================================================
                        IDLE STATE
   ========================================================= */

class IdleState implements State {

    private VendingMachine machine;

    public IdleState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override
    public void insertCoin(Coin coin) {

        machine.addCoin(coin);

        machine.setState(machine.getHasMoneyState());
    }

    @Override
    public void selectProduct(String code) {

        System.out.println("Insert money first");
    }

    @Override
    public void dispenseProduct() {

        System.out.println("No product selected");
    }

    @Override
    public void cancel() {

        System.out.println("No transaction to cancel");
    }
}

/* =========================================================
                    HAS MONEY STATE
   ========================================================= */

class HasMoneyState implements State {

    private VendingMachine machine;

    public HasMoneyState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override
    public void insertCoin(Coin coin) {

        machine.addCoin(coin);
    }

    @Override
    public void selectProduct(String code) {

        Slot slot = machine.getInventory().getSlot(code);

        if (slot == null) {
            System.out.println("Invalid product code");
            return;
        }

        if (!slot.isAvailable()) {
            System.out.println("Product out of stock");
            return;
        }

        int price = slot.getItem().getPrice();

        if (machine.getTotalPayment() < price) {

            System.out.println("Insufficient balance");
            return;
        }

        machine.setSelectedSlot(slot);

        machine.setState(machine.getDispenseState());

        System.out.println("Product Selected: "
                + slot.getItem().getName());
    }

    @Override
    public void dispenseProduct() {

        System.out.println("Select product first");
    }

    @Override
    public void cancel() {

        machine.refund();

        machine.setState(machine.getIdleState());
    }
}

/* =========================================================
                    DISPENSE STATE
   ========================================================= */

class DispenseState implements State {

    private VendingMachine machine;

    public DispenseState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override
    public void insertCoin(Coin coin) {

        System.out.println("Please wait, dispensing product");
    }

    @Override
    public void selectProduct(String code) {

        System.out.println("Already processing");
    }

    @Override
    public void dispenseProduct() {

        Slot slot = machine.getSelectedSlot();

        slot.dispenseItem();

        Item item = slot.getItem();

        System.out.println("Dispensing Product: "
                + item.getName());

        int change =
                machine.getTotalPayment() - item.getPrice();

        machine.returnChange(change);

        machine.resetTransaction();

        machine.setState(machine.getIdleState());
    }

    @Override
    public void cancel() {

        System.out.println("Cannot cancel now");
    }
}

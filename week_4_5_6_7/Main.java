package week_4_5_6_7;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// ══════════════════════════════════════════════════════════════════════════════
// ENUMS
// ══════════════════════════════════════════════════════════════════════════════

enum SplitType   { EQUAL, EXACT, PERCENTAGE }
enum ExpenseType { FOOD, TRAVEL, UTILITIES, ENTERTAINMENT, OTHER }

// ══════════════════════════════════════════════════════════════════════════════
// USER
// ══════════════════════════════════════════════════════════════════════════════

class User {
    final String userId;
    final String name;
    final String email;

    User(String name, String email) {
        this.userId = UUID.randomUUID().toString().substring(0, 8);
        this.name   = name;
        this.email  = email;
    }

    @Override public String toString() { return name; }
}

// ══════════════════════════════════════════════════════════════════════════════
// SPLIT — how much each participant owes
// ══════════════════════════════════════════════════════════════════════════════

abstract class Split {
    final User user;
    double amount; // resolved amount after calculation

    Split(User user) { this.user = user; }
}

class EqualSplit extends Split {
    EqualSplit(User user) { super(user); }
}

class ExactSplit extends Split {
    ExactSplit(User user, double amount) {
        super(user);
        this.amount = amount;
    }
}

class PercentageSplit extends Split {
    final double percent;
    PercentageSplit(User user, double percent) {
        super(user);
        this.percent = percent;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// EXPENSE
// ══════════════════════════════════════════════════════════════════════════════

class Expense {
    final String expenseId;
    final String description;
    final double totalAmount;
    final User paidBy;
    final List<Split> splits;
    final ExpenseType type;
    final long timestamp;

    Expense(String description, double totalAmount, User paidBy,
            List<Split> splits, ExpenseType type) {
        this.expenseId   = UUID.randomUUID().toString().substring(0, 8);
        this.description = description;
        this.totalAmount = totalAmount;
        this.paidBy      = paidBy;
        this.splits      = splits;
        this.type        = type;
        this.timestamp   = System.currentTimeMillis();
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SPLIT CALCULATOR — Strategy pattern
// ══════════════════════════════════════════════════════════════════════════════

interface SplitCalculator {
    void calculate(double totalAmount, List<Split> splits);
}

class EqualSplitCalculator implements SplitCalculator {
    public void calculate(double totalAmount, List<Split> splits) {
        double share = totalAmount / splits.size();
        splits.forEach(s -> s.amount = share);
    }
}

class ExactSplitCalculator implements SplitCalculator {
    public void calculate(double totalAmount, List<Split> splits) {
        double sum = splits.stream().mapToDouble(s -> s.amount).sum();
        if (Math.abs(sum - totalAmount) > 0.01)
            throw new IllegalArgumentException("Exact splits must sum to total: " + sum + " != " + totalAmount);
    }
}

class PercentageSplitCalculator implements SplitCalculator {
    public void calculate(double totalAmount, List<Split> splits) {
        double totalPercent = splits.stream()
            .mapToDouble(s -> ((PercentageSplit) s).percent).sum();
        if (Math.abs(totalPercent - 100.0) > 0.01)
            throw new IllegalArgumentException("Percentages must sum to 100: " + totalPercent);
        splits.forEach(s -> s.amount = totalAmount * ((PercentageSplit) s).percent / 100.0);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BALANCE SHEET — tracks who owes whom
// ══════════════════════════════════════════════════════════════════════════════

class BalanceSheet {
    // userId → (otherUserId → netAmount)
    // positive = other owes userId, negative = userId owes other
    private final Map<String, Map<String, Double>> balances = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    void updateBalance(User paidBy, User owedBy, double amount) {
        if (paidBy.userId.equals(owedBy.userId)) return;

        lock.lock();
        try {
            // paidBy is owed money (+amount)
            balances
                .computeIfAbsent(paidBy.userId, k -> new ConcurrentHashMap<>())
                .merge(owedBy.userId, amount, Double::sum);

            // owedBy owes money (-amount)
            balances
                .computeIfAbsent(owedBy.userId, k -> new ConcurrentHashMap<>())
                .merge(paidBy.userId, -amount, Double::sum);
        } finally {
            lock.unlock();
        }
    }

    // Get net balance of a user with everyone
    Map<String, Double> getBalances(String userId) {
        return balances.getOrDefault(userId, Collections.emptyMap());
    }

    // Get balance between two users
    double getBalance(String userAId, String userBId) {
        return balances
            .getOrDefault(userAId, Collections.emptyMap())
            .getOrDefault(userBId, 0.0);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// GROUP
// ══════════════════════════════════════════════════════════════════════════════

class Group {
    final String groupId;
    final String name;
    private final List<User> members  = new CopyOnWriteArrayList<>();
    private final List<Expense> expenses = new CopyOnWriteArrayList<>();

    Group(String name) {
        this.groupId = UUID.randomUUID().toString().substring(0, 8);
        this.name    = name;
    }

    void addMember(User user) { members.add(user); }

    void addExpense(Expense expense) { expenses.add(expense); }

    List<User>    getMembers()  { return Collections.unmodifiableList(members); }
    List<Expense> getExpenses() { return Collections.unmodifiableList(expenses); }
}

// ══════════════════════════════════════════════════════════════════════════════
// SETTLEMENT — simplify debts using net balance algorithm
// ══════════════════════════════════════════════════════════════════════════════

class Settlement {
    final User payer;
    final User payee;
    final double amount;

    Settlement(User payer, User payee, double amount) {
        this.payer  = payer;
        this.payee  = payee;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return String.format("%s pays %.2f to %s", payer.name, amount, payee.name);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SPLITWISE SERVICE — Core coordinator
// ══════════════════════════════════════════════════════════════════════════════

class SplitwiseService {
    private final Map<String, User>    users    = new ConcurrentHashMap<>();
    private final Map<String, Group>   groups   = new ConcurrentHashMap<>();
    private final BalanceSheet         balances = new BalanceSheet();

    // Calculator registry — Strategy pattern
    private final Map<SplitType, SplitCalculator> calculators = Map.of(
        SplitType.EQUAL,      new EqualSplitCalculator(),
        SplitType.EXACT,      new ExactSplitCalculator(),
        SplitType.PERCENTAGE, new PercentageSplitCalculator()
    );

    // ── User management ───────────────────────────────────────────────────────
    User createUser(String name, String email) {
        User user = new User(name, email);
        users.put(user.userId, user);
        return user;
    }

    // ── Group management ──────────────────────────────────────────────────────
    Group createGroup(String name, User... members) {
        Group group = new Group(name);
        for (User m : members) group.addMember(m);
        groups.put(group.groupId, group);
        return group;
    }

    // ── Add expense ───────────────────────────────────────────────────────────
    Expense addExpense(Group group, String description, double amount,
                       User paidBy, List<Split> splits,
                       SplitType splitType, ExpenseType expenseType) {

        // Calculate split amounts
        calculators.get(splitType).calculate(amount, splits);

        // Create expense
        Expense expense = new Expense(description, amount, paidBy, splits, expenseType);
        group.addExpense(expense);

        // Update balances
        for (Split split : splits) {
            if (!split.user.userId.equals(paidBy.userId)) {
                balances.updateBalance(paidBy, split.user, split.amount);
            }
        }

        System.out.printf("\n[EXPENSE ADDED] %s | $%.2f | Paid by %s | %s split\n",
            description, amount, paidBy.name, splitType);
        splits.forEach(s -> System.out.printf("  %s owes $%.2f\n", s.user.name, s.amount));

        return expense;
    }

    // ── Non-group expense (between individuals) ───────────────────────────────
    void addDirectExpense(String description, double amount, User paidBy, User owedBy) {
        balances.updateBalance(paidBy, owedBy, amount);
        System.out.printf("\n[DIRECT EXPENSE] %s | $%.2f | %s paid | %s owes\n",
            description, amount, paidBy.name, owedBy.name);
    }

    // ── Settle up ─────────────────────────────────────────────────────────────
    void settle(User payer, User payee, double amount) {
        balances.updateBalance(payee, payer, amount); // reverse the debt
        System.out.printf("\n[SETTLEMENT] %s paid $%.2f to %s\n",
            payer.name, amount, payee.name);
    }

    // ── Simplify debts — net settlement algorithm ─────────────────────────────
    // Reduces number of transactions needed to settle all debts
    List<Settlement> simplifyDebts(Group group) {
        // Calculate net balance per user
        Map<String, Double> netBalance = new HashMap<>();
        Map<String, User> userMap = new HashMap<>();

        for (User member : group.getMembers()) {
            userMap.put(member.userId, member);
            double net = balances.getBalances(member.userId).values()
                .stream().mapToDouble(Double::doubleValue).sum();
            netBalance.put(member.userId, net);
        }

        // Separate into creditors (+) and debtors (-)
        PriorityQueue<Map.Entry<String, Double>> creditors = new PriorityQueue<>(
            (a, b) -> Double.compare(b.getValue(), a.getValue())); // max heap

        PriorityQueue<Map.Entry<String, Double>> debtors = new PriorityQueue<>(
            (a, b) -> Double.compare(a.getValue(), b.getValue())); // min heap

        for (Map.Entry<String, Double> e : netBalance.entrySet()) {
            if (e.getValue() > 0.01)       creditors.offer(e);
            else if (e.getValue() < -0.01) debtors.offer(e);
        }

        // Greedily match biggest debtor with biggest creditor
        List<Settlement> settlements = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Map.Entry<String, Double> creditor = creditors.poll();
            Map.Entry<String, Double> debtor   = debtors.poll();

            double settleAmount = Math.min(creditor.getValue(), -debtor.getValue());
            settlements.add(new Settlement(
                userMap.get(debtor.getKey()),
                userMap.get(creditor.getKey()),
                settleAmount
            ));

            double creditorRemainder = creditor.getValue() - settleAmount;
            double debtorRemainder   = debtor.getValue() + settleAmount;

            if (creditorRemainder > 0.01)
                creditors.offer(Map.entry(creditor.getKey(), creditorRemainder));
            if (debtorRemainder < -0.01)
                debtors.offer(Map.entry(debtor.getKey(), debtorRemainder));
        }

        return settlements;
    }

    // ── Display balance ───────────────────────────────────────────────────────
    void displayBalances(User user) {
        System.out.println("\n═══ Balances for " + user.name + " ═══");
        Map<String, Double> userBalances = balances.getBalances(user.userId);

        if (userBalances.isEmpty()) {
            System.out.println("  All settled up!");
            return;
        }

        userBalances.forEach((otherId, amount) -> {
            User other = users.get(otherId);
            if (other == null || Math.abs(amount) < 0.01) return;
            if (amount > 0) {
                System.out.printf("  %s owes you $%.2f\n", other.name, amount);
            } else {
                System.out.printf("  You owe %s $%.2f\n", other.name, -amount);
            }
        });
    }

    void displayGroupSummary(Group group) {
        System.out.println("\n═══ Group: " + group.name + " ═══");
        System.out.println("Members: " + group.getMembers());
        System.out.println("Expenses: " + group.getExpenses().size());
        System.out.printf("Total: $%.2f\n",
            group.getExpenses().stream().mapToDouble(e -> e.totalAmount).sum());
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN — Demonstration
// ══════════════════════════════════════════════════════════════════════════════

public class Main {
    public static void main(String[] args) {
        SplitwiseService splitwise = new SplitwiseService();

        // Create users
        User alice   = splitwise.createUser("Alice",   "alice@gmail.com");
        User bob     = splitwise.createUser("Bob",     "bob@gmail.com");
        User charlie = splitwise.createUser("Charlie", "charlie@gmail.com");
        User david   = splitwise.createUser("David",   "david@gmail.com");

        // Create group
        Group trip = splitwise.createGroup("Goa Trip", alice, bob, charlie, david);

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 1: Equal split
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 1: Equal Split ═══");

        splitwise.addExpense(trip, "Hotel", 4000,
            alice,
            List.of(new EqualSplit(alice), new EqualSplit(bob),
                    new EqualSplit(charlie), new EqualSplit(david)),
            SplitType.EQUAL,
            ExpenseType.TRAVEL);

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 2: Exact split
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 2: Exact Split ═══");

        splitwise.addExpense(trip, "Dinner", 1000,
            bob,
            List.of(new ExactSplit(alice, 250), new ExactSplit(bob, 250),
                    new ExactSplit(charlie, 300), new ExactSplit(david, 200)),
            SplitType.EXACT,
            ExpenseType.FOOD);

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 3: Percentage split
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 3: Percentage Split ═══");

        splitwise.addExpense(trip, "Cab", 800,
            charlie,
            List.of(new PercentageSplit(alice, 40), new PercentageSplit(bob, 30),
                    new PercentageSplit(charlie, 20), new PercentageSplit(david, 10)),
            SplitType.PERCENTAGE,
            ExpenseType.TRAVEL);

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 4: Direct expense between two users
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 4: Direct Expense ═══");

        splitwise.addDirectExpense("Coffee", 200, david, alice);

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 5: Display balances
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 5: Balances ═══");

        splitwise.displayBalances(alice);
        splitwise.displayBalances(bob);
        splitwise.displayBalances(charlie);
        splitwise.displayBalances(david);

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 6: Simplify debts
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 6: Simplified Settlements ═══");

        List<Settlement> settlements = splitwise.simplifyDebts(trip);
        settlements.forEach(s -> System.out.println("  " + s));

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 7: Partial settlement
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 7: Bob settles with Alice ═══");

        double bobOwesAlice = -splitwise.balances.getBalance(bob.userId, alice.userId);
        if (bobOwesAlice > 0) {
            splitwise.settle(bob, alice, bobOwesAlice);
        }

        splitwise.displayBalances(bob);
        splitwise.displayGroupSummary(trip);
    }
}
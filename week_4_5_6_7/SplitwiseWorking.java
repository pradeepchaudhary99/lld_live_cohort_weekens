package week_4_5_6_7;

import java.util.*;

public class SplitwiseWorking {

    // User
    static class User {
        private final String id;
        private final String name;

        public User(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    // Expense

    static class Expense {
        private final User paidBy;
        private final double amount;
        private final List<User> participants;

        public Expense(User paidBy,
                       double amount,
                       List<User> participants) {
            this.paidBy = paidBy;
            this.amount = amount;
            this.participants = participants;
        }

        public User getPaidBy() {
            return paidBy;
        }

        public double getAmount() {
            return amount;
        }

        public List<User> getParticipants() {
            return participants;
        }
    }


    // Group

    static class Group {
        private final String id;
        private final String name;
        private final List<User> members = new ArrayList<>();
        private final List<Expense> expenses = new ArrayList<>();

        public Group(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public void addMember(User user) {
            members.add(user);
        }

        public List<User> getMembers() {
            return members;
        }

        public void addExpense(Expense expense) {
            expenses.add(expense);
        }

        public List<Expense> getExpenses() {
            return expenses;
        }
    }


    // Splitwise Service
  
    static class SplitwiseService {

        /*
         * balance[A][B] = amount B owes A
         */
        private final Map<String,
                Map<String, Double>> balance = new HashMap<>();

        public void addExpense(Group group,
                               User paidBy,
                               double amount,
                               List<User> participants) {

            Expense expense =
                    new Expense(paidBy, amount, participants);

            group.addExpense(expense);

            double share = amount / participants.size();

            for (User user : participants) {

                if (user.getId().equals(paidBy.getId()))
                    continue;

                addBalance(
                        paidBy.getId(),
                        user.getId(),
                        share
                );
            }
        }

        private void addBalance(String creditor,
                                String debtor,
                                double amount) {

            balance
                    .computeIfAbsent(creditor,
                            k -> new HashMap<>())
                    .merge(debtor,
                            amount,
                            Double::sum);
        }

        public void showBalances() {

            System.out.println("\nCurrent Balances:");

            for (String creditor : balance.keySet()) {

                for (Map.Entry<String, Double> entry :
                        balance.get(creditor).entrySet()) {

                    if (entry.getValue() > 0.0) {
                        System.out.println(
                                entry.getKey()
                                        + " owes "
                                        + creditor
                                        + " : "
                                        + String.format("%.2f",
                                        entry.getValue())
                        );
                    }
                }
            }
        }

        // Simplify debts
        public void simplifyDebts(List<User> users) {

            Map<String, Double> net = new HashMap<>();

            for (User user : users) {
                net.put(user.getId(), 0.0);
            }

            for (String creditor : balance.keySet()) {

                for (Map.Entry<String, Double> entry :
                        balance.get(creditor).entrySet()) {

                    String debtor = entry.getKey();
                    double amt = entry.getValue();

                    net.put(creditor,
                            net.get(creditor) + amt);

                    net.put(debtor,
                            net.get(debtor) - amt);
                }
            }

            PriorityQueue<PersonBalance> creditors =
                    new PriorityQueue<>(
                            (a, b) ->
                                    Double.compare(
                                            b.amount,
                                            a.amount)
                    );

            PriorityQueue<PersonBalance> debtors =
                    new PriorityQueue<>(
                            Comparator.comparingDouble(
                                    a -> a.amount)
                    );

            for (Map.Entry<String, Double> e :
                    net.entrySet()) {

                if (e.getValue() > 0)
                    creditors.offer(
                            new PersonBalance(
                                    e.getKey(),
                                    e.getValue())
                    );

                else if (e.getValue() < 0)
                    debtors.offer(
                            new PersonBalance(
                                    e.getKey(),
                                    e.getValue())
                    );
            }

            System.out.println("\nSimplified Debts:");

            while (!creditors.isEmpty()
                    && !debtors.isEmpty()) {

                PersonBalance creditor =
                        creditors.poll();

                PersonBalance debtor =
                        debtors.poll();

                double settle =
                        Math.min(
                                creditor.amount,
                                -debtor.amount);

                System.out.println(
                        debtor.userId
                                + " pays "
                                + creditor.userId
                                + " : "
                                + String.format("%.2f",
                                settle)
                );

                creditor.amount -= settle;
                debtor.amount += settle;

                if (creditor.amount > 0)
                    creditors.offer(creditor);

                if (debtor.amount < 0)
                    debtors.offer(debtor);
            }
        }
    }

    static class PersonBalance {
        String userId;
        double amount;

        PersonBalance(String userId,
                      double amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }

    // Main

    public static void main(String[] args) {

        User u1 = new User("U1", "Pradeep");
        User u2 = new User("U2", "Aman");
        User u3 = new User("U3", "Rahul");
        User u4 = new User("U4", "Neha");

        Group trip =
                new Group("G1", "Goa Trip");

        trip.addMember(u1);
        trip.addMember(u2);
        trip.addMember(u3);
        trip.addMember(u4);

        SplitwiseService service =
                new SplitwiseService();

        // Pradeep paid 4000 for all
        service.addExpense(
                trip,
                u1,
                4000,
                Arrays.asList(u1, u2, u3, u4)
        );

        // Aman paid 2000 for all
        service.addExpense(
                trip,
                u2,
                2000,
                Arrays.asList(u1, u2, u3, u4)
        );

        service.showBalances();

        service.simplifyDebts(trip.getMembers());
    }
}
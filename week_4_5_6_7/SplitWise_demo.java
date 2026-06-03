package week_4_5_6_7;

import java.security.Timestamp;
import java.util.Map;

/*
    User can create groups  
    User can add expenses
    Expense can be split in different ways
    System keeps track of the balance sheet of every user 
    User can settle debts


    NFR:
     -- Thread-safe 
     -- extensibility
        --> multiple types of spliting
        --> multiple types of payments

    */

class user{
    private String userId;
    private String name;
    private String email;
}

class Group{
    String groupId;
    String name;
    List<User> members;
}

abstract class Split{
    User user;
}

class EqualSplit extends Split{
    private double amount;
}

class ExactSplit extends Split{
    private double amount;
}

class PercentageWise extends Split{
    private double percentage;
}

class Expense{
    String expenseId;
    User paidBy;
    double amount;
    List<Split> splits;
}

class Split{
    User user;
    double amount;
    String expenseId;
}

class BalanceManager{

    Map<User, Map<User, Double>> balances = new HashMap<>();

    void updateBalances(Expense expenses){
        User paidBy = expenses.paidBy;

        for(Split split : expenses.splits){
            balances.get(paidBy).put(split.user, existing + split.amount);
            balances.get(split.user).put(paidBy, existing - split.amount);
        }

    }



}

class ExpenseManager{
    BalanceManager 
}



public class SplitWise_demo {
    
}

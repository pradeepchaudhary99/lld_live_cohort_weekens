package week_1;



//Core Entities:
/*
Transaction
    id
    customerId
    amount
    timestamp

TransactionFilter
    boolean matches(Transaction txn);

CustomerFilter implements TransactionFilter

Client - Controller -- Service -- Filter -- repos -- Result


*/

class Transaction {
    String id;
    String customerId;
    double amount;
    Date timestamp;

    Transaction(String id, String customerId, double amount, Date timestamp) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}

class TransactionRequest {
    String customerId;
    Date start;
    Date end;

    TransactionRequest(String customerId, Date start, Date end) {
        this.customerId = customerId;
        this.start = start;
        this.end = end;
    }
}

// -------------------- FILTER INTERFACE --------------------
interface TransactionFilter {
    boolean apply(Transaction txn);
}

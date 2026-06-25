package week_4_5_6_7;

import java.util.*;
import java.util.concurrent.*;

/**
 * PAYMENT GATEWAY - LOW LEVEL DESIGN
 *
 * Patterns used:
 *  - Strategy              -> PaymentMethod (CreditCard / UPI / Wallet)
 *  - Adapter               -> PaymentGateway (Razorpay / Stripe wrappers over fake vendor SDKs)
 *  - Factory               -> PaymentMethodFactory, PaymentGatewayFactory
 *  - Chain of Responsibility -> PaymentValidator (amount -> currency -> fraud)
 *  - Observer              -> PaymentEventListener (email / webhook)
 *  - State                 -> Transaction status transition map
 *  - Singleton             -> PaymentService, IdempotencyStore
 *
 * Idempotency:
 *  - ConcurrentHashMap.putIfAbsent() atomically "claims" an idempotency key.
 *  - IN_PROGRESS  -> a concurrent duplicate is rejected (409-style)
 *  - COMPLETED    -> a later duplicate gets the cached response, no re-charge
 */
public class PaymentGatewaySystem {

    // =====================================================================
    // ENUMS
    // =====================================================================

    enum PaymentMethodType { CREDIT_CARD, UPI, WALLET }

    enum GatewayProvider { RAZORPAY, STRIPE }

    enum TransactionStatus { CREATED, VALIDATING, PROCESSING, SUCCESS, FAILED, REFUNDED }

    enum IdempotencyStatus { IN_PROGRESS, COMPLETED }

    // =====================================================================
    // CORE ENTITIES
    // =====================================================================

    static class Order {
        final String orderId;
        final String userId;
        final double amount;
        final String currency;

        Order(String orderId, String userId, double amount, String currency) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
            this.currency = currency;
        }
    }

    static class PaymentRequest {
        final String idempotencyKey;
        final Order order;
        final PaymentMethodType methodType;
        final GatewayProvider preferredGateway;
        final Map<String, String> paymentDetails;

        PaymentRequest(String idempotencyKey, Order order, PaymentMethodType methodType,
                       GatewayProvider preferredGateway, Map<String, String> paymentDetails) {
            this.idempotencyKey = idempotencyKey;
            this.order = order;
            this.methodType = methodType;
            this.preferredGateway = preferredGateway;
            this.paymentDetails = paymentDetails;
        }
    }

    /** Uniform response after the adapter normalizes a vendor-specific reply. */
    static class GatewayResponse {
        final boolean success;
        final String gatewayTransactionId;
        final String message;

        GatewayResponse(boolean success, String gatewayTransactionId, String message) {
            this.success = success;
            this.gatewayTransactionId = gatewayTransactionId;
            this.message = message;
        }
    }

    /** What the client of PaymentService gets back. Cached verbatim for idempotent replays. */
    static class PaymentResponse {
        final String transactionId;
        final TransactionStatus status;
        final String message;

        PaymentResponse(String transactionId, TransactionStatus status, String message) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
        }

        @Override
        public String toString() {
            return "PaymentResponse{txnId=" + transactionId + ", status=" + status + ", msg=" + message + "}";
        }
    }

    /**
     * STATE PATTERN: status can only move along edges defined in TRANSITIONS.
     * Any other transition throws -> impossible to e.g. jump SUCCESS -> CREATED.
     */
    static class Transaction {
        final String transactionId;
        final Order order;
        final PaymentMethodType methodType;
        final GatewayProvider gatewayProvider;
        final List<TransactionStatus> statusHistory = new ArrayList<>();
        TransactionStatus status;
        String gatewayTransactionId;

        private static final Map<TransactionStatus, Set<TransactionStatus>> TRANSITIONS = Map.of(
                TransactionStatus.CREATED,    EnumSet.of(TransactionStatus.VALIDATING, TransactionStatus.FAILED),
                TransactionStatus.VALIDATING, EnumSet.of(TransactionStatus.PROCESSING, TransactionStatus.FAILED),
                TransactionStatus.PROCESSING, EnumSet.of(TransactionStatus.SUCCESS, TransactionStatus.FAILED),
                TransactionStatus.SUCCESS,    EnumSet.of(TransactionStatus.REFUNDED),
                TransactionStatus.FAILED,     EnumSet.of(TransactionStatus.PROCESSING), // allow retry
                TransactionStatus.REFUNDED,   EnumSet.noneOf(TransactionStatus.class)
        );

        Transaction(Order order, PaymentMethodType methodType, GatewayProvider gatewayProvider) {
            this.transactionId = "TXN-" + UUID.randomUUID();
            this.order = order;
            this.methodType = methodType;
            this.gatewayProvider = gatewayProvider;
            this.status = TransactionStatus.CREATED;
            this.statusHistory.add(status);
        }

        synchronized void transitionTo(TransactionStatus newStatus) {
            Set<TransactionStatus> allowed = TRANSITIONS.get(status);
            if (allowed == null || !allowed.contains(newStatus)) {
                throw new IllegalStateException("Invalid transition: " + status + " -> " + newStatus);
            }
            status = newStatus;
            statusHistory.add(status);
        }
    }

    // =====================================================================
    // EXCEPTIONS
    // =====================================================================

    static class ValidationException extends RuntimeException {
        ValidationException(String message) { super(message); }
    }

    /** Thrown when a request with the same idempotency key is currently being processed. */
    static class DuplicateRequestInProgressException extends RuntimeException {
        DuplicateRequestInProgressException(String message) { super(message); }
    }

    // =====================================================================
    // STRATEGY: PaymentMethod -> validates method-specific details
    // =====================================================================

    interface PaymentMethod {
        void validateDetails(PaymentRequest request);
    }

    static class CreditCardPayment implements PaymentMethod {
        @Override
        public void validateDetails(PaymentRequest request) {
            String card = request.paymentDetails.get("cardNumber");
            if (card == null || card.length() < 13) {
                throw new ValidationException("Invalid card number");
            }
        }
    }

    static class UpiPayment implements PaymentMethod {
        @Override
        public void validateDetails(PaymentRequest request) {
            String upiId = request.paymentDetails.get("upiId");
            if (upiId == null || !upiId.contains("@")) {
                throw new ValidationException("Invalid UPI id");
            }
        }
    }

    static class WalletPayment implements PaymentMethod {
        @Override
        public void validateDetails(PaymentRequest request) {
            if (request.paymentDetails.get("walletId") == null) {
                throw new ValidationException("Wallet id is required");
            }
        }
    }

    /** FACTORY: hides the mapping of enum -> strategy implementation. */
    static class PaymentMethodFactory {
        private static final Map<PaymentMethodType, PaymentMethod> STRATEGIES = Map.of(
                PaymentMethodType.CREDIT_CARD, new CreditCardPayment(),
                PaymentMethodType.UPI, new UpiPayment(),
                PaymentMethodType.WALLET, new WalletPayment()
        );

        static PaymentMethod get(PaymentMethodType type) {
            PaymentMethod method = STRATEGIES.get(type);
            if (method == null) {
                throw new IllegalArgumentException("Unsupported payment method: " + type);
            }
            return method;
        }
    }

    // =====================================================================
    // SIMULATED THIRD-PARTY SDKS
    // (deliberately different shapes from each other and from our domain ->
    //  this is *why* we need the Adapter pattern)
    // =====================================================================

    static class RazorpayResult {
        final boolean captured;
        final String id;
        final String message;

        RazorpayResult(boolean captured, String id, String message) {
            this.captured = captured;
            this.id = id;
            this.message = message;
        }
    }

    /** Razorpay-style SDK: amounts in paise (long), its own result object. */
    static class RazorpaySDKClient {
        RazorpayResult makePayment(String orderId, long amountInPaise, String method) {
            // Simulated network call to Razorpay.
            return new RazorpayResult(true, "rzp_" + UUID.randomUUID(), "Payment captured");
        }

        RazorpayResult makeRefund(String gatewayTxnId, long amountInPaise) {
            return new RazorpayResult(true, "rzp_rfnd_" + UUID.randomUUID(), "Refund processed");
        }
    }

    static class StripeCharge {
        final String id;
        final String status;

        StripeCharge(String id, String status) {
            this.id = id;
            this.status = status;
        }
    }

    /** Stripe-style SDK: amounts in cents (long), its own response object. */
    static class StripeSDKClient {
        StripeCharge createCharge(String customerId, long amountInCents, String currency) {
            // Simulated network call to Stripe.
            return new StripeCharge("ch_" + UUID.randomUUID(), "succeeded");
        }

        StripeCharge createRefund(String chargeId) {
            return new StripeCharge("re_" + UUID.randomUUID(), "succeeded");
        }
    }

    // =====================================================================
    // ADAPTER: PaymentGateway -> uniform interface over vendor SDKs
    // =====================================================================

    interface PaymentGateway {
        GatewayResponse charge(Transaction txn, PaymentRequest request);
        GatewayResponse refund(Transaction txn);
    }

    static class RazorpayGatewayAdapter implements PaymentGateway {
        private final RazorpaySDKClient client = new RazorpaySDKClient();

        @Override
        public GatewayResponse charge(Transaction txn, PaymentRequest request) {
            long amountInPaise = Math.round(request.order.amount * 100);
            RazorpayResult result = client.makePayment(request.order.orderId, amountInPaise, request.methodType.name());
            return new GatewayResponse(result.captured, result.id, result.message);
        }

        @Override
        public GatewayResponse refund(Transaction txn) {
            long amountInPaise = Math.round(txn.order.amount * 100);
            RazorpayResult result = client.makeRefund(txn.gatewayTransactionId, amountInPaise);
            return new GatewayResponse(result.captured, result.id, result.message);
        }
    }

    static class StripeGatewayAdapter implements PaymentGateway {
        private final StripeSDKClient client = new StripeSDKClient();

        @Override
        public GatewayResponse charge(Transaction txn, PaymentRequest request) {
            long amountInCents = Math.round(request.order.amount * 100);
            StripeCharge charge = client.createCharge(request.order.userId, amountInCents, request.order.currency);
            boolean success = "succeeded".equals(charge.status);
            return new GatewayResponse(success, charge.id, "Stripe charge " + charge.status);
        }

        @Override
        public GatewayResponse refund(Transaction txn) {
            StripeCharge refund = client.createRefund(txn.gatewayTransactionId);
            boolean success = "succeeded".equals(refund.status);
            return new GatewayResponse(success, refund.id, "Stripe refund " + refund.status);
        }
    }

    /** FACTORY: hides the mapping of enum -> adapter implementation. */
    static class PaymentGatewayFactory {
        private static final Map<GatewayProvider, PaymentGateway> GATEWAYS = Map.of(
                GatewayProvider.RAZORPAY, new RazorpayGatewayAdapter(),
                GatewayProvider.STRIPE, new StripeGatewayAdapter()
        );

        static PaymentGateway getGateway(GatewayProvider provider) {
            PaymentGateway gateway = GATEWAYS.get(provider);
            if (gateway == null) {
                throw new IllegalArgumentException("Unsupported gateway: " + provider);
            }
            return gateway;
        }
    }

    // =====================================================================
    // CHAIN OF RESPONSIBILITY: validators run before any gateway call
    // =====================================================================

    abstract static class PaymentValidator {
        private PaymentValidator next;

        PaymentValidator setNext(PaymentValidator next) {
            this.next = next;
            return next;
        }

        void validate(PaymentRequest request) {
            check(request);
            if (next != null) {
                next.validate(request);
            }
        }

        protected abstract void check(PaymentRequest request);
    }

    static class AmountValidator extends PaymentValidator {
        private static final double MAX_AMOUNT = 200000.0; // e.g. 2 lakh per-transaction cap

        @Override
        protected void check(PaymentRequest request) {
            if (request.order.amount <= 0) {
                throw new ValidationException("Amount must be positive");
            }
            if (request.order.amount > MAX_AMOUNT) {
                throw new ValidationException("Amount exceeds maximum transaction limit");
            }
        }
    }

    static class CurrencyValidator extends PaymentValidator {
        private static final Set<String> SUPPORTED = Set.of("INR", "USD");

        @Override
        protected void check(PaymentRequest request) {
            if (!SUPPORTED.contains(request.order.currency)) {
                throw new ValidationException("Unsupported currency: " + request.order.currency);
            }
        }
    }

    static class FraudCheckValidator extends PaymentValidator {
        @Override
        protected void check(PaymentRequest request) {
            // Placeholder for real risk scoring: velocity checks, device fingerprinting, etc.
            if (request.order.amount > 100000 && request.methodType == PaymentMethodType.WALLET) {
                throw new ValidationException("Flagged for manual review: high value wallet transaction");
            }
        }
    }

    // =====================================================================
    // OBSERVER: notify other systems on status change
    // =====================================================================

    interface PaymentEventListener {
        void onStatusChange(Transaction txn);
    }

    static class EmailNotificationListener implements PaymentEventListener {
        @Override
        public void onStatusChange(Transaction txn) {
            System.out.println("[Email]   notify user -> txn=" + txn.transactionId + " status=" + txn.status);
        }
    }

    static class WebhookNotificationListener implements PaymentEventListener {
        @Override
        public void onStatusChange(Transaction txn) {
            System.out.println("[Webhook] POST /merchant/webhook -> txn=" + txn.transactionId + " status=" + txn.status);
        }
    }

    static class PaymentEventPublisher {
        private final List<PaymentEventListener> listeners = new CopyOnWriteArrayList<>();

        void subscribe(PaymentEventListener listener) {
            listeners.add(listener);
        }

        void publish(Transaction txn) {
            for (PaymentEventListener listener : listeners) {
                listener.onStatusChange(txn);
            }
        }
    }

    // =====================================================================
    // IDEMPOTENCY STORE - the heart of "no double charge"
    // =====================================================================

    static class IdempotencyRecord {
        volatile IdempotencyStatus status;
        volatile PaymentResponse response;

        IdempotencyRecord() {
            this.status = IdempotencyStatus.IN_PROGRESS;
        }
    }

    static class IdempotencyStore {
        private final ConcurrentHashMap<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

        /**
         * Atomically claims the key.
         * Returns null  -> caller now OWNS this key, proceed with processing.
         * Returns record -> someone already claimed/finished this key.
         */
        IdempotencyRecord getOrClaim(String key) {
            return store.putIfAbsent(key, new IdempotencyRecord());
        }

        void markCompleted(String key, PaymentResponse response) {
            IdempotencyRecord record = store.get(key);
            record.response = response;
            record.status = IdempotencyStatus.COMPLETED;
        }
    }

    // =====================================================================
    // ORCHESTRATOR (Facade + Singleton)
    // =====================================================================

    static class PaymentService {
        private static final PaymentService INSTANCE = new PaymentService();

        private final IdempotencyStore idempotencyStore = new IdempotencyStore();
        private final PaymentEventPublisher eventPublisher = new PaymentEventPublisher();
        private final Map<String, Transaction> transactionStore = new ConcurrentHashMap<>();

        private PaymentService() {
            eventPublisher.subscribe(new EmailNotificationListener());
            eventPublisher.subscribe(new WebhookNotificationListener());
        }

        static PaymentService getInstance() {
            return INSTANCE;
        }

        PaymentResponse processPayment(PaymentRequest request) {

            // ---- Step 1: idempotency check (atomic claim) ----
            IdempotencyRecord existing = idempotencyStore.getOrClaim(request.idempotencyKey);
            if (existing != null) {
                if (existing.status == IdempotencyStatus.COMPLETED) {
                    System.out.println("[Idempotency] cache hit for key=" + request.idempotencyKey
                            + " -> returning previous result, no re-charge");
                    return existing.response;
                }
                throw new DuplicateRequestInProgressException(
                        "Request with idempotency key " + request.idempotencyKey + " is already being processed");
            }

            // From here on, this thread is the sole owner of this idempotency key.
            try {
                Transaction txn = new Transaction(request.order, request.methodType, request.preferredGateway);
                transactionStore.put(txn.transactionId, txn);

                // ---- Step 2: validation chain ----
                txn.transitionTo(TransactionStatus.VALIDATING);
                buildValidatorChain().validate(request);

                // ---- Step 3: method-specific validation (Strategy) ----
                PaymentMethod method = PaymentMethodFactory.get(request.methodType);
                method.validateDetails(request);

                // ---- Step 4: charge via gateway (Adapter via Factory) ----
                txn.transitionTo(TransactionStatus.PROCESSING);
                PaymentGateway gateway = PaymentGatewayFactory.getGateway(request.preferredGateway);
                GatewayResponse gatewayResponse = gateway.charge(txn, request);
                txn.gatewayTransactionId = gatewayResponse.gatewayTransactionId;

                // ---- Step 5: update state (State pattern) ----
                txn.transitionTo(gatewayResponse.success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);

                // ---- Step 6: notify observers ----
                eventPublisher.publish(txn);

                // ---- Step 7: cache response for idempotent replays ----
                PaymentResponse response = new PaymentResponse(txn.transactionId, txn.status, gatewayResponse.message);
                idempotencyStore.markCompleted(request.idempotencyKey, response);
                return response;

            } catch (ValidationException ex) {
                PaymentResponse failureResponse = new PaymentResponse(null, TransactionStatus.FAILED, ex.getMessage());
                idempotencyStore.markCompleted(request.idempotencyKey, failureResponse);
                return failureResponse;
            }
        }

        private PaymentValidator buildValidatorChain() {
            PaymentValidator chain = new AmountValidator();
            chain.setNext(new CurrencyValidator()).setNext(new FraudCheckValidator());
            return chain;
        }
    }

    // =====================================================================
    // DEMO
    // =====================================================================

    public static void main(String[] args) throws InterruptedException {
        PaymentService service = PaymentService.getInstance();

        // ---- Scenario 1: normal payment, then a duplicate retry with the same key ----
        Order order1 = new Order("ORDER-1001", "USER-1", 4999.0, "INR");
        PaymentRequest request1 = new PaymentRequest(
                "idem-key-001", order1, PaymentMethodType.UPI, GatewayProvider.RAZORPAY,
                Map.of("upiId", "pradeep@upi"));

        System.out.println("== First call ==");
        PaymentResponse r1 = service.processPayment(request1);
        System.out.println(r1);

        System.out.println("\n== Duplicate call with SAME idempotency key (e.g. client retry) ==");
        PaymentResponse r2 = service.processPayment(request1);
        System.out.println(r2);
        System.out.println("Same transaction id returned? " + (r1.transactionId.equals(r2.transactionId)));

        // ---- Scenario 2: concurrent duplicate requests racing on the same key ----
        System.out.println("\n== Concurrent duplicate requests with SAME idempotency key ==");
        Order order2 = new Order("ORDER-1002", "USER-1", 1500.0, "INR");
        PaymentRequest request2 = new PaymentRequest(
                "idem-key-002", order2, PaymentMethodType.CREDIT_CARD, GatewayProvider.STRIPE,
                Map.of("cardNumber", "4111111111111111"));

        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    PaymentResponse r = service.processPayment(request2);
                    System.out.println(Thread.currentThread().getName() + " -> " + r);
                } catch (DuplicateRequestInProgressException ex) {
                    System.out.println(Thread.currentThread().getName() + " -> rejected: " + ex.getMessage());
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // ---- Scenario 3: validation failure (bad currency) ----
        System.out.println("\n== Invalid request (unsupported currency) ==");
        Order order3 = new Order("ORDER-1003", "USER-2", 200.0, "JPY");
        PaymentRequest request3 = new PaymentRequest(
                "idem-key-003", order3, PaymentMethodType.WALLET, GatewayProvider.RAZORPAY,
                Map.of("walletId", "wallet-123"));
        System.out.println(service.processPayment(request3));
    }
}
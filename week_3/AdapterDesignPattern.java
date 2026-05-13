package week_3;

interface IPayment{
    void pay();
}

class GrowwPayment implements IPayment{
    @Override
    public void pay() {
        System.out.println("payment by Growww");
    }
}

class StripePayment{
    public void stripePayment() {
        System.out.println("Stripe own payment 3rd Party");
    }
}

class PaymentAdapterStripe implements IPayment{
    StripePayment payment;
    public PaymentAdapterStripe(){
        payment = new StripePayment();
    }
    @Override
    public void pay() {
        payment.stripePayment();
    }
}

public class AdapterDesignPattern {
    public static void main(String[] args) {
        IPayment payment = new StripePayment();
        payment.pay();
    }    
}

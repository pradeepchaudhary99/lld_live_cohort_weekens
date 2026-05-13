package week_1;


// Discount...
interface Discount{
    int discounted_price(int money);
}

class Ten_Discount implements Discount{
    float discount = 0.10f;
    @Override
    public int discounted_price(int money) {
        return (int)(money*discount);
    }
}

class Diwali_Discount implements Discount{
    float discount = 0.20f;
    @Override
    public int discounted_price(int money) {
        return (int)(money*discount);
    }
}

class DiscountFactory{
    static Discount getDiscountedPrice(int amount){
        if(amount < 5000){
            return new Ten_Discount();
        }
        else if(amount > 10000){
            return new Diwali_Discount();
        }
        return null;
    }
}

class DiscountCalulator{
    
    public int getDiscountedAmount(int amount){
        Discount relevantDiscount = DiscountFactory.getDiscountedPrice(amount);
        return relevantDiscount.discounted_price(amount);
    }
}

public class Factory_Design_Pattern {
    public static void main(String[] args) {
        
    }
}

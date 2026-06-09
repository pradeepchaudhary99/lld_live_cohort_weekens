package week_1;

import java.lang.constant.DirectMethodHandleDesc;

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

interface DiscountFactory{
    Discount createDiscount();
}

class Ten_DiscountFactory implements DiscountFactory{
    @Override
    public Discount createDiscount() {
        return new Ten_Discount();
    }
}

class PradeepDiscount implements Discount{
    @Override
    public int discounted_price(int money) {
        return 30;
    }
}

class PradeepDiscountFactory implements DiscountFactory{

    @Override
    public Discount createDiscount() {
        return new PradeepDiscount();
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

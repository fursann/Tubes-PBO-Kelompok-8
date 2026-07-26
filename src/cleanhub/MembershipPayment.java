package cleanhub;

public class MembershipPayment implements DiscountSystem {
    private static final double DISCOUNT_RATE = 0.10;
    private final Customer customer;

    public MembershipPayment(Customer customer) {
        this.customer = customer;
    }

    @Override
    public double applyDiscount(double amount) {
        if (validateStatus()) {
            System.out.println("[MembershipPayment] Status Valid! Anda mendapatkan diskon 10%.");
            return amount - (amount * DISCOUNT_RATE);
        }
        System.out.println("[MembershipPayment] Anda bukan member. Tidak ada diskon.");
        return amount;
    }

    @Override
    public boolean validateStatus() {
        return customer.isMember();
    }
}

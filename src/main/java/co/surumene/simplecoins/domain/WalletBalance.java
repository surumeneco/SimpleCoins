package co.surumene.simplecoins.domain;

/** Immutable value transition, without inventory side-effects. */
public record WalletBalance(long amount) {
    public WalletBalance {
        if (amount < 0) throw new IllegalArgumentException("Negative balance");
    }

    /** May not deposit while above a newly reduced limit; existing balance is untouched. */
    public WalletBalance deposit(long value, long limit) {
        if (value <= 0 || limit < 0) throw new IllegalArgumentException("Invalid deposit or limit");
        long next = Math.addExact(amount, value);
        if (next > limit) throw new IllegalStateException("Wallet capacity exceeded");
        return new WalletBalance(next);
    }

    public WalletBalance withdraw(long value) {
        if (value <= 0) throw new IllegalArgumentException("Invalid withdrawal");
        if (value > amount) throw new IllegalStateException("Insufficient balance");
        return new WalletBalance(amount - value);
    }
}

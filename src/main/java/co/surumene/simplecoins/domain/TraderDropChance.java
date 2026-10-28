package co.surumene.simplecoins.domain;

/** Validates configuration before deciding whether a trader may mint currency. */
public final class TraderDropChance {
    private TraderDropChance() {}

    public static boolean shouldDrop(double chance, double random) {
        if (!Double.isFinite(random) || random < 0.0 || random >= 1.0)
            throw new IllegalArgumentException("Random sample must be in [0, 1)");
        if (!Double.isFinite(chance) || chance <= 0.0 || chance > 1.0) return false;
        return random < chance;
    }
}

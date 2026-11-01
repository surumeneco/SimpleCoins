package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only queries over an already committed, coherent settlement result.
 * Caller must authenticate admin permissions and supply a published snapshot;
 * this class does not scan worlds or authorize command senders.
 */
public final class AdminSettlementQuery {
    private AdminSettlementQuery() {}

    public static AssetStatistics.Snapshot statistics(
            SettlementAccumulator.Result result) throws IOException {
        validate(result);
        return result.statistics();
    }

    /** Empty means unregistered/unknown, not a registered zero balance. */
    public static Optional<BigInteger> personalBalance(
            SettlementAccumulator.Result result, UUID player) throws IOException {
        if (player == null)
            throw new IOException("Missing requested player UUID");
        validate(result);
        return Optional.ofNullable(result.personalBalances().get(player));
    }

    public static List<SettlementAccumulator.DeadLocation> deadLocations(
            SettlementAccumulator.Result result) throws IOException {
        validate(result);
        return List.copyOf(result.deadLocations());
    }

    public static List<SettlementAccumulator.DeadLocation> deadLocationsFor(
            SettlementAccumulator.Result result, UUID owner) throws IOException {
        if (owner == null)
            throw new IOException("Missing requested dead-asset owner UUID");
        validate(result);
        return result.deadLocations().stream()
                .filter(location -> owner.equals(location.owner())).toList();
    }

    private static void validate(SettlementAccumulator.Result result) throws IOException {
        if (result == null)
            throw new IOException("Missing committed settlement result");
        try {
            Objects.requireNonNull(result.personalBalances());
            Objects.requireNonNull(result.statistics());
            Objects.requireNonNull(result.deadLocations());
            var computed = AssetStatistics.calculate(
                    result.personalBalances(), result.free(), result.dead());
            if (!computed.equals(result.statistics()))
                throw new IOException("Inconsistent committed settlement statistics");
            BigInteger located = BigInteger.ZERO;
            for (var location : result.deadLocations()) {
                if (location == null || location.owner() == null ||
                        location.reason() == null || location.value() == null ||
                        location.value().signum() <= 0 || location.location() == null ||
                        location.location().isBlank())
                    throw new IOException("Malformed dead-asset location evidence");
                located = located.add(location.value());
            }
            if (!located.equals(result.dead()))
                throw new IOException("Dead-asset location totals do not match the settlement");
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid committed settlement query evidence", invalid);
        }
    }
}

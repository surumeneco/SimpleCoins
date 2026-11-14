package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrantDistributionPlanTest {

    @Test void amountFormUsesMinimumCoinsAndBoundedPhysicalStacksForEveryRecipient() {
        GrantDistributionPlan plan = GrantDistributionPlan.forAmount(5200, 2);
        assertEquals(List.of(
                new GrantDistributionPlan.CoinStack(CoinDenomination.FIVE_THOUSAND, 1),
                new GrantDistributionPlan.CoinStack(CoinDenomination.HUNDRED, 2)),
                plan.coinStacks());
        assertEquals(2, plan.recipientCount());
        assertEquals(5200L, plan.monetaryValuePerRecipient());
        assertEquals(BigInteger.valueOf(10400), plan.totalMonetaryValue());
        assertEquals(2L, plan.stacksPerRecipient());
        assertEquals(4L, plan.totalOutputStacks());
        assertEquals(0L, plan.emptyWalletCountPerRecipient());
        assertThrows(UnsupportedOperationException.class, plan.coinStacks()::clear);
    }

    @Test void explicitCoinFormPreservesDenominationAndSplitsAt64() {
        GrantDistributionPlan plan =
                GrantDistributionPlan.forExactCoins(CoinDenomination.FIVE, 129, 2);
        assertEquals(List.of(
                new GrantDistributionPlan.CoinStack(CoinDenomination.FIVE, 64),
                new GrantDistributionPlan.CoinStack(CoinDenomination.FIVE, 64),
                new GrantDistributionPlan.CoinStack(CoinDenomination.FIVE, 1)),
                plan.coinStacks());
        assertEquals(645, plan.monetaryValuePerRecipient());
        assertEquals(BigInteger.valueOf(1290), plan.totalMonetaryValue());
        assertEquals(6L, plan.totalOutputStacks());
    }

    @Test void emptyWalletsAreNonStackableAndNeverInventMonetaryValue() {
        GrantDistributionPlan plan = GrantDistributionPlan.forEmptyWallets(3, 2);
        assertTrue(plan.coinStacks().isEmpty());
        assertEquals(3L, plan.emptyWalletCountPerRecipient());
        assertEquals(0L, plan.monetaryValuePerRecipient());
        assertEquals(BigInteger.ZERO, plan.totalMonetaryValue());
        assertEquals(3L, plan.stacksPerRecipient());
        assertEquals(6L, plan.totalOutputStacks());
    }

    @Test void globalStackBudgetIsEnforcedAcrossRecipientsBeforeAllocatingStacks() {
        assertEquals(1024L,
                GrantDistributionPlan.forEmptyWallets(512, 2).totalOutputStacks());
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forEmptyWallets(513, 2));
        assertEquals(1024L,
                GrantDistributionPlan.forExactCoins(CoinDenomination.ONE, 65536, 1)
                        .totalOutputStacks());
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forExactCoins(CoinDenomination.ONE, 65537, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forExactCoins(CoinDenomination.ONE, 1, 1025));
        assertEquals(1024L,
                GrantDistributionPlan.forAmount(655360000L, 1).totalOutputStacks());
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forAmount(655360001L, 1));
    }

    @Test void invalidTargetsAmountsCountsAndOverflowNeverMakeAnIssuancePlan() {
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forAmount(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forAmount(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forAmount(10, 0));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forExactCoins(CoinDenomination.TEN, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forExactCoins(CoinDenomination.TEN_THOUSAND,
                        Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDistributionPlan.forEmptyWallets(-1, 1));
        assertThrows(NullPointerException.class,
                () -> GrantDistributionPlan.forExactCoins(null, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new GrantDistributionPlan.CoinStack(CoinDenomination.ONE, 65));
    }
}

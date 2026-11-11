package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WalletTransferTransactionTest {
    private static final class Inventory {
        long wallet = 100;
        long physical = 50;
        int walletWrites;
        int coinWrites;
        int walletRestores;
        int coinRestores;

        Runnable depositWallet() { return () -> { walletWrites++; wallet += 50; }; }
        Runnable consumeCoins() { return () -> { coinWrites++; physical -= 50; }; }
        Runnable restoreWallet() { return () -> { walletRestores++; wallet = 100; }; }
        Runnable restoreCoins() { return () -> { coinRestores++; physical = 50; }; }

        void conserved() { assertEquals(150, wallet + physical); }
    }

    @Test void successfulDepositConservesValueWithoutRollback() {
        var i = new Inventory();
        WalletTransferTransaction.execute(i.depositWallet(), i.consumeCoins(),
                i.restoreWallet(), i.restoreCoins());
        assertEquals(150, i.wallet);
        assertEquals(0, i.physical);
        assertEquals(0, i.coinRestores + i.walletRestores);
        i.conserved();
    }

    @Test void failureBeforeAnyWriteStillAttemptsBothRestores() {
        var i = new Inventory();
        assertThrows(WalletTransferTransaction.RolledBack.class,
                () -> WalletTransferTransaction.execute(
                        () -> { throw new IllegalStateException("wallet unavailable"); },
                        i.consumeCoins(), i.restoreWallet(), i.restoreCoins()));
        assertEquals(0, i.coinWrites);
        assertEquals(1, i.walletRestores);
        assertEquals(1, i.coinRestores);
        assertEquals(100, i.wallet);
        assertEquals(50, i.physical);
        i.conserved();
    }

    @Test void physicalWriteFailureAfterPartialMutationRestoresBoth() {
        var i = new Inventory();
        var error = assertThrows(WalletTransferTransaction.RolledBack.class,
                () -> WalletTransferTransaction.execute(i.depositWallet(),
                        () -> {
                            i.consumeCoins().run();
                            throw new IllegalStateException("partial inventory write");
                        },
                        i.restoreWallet(), i.restoreCoins()));
        assertEquals("partial inventory write", error.getCause().getMessage());
        assertEquals(1, i.walletRestores);
        assertEquals(1, i.coinRestores);
        assertEquals(100, i.wallet);
        assertEquals(50, i.physical);
        i.conserved();
    }

    @Test void unrecoverableCoinRestoreStillAttemptsWalletRestore() {
        var i = new Inventory();
        var error = assertThrows(WalletTransferTransaction.Unrecoverable.class,
                () -> WalletTransferTransaction.execute(i.depositWallet(),
                        () -> { i.consumeCoins().run(); throw new IllegalStateException("coin write"); },
                        i.restoreWallet(),
                        () -> { i.coinRestores++; throw new IllegalStateException("coin rollback"); }));
        assertEquals("coin write", error.getCause().getMessage());
        assertEquals("coin rollback", error.getSuppressed()[0].getMessage());
        assertEquals(1, i.walletRestores);
        assertEquals(1, i.coinRestores);
        assertEquals(100, i.wallet);
        assertEquals(0, i.physical);
    }

    @Test void unrecoverableWalletRestoreStillAttemptsCoinRestore() {
        var i = new Inventory();
        var error = assertThrows(WalletTransferTransaction.Unrecoverable.class,
                () -> WalletTransferTransaction.execute(i.depositWallet(),
                        () -> { i.consumeCoins().run(); throw new IllegalStateException("coin write"); },
                        () -> { i.walletRestores++; throw new IllegalStateException("wallet rollback"); },
                        i.restoreCoins()));
        assertEquals(1, i.walletRestores);
        assertEquals(1, i.coinRestores);
        assertEquals("wallet rollback", error.getSuppressed()[0].getMessage());
        assertEquals(150, i.wallet);
        assertEquals(50, i.physical);
    }

    @Test void failureOfBothRestoresIsPreservedRatherThanMasked() {
        var i = new Inventory();
        var error = assertThrows(WalletTransferTransaction.Unrecoverable.class,
                () -> WalletTransferTransaction.execute(i.depositWallet(),
                        () -> { throw new AssertionError("coin write"); },
                        () -> { i.walletRestores++; throw new IllegalStateException("wallet restore"); },
                        () -> { i.coinRestores++; throw new IllegalStateException("coin restore"); }));
        assertEquals(1, i.walletRestores);
        assertEquals(1, i.coinRestores);
        assertInstanceOf(AssertionError.class, error.getCause());
        assertEquals("coin restore", error.getSuppressed()[0].getMessage());
        assertEquals("wallet restore", error.getSuppressed()[0].getSuppressed()[0].getMessage());
    }

    @Test void nullCallbacksRejectedBeforeAnyMutation() {
        var i = new Inventory();
        assertThrows(NullPointerException.class, () -> WalletTransferTransaction.execute(
                null, i.consumeCoins(), i.restoreWallet(), i.restoreCoins()));
        assertThrows(NullPointerException.class, () -> WalletTransferTransaction.execute(
                i.depositWallet(), null, i.restoreWallet(), i.restoreCoins()));
        assertThrows(NullPointerException.class, () -> WalletTransferTransaction.execute(
                i.depositWallet(), i.consumeCoins(), null, i.restoreCoins()));
        assertThrows(NullPointerException.class, () -> WalletTransferTransaction.execute(
                i.depositWallet(), i.consumeCoins(), i.restoreWallet(), null));
        assertEquals(0, i.walletWrites);
        assertEquals(0, i.coinWrites);
    }
}

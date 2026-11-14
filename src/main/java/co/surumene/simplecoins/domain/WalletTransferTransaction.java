package co.surumene.simplecoins.domain;

import java.util.Objects;

/**
 * A synchronous, in-memory, compensating update of the wallet item and the
 * corresponding physical coins.  The caller must take immutable snapshots
 * BEFORE invoking this boundary and run all callbacks on Paper's main thread.
 *
 * This is deliberately NOT a durable transaction. The world-save/crash/ledger
 * gap is owned by the settlement integration and must fail admission until
 * it can be independently reconciled.
 */
public final class WalletTransferTransaction {
    private WalletTransferTransaction() {}

    /** A mutation failed but both in-memory snapshots were restored. */
    public static final class RolledBack extends RuntimeException {
        public RolledBack(Throwable cause) {
            super("Wallet transfer rejected; original inventory state restored", cause);
        }
    }

    /** At least one snapshot could NOT be restored; halt the server. */
    public static final class Unrecoverable extends RuntimeException {
        public Unrecoverable(Throwable cause, Throwable rollbackFailure) {
            super("Wallet inventory consistency cannot be restored", cause);
            addSuppressed(rollbackFailure);
        }
    }

    /**
     * Write wallet value first, then consume/produce exact equivalent coins.
     * If any step fails, attempt BOTH restores even if the first restore
     * fails. Never claim a successful rollback when either restore failed.
     */
    public static void execute(
            Runnable writeWallet, Runnable writeCoins,
            Runnable restoreWallet, Runnable restoreCoins) {
        Objects.requireNonNull(writeWallet, "writeWallet");
        Objects.requireNonNull(writeCoins, "writeCoins");
        Objects.requireNonNull(restoreWallet, "restoreWallet");
        Objects.requireNonNull(restoreCoins, "restoreCoins");
        try {
            writeWallet.run();
            writeCoins.run();
        } catch (RuntimeException | Error failure) {
            Throwable rollbackFailure = null;
            try {
                restoreCoins.run();
            } catch (Throwable rollback) {
                rollbackFailure = rollback;
            }
            try {
                restoreWallet.run();
            } catch (Throwable rollback) {
                if (rollbackFailure == null) rollbackFailure = rollback;
                else rollbackFailure.addSuppressed(rollback);
            }
            if (rollbackFailure != null)
                throw new Unrecoverable(failure, rollbackFailure);
            throw new RolledBack(failure);
        }
    }
}

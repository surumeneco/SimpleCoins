package co.surumene.simplecoins.paper;

/** Pure, event-independent decision for drags performed while a wallet GUI is open. */
final class WalletDragPolicy {
    private WalletDragPolicy() {}

    static boolean shouldCancel(boolean alreadyCancelled, boolean walletSessionValid,
                                boolean cursorIsWallet, boolean lowerOnly,
                                boolean touchesWalletSource) {
        return alreadyCancelled || !walletSessionValid || cursorIsWallet
                || !lowerOnly || touchesWalletSource;
    }
}

package co.surumene.simplecoins.domain;

/** Choose the authoritative source for one player inventory partition. */
public final class InvseeSourceSelection {
    private InvseeSourceSelection() {}
    public enum Source { LIVE, EDITOR, SAVED }
    public static Source select(boolean online, boolean editorOpen) {
        return online ? Source.LIVE : editorOpen ? Source.EDITOR : Source.SAVED;
    }
}

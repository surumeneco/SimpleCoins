#!/bin/sh
# Non-normative, dependency-free reproduction of the Draft Red vectors.
# Exit 0 means both known defects were reproduced, NOT that the feature is Green.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
cat > "$tmp/EveryoneAliasProbe.java" <<'JAVA'
import co.surumene.simplecoins.domain.BlockLockerEveryoneText;
import java.io.IOException;
import java.util.Set;

public final class EveryoneAliasProbe {
    public static void main(String[] args) throws Exception {
        int expectedRed = 0;
        if (!BlockLockerEveryoneText.matches(
                " §b[EVERYONE]§r ", Set.of("Everyone")))
            throw new AssertionError("uncolored runtime alias baseline did not match");
        try {
            BlockLockerEveryoneText.matches(
                    "[Everyone]", Set.of("Everyone", "§aOther"));
            expectedRed++;
            System.out.println("EXPECTED RED: colored runtime alias was accepted");
        } catch (IOException expected) {
            System.out.println("NOT RED: colored alias rejected");
        }
        try {
            BlockLockerEveryoneText.matches(
                    "[Everyone]", Set.of("Everyone", "§r"));
            expectedRed++;
            System.out.println("EXPECTED RED: formatting-only alias was accepted");
        } catch (IOException expected) {
            System.out.println("NOT RED: malformed alias rejected");
        }
        if (BlockLockerEveryoneText.matches(
                "§b[Everyone] other§r", Set.of("Everyone")))
            throw new AssertionError("negative control incorrectly matched");
        if (expectedRed != 2)
            throw new AssertionError("expected exactly 2 reproducible Red vectors, got " + expectedRed);
        System.out.println("Reproduced exactly 2 expected Red vectors; 1 negative control passed.");
    }
}
JAVA
javac -encoding UTF-8 -d "$tmp" \
    "$root/src/main/java/co/surumene/simplecoins/domain/BlockLockerEveryoneText.java" \
    "$tmp/EveryoneAliasProbe.java"
java -cp "$tmp" EveryoneAliasProbe

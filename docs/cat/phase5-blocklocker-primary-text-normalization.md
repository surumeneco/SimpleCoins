# Phase 5: BlockLocker primary visible-name normalization (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-004.

## Process / PI

The saved visible sign header and Everyone tag already strip legacy Minecraft formatting codes before matching runtime aliases. The PRIVATE sign's first displayed profile is currently passed to the authoritative identity resolver after trimming only, so a color-formatted name may fail to resolve despite referring to the same player.

Normalize the first visible profile with the **same** legacy formatting-code removal and whitespace trimming as the header matcher. The normalized text is still only a lookup key: the identity resolver and optional saved UUID hint must independently verify ownership. An empty normalized profile remains UNKNOWN, and a lookup failure must not be converted to FREE.

## TCE / acceptance vectors

- SC-BL-NAME-01 (Red): `§a Alice §r` resolves through the authoritative lookup with the exact key `Alice`.
- SC-BL-NAME-02 (Regression): unformatted `Alice` is unchanged.
- SC-BL-NAME-03 (Regression): formatting-only profile remains UNKNOWN even when an Everyone tag is visible.
- SC-BL-NAME-04 (Regression): saved UUID hint disagreement remains UNKNOWN.

## Gate status

Formal CAT machine Gates **not-run**; Draft not Confirmed. This does not prove live BlockLocker localization, UUID resolution, world snapshot completeness or settlement publication.

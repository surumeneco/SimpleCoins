# Phase 5: BlockLocker localized Everyone profile matching (CAT Draft)

Status: Draft, not Confirmed. Sources: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md; BlockLocker v1.14.1 ProfileFactoryImpl.fromDisplayText.

## Process / PI

After a trusted adapter decodes a saved or live sign line into its displayed plain text, detect a BlockLocker Everyone profile using the **runtime translator's localized Everyone tag names**. BlockLocker constructs bracketed aliases such as `[Everyone]`, strips legacy color codes and compares case-insensitively. The caller must provide the actual runtime tag names without brackets; no English-only hard-coded assumption is sufficient. Malformed or absent configuration fails closed.

A true result is only a profile-text observation. It does not prove that the sign is a valid protection sign, that it is attached to the container, or that all applicable signs have been inspected. A false result is not evidence that the container is privately owned. Raw JSON components and saved PDC must be reconciled separately.

## Test model / TCE

- BL-EVERYONE-01 Red: localized and color-formatted Everyone tags match case-insensitively.
- BL-EVERYONE-02: unknown bracketed groups and trailing text do not match.
- BL-EVERYONE-03: missing, blank, malformed or bracket-containing runtime aliases fail closed.

Formal CAT machine gates not-run; Draft is not Confirmed.

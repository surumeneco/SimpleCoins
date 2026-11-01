# Phase 7: authoritative saved player receipt path (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-006. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/05_1.0.0_実装計画.md and the existing OfflineGrants playerdata readback contract.

## Process / PI

After Paper saves inventory and the issuance receipt, the server must verify one authoritative saved playerdata file for the exact recipient UUID. A symlinked file or any symlinked ancestor must not redirect the receipt read outside the declared world roots. Missing, ambiguous or non-regular candidates fail closed. The lookup never creates playerdata and does not treat an unreadable receipt as zero. The final NBT receipt comparison remains the existing OfflineGrants responsibility.

## Test model / TCE

- SC-RECEIPT-PATH-01: one ordinary saved `playerdata/<uuid>.dat` file is accepted, including the supported `players/data` layout.
- SC-RECEIPT-PATH-02: missing and duplicate candidates across worlds/layouts reject rather than selecting one arbitrarily.
- SC-RECEIPT-PATH-03: a symlinked saved file or symlinked directory/ancestor rejects before opening the receipt, including links pointing outside the world folder.
- SC-RECEIPT-PATH-04: null recipient or invalid/non-regular candidate fails closed; the existing receipt parser still checks UUID and all three PDC values after the path is resolved.

This is a filesystem selection boundary, not proof of an atomic world snapshot or full server crash recovery. Formal CAT machine Gates and Paper live integration are not-run.

- SC-RECEIPT-PATH-05: after locating the unique real path, receipt verification uses the existing bounded PlayerDataReader with separate compressed/inflated byte budgets and canonical UUID filename validation. A malformed, truncated, oversized or aliased saved receipt cannot be accepted as a zero receipt.

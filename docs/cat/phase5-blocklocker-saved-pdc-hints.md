# Phase 5: BlockLocker saved PDC hints (CAT Draft)

Status: Draft, not Confirmed. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md, BlockLocker v1.14.1 SignParserImpl / NbtSecretSignEntry / ProfileFactoryImpl.

## Process / boundary

Read the saved `PublicBukkitValues` map of a candidate sign without using Bukkit APIs. `blocklocker:header` identifies a saved `PRIVATE` or `MORE_USERS` sign. `blocklocker:profile_1` through `profile_6` are nested PDC compounds. A player profile has `blocklocker:n` (string) and optional `blocklocker:u` (two-long UUID). An Everyone marker has `blocklocker:e` (byte); BlockLocker v1.14.1 treats the *presence* of this Boolean key as an Everyone profile in its saved-profile factory, including a zero byte.

Return **hints only**: saved header, optional first-profile UUID on a PRIVATE sign, and presence of any saved Everyone marker. These hints must **never** be used alone for economic ownership: BlockLocker compares sign display text against PDC and prefers text on mismatch, accepts text-only legacy signs, and validates actual sign attachment, double chests and world state. The future authoritative resolver must reconcile all those inputs before passing ownership to settlement.

Malformed present profile compounds, malformed UUID arrays and orphan profiles without a header fail closed. A sign with no BlockLocker PDC is not evidence of an unprotected container.

## Test model

- BL-PDC-01: PRIVATE with first player profile UUID yields a candidate UUID, not a confirmed owner.
- BL-PDC-02: saved Everyone marker in any of six profiles is visible as a hint, including byte zero; never final free-asset classification.
- BL-PDC-03: MORE_USERS does not claim an owner from its first profile.
- BL-PDC-04: malformed UUID array, profile type or profile without a header throws IOException.
- BL-PDC-05: absent BlockLocker keys yields Optional.empty, requiring text/placement resolution.

Formal CAT machine gates: not-run. This Draft is not Confirmed and does not authorize settlement publication.

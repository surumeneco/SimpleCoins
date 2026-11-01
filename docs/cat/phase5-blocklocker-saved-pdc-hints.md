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

## Saved sign candidate enumeration (additional Draft vector)

A read-only terrain-chunk adapter may enumerate `minecraft:sign` and `minecraft:hanging_sign` block entities, validate their saved `PublicBukkitValues` compound, and expose only candidates with recognized BlockLocker headers. Use canonical block coordinates as keys and reject duplicate candidate positions. A sign without saved BlockLocker keys is *not* evidence of an unprotected container; legacy text-only signs require separate inspection. Non-sign block entities are not protection signs. This helper does not infer which containers a sign protects, does not reconcile sign display text, and cannot publish ownership.

- BL-PDC-06 Red: one saved sign with valid PRIVATE PDC produces a coordinate-keyed hint.
- BL-PDC-07 regression: a sign without PDC yields no hint, not a free-asset ownership decision.
- BL-PDC-08: malformed sign PDC and duplicate saved candidate positions throw IOException.
- Formal CAT machine Gates remain not-run; Draft not Confirmed.

## Declaring chunk coordinate integrity (additional Draft vector)

When a terrain chunk declares `xPos` and `zPos`, a saved sign candidate's block coordinates must lie inside that chunk. An out-of-chunk candidate cannot safely be associated with a nearby protected container. If either chunk coordinate is present, require both as integer NBT scalars and validate floorDiv(blockX, 16) and floorDiv(blockZ, 16). This is the same physical-source invariant already enforced for saved block inventories, not a new protection rule.

- BL-PDC-09 Red: sign at block x=16 inside a chunk declaring xPos=0 must throw IOException.
- BL-PDC-10 regression: sign at block x=15 inside chunk xPos=0 remains a candidate.
- Formal CAT machine Gates remain not-run; Draft not Confirmed.

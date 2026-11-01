# Phase 5: Ambiguous nested-storage components (CAT Draft)

Status: Draft (not Confirmed). Scope: read-only saved ItemStack valuation in SimpleCoins 1.0.0.
Source requirements: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md, 05_1.0.0_実装計画.md (A02/A03, G0).

## Process / invariant
A settlement may publish only after every authoritative physical item has been accounted for exactly once. A saved item that simultaneously advertises `minecraft:bundle_contents` and `minecraft:container` has ambiguous nested-storage representation. Do not add both collections or silently choose one. Reject the item with IOException and prevent a successful settlement result. A single supported collection continues to be valued normally.

## Test model
- Red: one parent item with both nested-storage component keys, each holding a valid 100-C coin, must fail closed rather than return 200 C.
- Regression: a bundle with only bundle_contents and a shulker with only container retain their current totals.
- Invalid or empty competing component is still ambiguous; presence of both keys is sufficient.

## Verification status
CAT machine gates (process-closure, TestModel generation, model-conformance, vectors, Queue, current-item, implementation-conformance): not-run; the dedicated Chat plugin/Skill is unavailable in this session. This Draft does not claim Confirmed status or substitute for a formal CAT Artifact.

## Additional physical-stack invariant (Draft)
A nonempty vanilla bundle or shulker box is not a stackable container of identical hidden inventories. If saved NBT reports count greater than one alongside a nested-storage component, fail closed rather than multiplying the nested balance by that count. Ordinary count-one containers remain valid. This is separate from the dual-component ambiguity and requires its own Red/Green cycle.

## Additional shulker slot invariant (Draft)
Vanilla shulker boxes have 27 physical inventory slots, indexed 0..26. A saved `minecraft:container` entry with slot 27 or higher on a shulker item is not a valid physical slot. Fail closed instead of accounting an impossible location. This is distinct from generic component storage and needs its own regression test.

## Additional malformed-components invariant (Draft)

Modern saved NBT may omit `components` for an untagged item, which contributes zero currency. An explicitly present `components: null` is not a valid NBT compound and must not be silently interpreted as absence; reject it with `IOException`. This prevents malformed source data from being accepted as a verified zero-value item.

Exploratory vectors: (1) `components` key present with null fails closed; (2) key absent remains a valid zero-value ordinary item; (3) a valid tagged coin remains countable; (4) `minecraft:bundle_contents` or `minecraft:container` present with null fails closed instead of silently dropping a potentially unreadable nested inventory. (5) `minecraft:custom_data` or nested `PublicBukkitValues` present with null is malformed, not an untagged item; absence remains valid. Formal CAT gates remain not-run.

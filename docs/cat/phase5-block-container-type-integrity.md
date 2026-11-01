# Phase 5: saved block container type integrity (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-003. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md.

## Process / PI

A saved block entity's generic `Items` list is authoritative physical currency only when its block-entity type is a supported vanilla inventory holder. A fabricated or future unknown type carrying an `Items` tag cannot be silently counted or ignored: abort the staging scan until an explicit adapter validates its physical storage semantics. Supported types include chest/trapped chest, barrel, hopper, dispenser/dropper, furnace family, brewing stand, crafter, campfire family, chiseled bookshelf and the standard uncolored/16 colored shulker boxes. Decorated pot is a separate single-item source and must not use `Items`.

## TCE

- BLOCK-TYPE-01 Red: an unknown vanilla or third-party block entity with a syntactically valid `Items` coin stack is rejected, not valued.
- BLOCK-TYPE-02: an invented shulker color suffix is rejected, while a real colored shulker box remains countable.
- BLOCK-TYPE-03: existing known container types and slot bounds continue to work.

This validates saved block entity IDs only. It does not establish loaded-world identity, ownership, BlockLocker protection or complete snapshot publication. Formal CAT machine Gates and Paper live-world tests not-run.
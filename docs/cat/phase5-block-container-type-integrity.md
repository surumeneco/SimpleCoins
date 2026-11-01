---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.block-container-type-integrity
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved block container type integrity (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-003. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md.


## 対象
| Ref | Aspect |
| --- | --- |
| saved BlockEntity | コンテナ型とItemsリストの権威性 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| block.type-unknown | 未知・未対応のBlockEntityがItemsを持つとき | 推測で資産計上せず、保存走査を拒否する | BLOCK-TYPE-01 |
| block.shulker | shulker型の色識別子を評価するとき | 実在するvanilla色のみ受け入れ、架空色を拒否する | BLOCK-TYPE-02 |
| block.known | 既知コンテナ型を読み取るとき | 既存のslot上限と正当な保存資産計上を維持する | BLOCK-TYPE-03 |

## Process / PI

A saved block entity's generic `Items` list is authoritative physical currency only when its block-entity type is a supported vanilla inventory holder. A fabricated or future unknown type carrying an `Items` tag cannot be silently counted or ignored: abort the staging scan until an explicit adapter validates its physical storage semantics. Supported types include chest/trapped chest, barrel, hopper, dispenser/dropper, furnace family, brewing stand, crafter, campfire family, chiseled bookshelf and the standard uncolored/16 colored shulker boxes. Decorated pot is a separate single-item source and must not use `Items`.

## TCE

- BLOCK-TYPE-01 Red: an unknown vanilla or third-party block entity with a syntactically valid `Items` coin stack is rejected, not valued.
- BLOCK-TYPE-02: an invented shulker color suffix is rejected, while a real colored shulker box remains countable.
- BLOCK-TYPE-03: existing known container types and slot bounds continue to work.

This validates saved block entity IDs only. It does not establish loaded-world identity, ownership, BlockLocker protection or complete snapshot publication. Formal CAT machine Gates and Paper live-world tests not-run.
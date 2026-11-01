---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.dangling-journal-head
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: dangling journal head is not an empty journal (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-005`.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | journal headのNOFOLLOW分類 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| head.link | journal headがdangling symlinkのとき | 存在する不正なdurable recordとして拒否し既存linkを維持する | SC-JOURNAL-HEAD-01 |
| head.empty | headがなくgrant記録もないとき | 正当な初期sequence zeroのみ生成する | SC-JOURNAL-HEAD-02 |
| head.missing-records | headがないがgrant記録が存在するとき | 空journalと誤認せずfail-closedとする | SC-JOURNAL-HEAD-03 |

## Process / PI

At journal startup, a dangling symbolic link at `head` is an existing but invalid durable record. Following symbolic links while testing for existence can misclassify that entry as absent and replace it with a fresh zero head, masking corrupted or redirected journal state.

Classify the `head` path with `NOFOLLOW_LINKS`. If the directory entry exists, the bounded `DurableFile.read` contract must validate it and reject non-regular/symbolic content. Only a genuinely absent head in a journal with no grant records may initialize sequence zero.

## TCE / acceptance vectors

- SC-JOURNAL-HEAD-01: dangling symlink at `head` and no `.grant` files -> constructor throws IOException, link remains unchanged.
- SC-JOURNAL-HEAD-02: absent head and empty journal -> sequence zero is initialized normally.
- SC-JOURNAL-HEAD-03: missing head with existing grant records -> fail closed.

This does not establish a complete historical journal audit or a Paper live recovery Gate.

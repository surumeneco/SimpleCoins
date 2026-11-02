---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.fatal-source-read
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: fatal source-read failure closes admission (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: TASK-20261101-005.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | source read失敗時のjoin barrier |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| barrier.exception | source callbackがunchecked exceptionを投げるとき | FAILEDに遷移して例外を伝播しjoin許可しない | SETTLEMENT-BARRIER-10 |
| barrier.error | source callbackがJVM Errorを投げるとき | FAILEDに遷移してErrorを伝播しjoin許可しない | SETTLEMENT-BARRIER-11 |
| barrier.null | source readerがnullのとき | source completeにせずfail-closedとする | SETTLEMENT-BARRIER-12 |
| barrier.latch | FAILED後に成功callbackがあるとき | 失敗したbarrierを再活性化しない | SETTLEMENT-BARRIER-13 |

## Process / PI

A source may fail with an unchecked exception or JVM `Error` while the settlement barrier is staging. A partially read source must never be marked complete. Any failure in the bounded read callback invalidates the entire staging attempt, and no join permit may be issued. The original failure must propagate without being converted into a successful read.

This tests the existing in-memory barrier contract only; it does not establish a coherent Paper snapshot or durable delta journal.

## TCE

- SETTLEMENT-BARRIER-10: an unchecked exception during source read sets the barrier to FAILED and propagates the exception.
- SETTLEMENT-BARRIER-11: an `AssertionError` during source read sets the barrier to FAILED and propagates the Error.
- SETTLEMENT-BARRIER-12: a null source reader fails closed, without marking its source complete.
- SETTLEMENT-BARRIER-13: after failure, a subsequent successful callback cannot revive the same barrier.

## Gate status

Formal CAT machine gates and live Paper fault injection **not-run**. This Draft does not authorize settlement publication, join admission or merge.

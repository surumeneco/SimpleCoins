---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.snapshot-publication
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase6: 決算結果の原子的な参照公開境界（Draft）

## Targets

| Ref | Aspect |
| --- | --- |
| SettlementSnapshotPublisher | 未確定集計の参照抑止・確定後の単一世代参照 |
| SettlementAccumulator | 全必須入力完了後のみcommit可能 |
| AdmissionBarrier | 未検証・停止・障害時の公開停止 |
| SettlementAdmissionListener | READYのみでなく公開済み結果も要求 |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SNAP-01 | SCANNING または BLOCKED | 現在値の参照と入室を許さない | invisibleBeforeReady |
| SNAP-02 | 全必須入力完了のAccumulatorをSCANNINGで一回登録 | commit後の不変Resultを保持し、READYまで参照不可 | validPublicationIsGated |
| SNAP-03 | 入力不足・失敗・二重登録 | 公開を取り消し、fail-closedする | incompleteResultFailsClosed |
| SNAP-04 | READYだけで実際のResult未公開 | 入室を許可しない | readyWithoutPublicationDenies |
| SNAP-05 | READY後に重大な異常または停止 | 参照許可を即座に撤回する | fatalFailureRevokesVisibility |

## Boundaries

このDraftは、起動時決算の全件正本性、Paperでの実際のリスナー登録、ワールド保存時点一致、外部BAN/BlockLocker/WWC/WWW所有権、永続checkpoint、正式CAT implementation-conformanceを証明しない。単一Javaプロセス中の参照公開境界だけを対象とし、status: draftをConfirmedとみなさない。

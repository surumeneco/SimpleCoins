---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.generation-visibility-fault
status: candidate
process: simplecoins.settlement-journal
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/e293a7b9f9ebaf1ad5559d45c0204046e1d04494/src/main/java/co/surumene/simplecoins/domain/SettlementSnapshotStore.java
  - https://github.com/surumeneco/SimpleCoins/blob/e293a7b9f9ebaf1ad5559d45c0204046e1d04494/src/main/java/co/surumene/simplecoins/domain/SettlementGenerationPublisher.java
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: 決算世代の公開境界と第二世代の故障注入（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.generation-atomicity | 既存の完全な世代を次世代の書込途中に失わない |
| simplecoins.snapshot.concurrent-visibility | 新世代の永続化が確定する前に部分的な新世代を読み出さない |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| RC-GEN-01 | 世代1が永続化済みで、世代2のsnapshot置換前にI/O例外が起きる | 世代1の完全な記録を読める。永続化済みHEADを読み直してから世代2を再試行できる | failedSecondGenerationRetainsPriorHeadAndCanRecover |
| RC-GEN-02 | 世代2の書込がsnapshot置換前の故障注入hookで停止している | 同時読取は世代1の完全な記録を返し、hook解放後に世代2の完全な記録を返す | concurrentReaderSeesOldHeadUntilSecondGenerationCommit |

## Evidence and limits

- PR #10の既存コードにあるpackage-private `BeforeSnapshotWrite` hookを使用するcode-to-spec Candidateであり、ユーザーが新しい規範をConfirmedにしたことを意味しない。
- 本テストは単一ホスト・単一プロセスでの可視性と第二世代の故障注入のみを検証する。電源断、fsync、OSキャッシュ、複数プロセス、Paper/JE/BEの実機整合は対象外。
- CAT正式Process/PI/TCE意味確認、machine Gates、Paper実機検証は未実施。上流PR #10の実装を独立にdevelopへ統合する根拠にはしない。

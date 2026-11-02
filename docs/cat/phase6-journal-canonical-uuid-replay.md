---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.journal-canonical-uuid-replay
status: candidate
process: simplecoins.grant-journal
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/e293a7b9f9ebaf1ad5559d45c0204046e1d04494/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase6-crash-recovery-acceptance.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: 永続予約レコードのUUID正規表記と再読込（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.grant-journal.replay-uuid | チェックサムが正しくても非正規のgrant/player UUID表記を受け入れない |
| simplecoins.grant-journal.replay-shape | 正規レコードは復元し、末尾余剰データは拒否する |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-JUUID-01 | checksummed SCG2 grant IDが大文字UUID | IOExceptionで拒否し、記録を復元しない | uppercaseGrantUuidMustNotReplay |
| SC-JUUID-02 | checksummed SCG2 player IDが省略UUID表記 | IOExceptionで拒否し、記録を復元しない | abbreviatedPlayerUuidMustNotReplay |
| SC-JUUID-03 | 両UUIDが正規表記でheadとrecordが整合 | 予約1件を復元できる | canonicalUuidJournalReplaysOnePendingGrant |
| SC-JUUID-04 | 正規SCG2 payloadに余剰byteがあるがchecksumは正しい | IOExceptionで拒否する | checksummedTrailingBytesMustNotReplay |

## Status and boundaries

既存`GrantLedger.decode`の`UUID.fromString`後の正規表記照合と余剰データ検査を検証するcode-to-spec Candidate。新しい仕様決定ではなく、CAT Confirmedではない。RC-07〜09のジャーナル受入境界に関連するが、再起動故障注入、writer排他、実際のPaper配布、CAT machine Gatesは未実施。テストは既存実装の回帰証拠であり、PR #10のソースがdevelopに入るまで単独で統合しない。

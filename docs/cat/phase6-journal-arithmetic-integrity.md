---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.journal-arithmetic
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 永続発行予約の算術整合性

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.journal | checksummed journal replayとdenomination×coinCount |
| simplecoins.grant | 異常時の発行停止と正常予約の継続 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal.replay.arithmetic-overflow | checksummedなtyped grantのdenomination×coinCountがsigned 64-bit範囲を超えるとき | 再生をchecked IOExceptionで拒否し、ArithmeticExceptionを外へ漏らさず部分復旧journalを返さない | SC-JOURNAL-ARITH-01 |
| journal.replay.no-issue | 永続レコードの意味上の算術整合が不正なとき | その状態から新しい発行予約を許可しない | SC-JOURNAL-ARITH-01 |
| journal.replay.valid | 正常なtyped予約を再起動後に再生するとき | denominationとcoinCountを保持し、従来のsequence/receipt意味論を変えない | SC-JOURNAL-ARITH-02 |

## 境界と状態

既存SC-JOURNAL-ARITH-01/02を意味変更せず構造化したTASK-20261101-005のCAT Draft。例示入力はcoinCount=Long.MAX_VALUE、denomination=10000。変更対象は永続再生のエラー分類であり、正常なtyped予約・sequence・receiptの意味を変更しない。正式CAT Process/PI/TCE・機械Gateはnot-run。Confirmed・マージ・リリースを意味しない。

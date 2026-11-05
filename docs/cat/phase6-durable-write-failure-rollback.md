---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.durable-write-rollback
status: draft
process: simplecoins.journal.durable-write
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Durable write rollback observation

TASK-20261101-005. Code-to-spec Draft, **not Confirmed**.
Source: `DurableFile.java` Git blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8` on `develop`.

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.journal.durable-write-rollback | `DurableFile.java` の既存コードにおける書込み失敗・ロールバックのDraft観測 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| DURABLE-ROLLBACK-01 | 有効recordの置換時にnull payloadを渡す | エラーを返し、従来の読取可能なrecordを維持して一時ファイルを除去する | 既存JUnit回帰（本書ではテストID未特定） |
| DURABLE-ROLLBACK-02 | 書込先が内容を持つdirectory | directoryと内容を削除せず失敗し、一時ファイルも残さない | 既存JUnit回帰（本書ではテストID未特定） |
| DURABLE-ROLLBACK-03 | 成功したwrite後に呼出し元のbyte arrayを変更 | 保存されたchecksummed payloadは変化しない | 既存JUnit回帰（本書ではテストID未特定） |

These are observations of the existing implementation on a Linux filesystem, not a new approved journal design. Passing JUnit tests are scoped regression evidence only. CAT normative gates, full settlement, restart/crash recovery and Paper integration remain unverified.

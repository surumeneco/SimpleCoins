---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.ban-decision-evidence-matrix
status: draft
process: simplecoins.owner-ban-evidence
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BAN Decisionの組合せ検証（CAT Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.ban-evidence | `BanEvidence.evaluate` の必要ソース集合とBANNED/CLEAR/UNKNOWN集約 |
| simplecoins.ban-decision | 集約結果を保持するtyped Decision境界 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| BAN-DEC-01 | 宣言済み3ソースの全27組合せ | BANNEDが一つでもあればBANNED、なければUNKNOWN優先、全CLEARのみCLEAR | BAN-MATRIX-01 |
| BAN-DEC-02 | 必須ソースが1つ欠落 | BANNEDの証拠がなければUNKNOWN、存在すればBANNED | BAN-MATRIX-02 |
| BAN-DEC-03 | IDが大文字・空白で表記揺れする | 同一の必須ソースとして正規化し、二重登録を拒否する | BAN-MATRIX-03 |
| BAN-DEC-04 | 未宣言のソースやnullの証拠が混入 | IOExceptionでfail-closedし、Decisionを生成しない | BAN-MATRIX-04 |
| BAN-DEC-05 | 3つの証拠の列挙順が変わる | BAN/UNKNOWN/CLEARの決定は入力順に依存しない | BAN-MATRIX-05 |

## 出典と適用範囲
SimpleCoins `develop` commit `a6005ffa5ed1bb1438c3790da8633205507d40db`、`BanEvidence.java` blob `0e3f29c2a85fca1d711a928b9db7637193805299` を検証対象とする。これは既存純Javaコードの**verification** Workであり、BAN/IP BANプラグインの検出、IPからUUIDへの対応付け、Bukkit APIの実行、完全なprovider宣言の証明を行わない。呼出元が完全な必須ソース集合を宣言したという前提自体は、このクラス単独では検証できない。

CAT Draft構文lint・JUnit実行結果を、正式CAT Process/PI/TCE Gateと区別する。DraftをConfirmedに昇格せず、実装の変更やPaper実機の所有者判定完了を主張しない。

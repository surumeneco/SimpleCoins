---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.issuer-blankness
status: candidate
process: simplecoins.phase7
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/d9b90a78df491af266651f187ea03e48934cae94/docs/verification/verify-issuance-journal.py
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7 — 発行台帳の発行者名とJava/Python空白判定（code-to-spec Candidate）

## Observation（固定ソースで観測）

- Java `GrantLedger.Grant` は発行者を `issuer.isBlank()` で検査する。Pythonの読取専用診断器 `_grant` は `not issuer.strip()` で検査する。両述語は同値ではない。
- JDK 21 の `String.isBlank()` では U+00A0 / U+0085 / U+2007 / U+202F の単一文字列はいずれも非blank、Python 3.13 の `str.strip()` はすべて空文字列にする。U+2003、ASCII空白、TABは両方でblank。
- PR #117 HEAD `d243e3db94b128880284f810d8694b848113d470` の実Java `GrantLedger` writer fixtureはU+00A0の発行者を保存できたが、Python診断器は `InvalidJournal("invalid grant fields")` を返した。Actions `41479846914` では19テスト中5件がこの互換性差分に由来する期待Red。これは診断器の実行成功・Greenを意味しない。

## Candidate（承認前の提案）

Java writerが受理・保存できる正規SCG1/SCG2レコードを、外部診断器が言語ごとの空白述語の違いだけで不正と判定しない。チェックサム、UUID、金額、長さ、改変検出、読み取り専用・匿名化の既存境界は維持する。

| ベクトル | 入力 | 期待する互換性 |
| --- | --- | --- |
| ISS-DIAG-08A | 発行者がU+00A0、U+0085、U+2007、U+202Fのいずれかのみ | Java writerと診断器の受理が一致 |
| ISS-DIAG-08B | 発行者がU+2003、ASCII空白、TAB、U+001Cのみ | Java writerと診断器の拒否が一致 |
| ISS-DIAG-08C | 実Java writerが保存したU+00A0のSCG2とhead | 実バイトの診断結果を比較する |

## Question（規範決定が必要）

- Java側の現行 `String.isBlank()` を将来の確定仕様として維持するか、発行者の入力制約自体を別途変更するか。後者は保存形式と互換性に影響するため、このCandidateだけで決定しない。
- Python診断器の修正・Greenは、適用すべき正規CAT Gateと意味論上の承認後に実施する。診断器の結果はPaperのオフラインgive、再起動時一回性、JE/BE/Nexo実機受入を証明しない。

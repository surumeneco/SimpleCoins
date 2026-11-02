---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.plugin-jar-inventory-reconciliation
status: draft
process: simplecoins.phase5.external-source-inventory
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/pull/105
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: plugin JAR filesystem inventory reconciliation (CAT Draft)

## 対象と証拠境界

PR #105 の `schema_version: 1`, `scope: filesystem-root-jars-only` JSON 2件を、固定された基準スキャンと新しい観測スキャンとして**読み取り専用**で比較する。双方の JAR 名・バイト長・SHA-256・ルート plugin descriptor を照合する。**一致してもPaperのロード済みPlugin、BANソース列挙、UUID/IP対応、WWC/WWW/WCL所有権、実機稼働状態の証明にはならない。** この独立した検証補助ツールはSimpleCoinsの決算処理に入力しない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| JAR-DIFF-01 | 2つの完全なファイルシステムスキャンが同一 | 差分なしと報告するが、loaded/BAN completeness/settlement はすべて未証明 | DIFF-01 |
| JAR-DIFF-02 | 追加・欠落・同名JARの内容/サイズ/descriptor変更 | 名前順で決定論的に差分を列挙し非ゼロ終了 | DIFF-02, DIFF-03 |
| JAR-DIFF-03 | `scan_complete:false` または `errors` が非空 | 比較不能として fail-closed、空の一覧を一致扱いしない | DIFF-04 |
| JAR-DIFF-04 | 不正schema、重複名（大小文字差を含む）、不正SHA/サイズ/descriptor | 比較不能として fail-closed | DIFF-05, DIFF-06 |
| JAR-DIFF-05 | 読込不可、壊れたJSON、基準・観測の欠落 | エラー理由を出し非ゼロ終了、状態を推測しない | DIFF-07 |
| JAR-DIFF-06 | 出力形式 | 安定したJSON、証明されていない範囲を明示する | DIFF-08 |

## Process / PI

入力の信頼境界は **2つのスキャンJSONの構造・完全性の検証**。両者の`entries`と`errors`は整合していなければならない。比較はJARファイルの観測差分に限定し、未知のPluginが無い・BANがCLEAR・経済的所有者がFREEといった判断へ昇格させない。

## Test model / TCE

DIFF-01: 同一の完全なスキャン。DIFF-02: JAR追加・削除。DIFF-03: 同名JARのハッシュ・サイズ・descriptor変化。DIFF-04: 不完全スキャン・エラー。DIFF-05: schema/scope/状態フラグ不正。DIFF-06: 重複名・不正フィールド。DIFF-07: 入力欠落・壊れたJSON。DIFF-08: 安定順序と未証明の明示。

**Status: Draft, not Confirmed.** 正規CAT Process/PI/TCEの承認と機械Gateは未実施。本ツールは独立した非本番の検証補助候補であり、正式Gate前に所有権決算へ組み込まない。

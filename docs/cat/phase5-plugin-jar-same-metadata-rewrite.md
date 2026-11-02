---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.plugin-jar-same-metadata-rewrite
status: draft
process: simplecoins.phase5.external-source-inventory
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/pull/105
  - docs/cat/phase5-plugin-jar-inventory-observation.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: 同一メタデータのJAR書換えと観測整合性（CAT Draft）

## 対象

PR #105の読取専用JAR列挙器がSHA-256計算後に同じファイル記述子でZIP descriptorを読む間、JAR内容が同じ長さの別の有効ZIPへ書き換えられ、mtimeを元へ戻された場合の観測境界。これは既存JAR-OBS-06（走査中の変更を完全な証拠としない）の未検証条件を具体化するcode-to-spec診断であり、所有者やBANの新しい確定規則ではない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| JAR-COH-01 | 同じinode、同じサイズ、復元されたmtimeでJARの内容がSHA計算とdescriptor読取の間に変わる | SHAとdescriptorが異なるバイト版を表す観測を完全と認定しない | test_same_size_restored_mtime_rewrite_is_not_complete |
| JAR-COH-02 | 変更がない同じJARを読んだ | SHA-256とdescriptorを一貫した観測として報告する | test_unchanged_same_size_jar_is_complete |

## Process / PI

既存の `fstat` 比較は `st_dev`、`st_ino`、`st_size`、`st_mtime_ns` を使う。同じinodeを同じ長さで上書きしmtimeを復元できると、前後のメタデータ一致だけではバイト同一性を証明できない。SHA計算とZIP解析を単一FDで行うことはパス差替え対策だが、FDが指すinodeの内容変更を防止しない。修正方式（単一の不変バイトスナップショット、再読取照合等）の採否は未決定。

## Test model / TCE

- COH-00: 一時ディレクトリ内の正常JARを変更せずに読んだ場合は、正しいSHA・descriptorを返す。
- COH-01: 同じバイト長で異なる有効ZIPの内容へ同一inodeを上書きし、mtimeを元に復元する。テストは書換えが実行されたこと、旧新SHAが異なること、前後のsize/mtimeが一致することを検証する。完全な観測として旧SHAを採用してはならない。
- 本番JARや実機Paperは操作しない。期待Redは観測の不整合だけを示し、実機攻撃や外部BAN証拠の不正を証明しない。

**Status: Draft, not Confirmed.** 正式CAT Process/PI/TCE確認、機械Gate、Green、Paper実機検証は未実施。

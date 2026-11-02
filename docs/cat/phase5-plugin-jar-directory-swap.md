---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.plugin-jar-directory-swap
status: draft
process: simplecoins.phase5.external-source-inventory
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/pull/105
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: JAR inventory ancestor-directory race (CAT Draft / expected Red)

## 対象

既存の読み取り専用JAR列挙器が`plugins/`を検証した後、列挙時または`os.open`直前にその親ディレクトリが別ディレクトリへのsymlinkに置換された場合の観測境界。JARファイル自身に`O_NOFOLLOW`を指定しても、途中の親パス要素に対するsymlink追跡を禁止できるとは限らない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| JAR-DIR-01 | 正常な`plugins/`がJAR列挙直前に別ディレクトリへのsymlinkに置換 | 別ディレクトリのJARを完全な観測として報告しない | DIR-RACE-01 |
| JAR-DIR-02 | JAR列挙後、`os.open`直前に同じ親ディレクトリが置換 | `O_NOFOLLOW`の最終パス要素保護だけを信頼せず、外部JARを正規観測として報告しない | DIR-RACE-02 |
| JAR-DIR-03 | 観測完全性が未証明 | `scan_complete:true`を根拠にBANソース完全性やPaperロード状態を証明しない | DIR-RACE-01, DIR-RACE-02 |

## Process / PI

このテストは**既存コードから観測した失敗の再現**であり、製品の所有者決算やPaper実機での受入を代替しない。現在の実装は開始時の`is_symlink()`検査と最終JARに対する`O_NOFOLLOW`を行うが、親ディレクトリの継続的な同一性を保証していない。安全な対策候補は、ディレクトリFDを保持し相対パスで開く方式等だが、採否・正規CAT Gate・Greenは未決定。

## Test model / TCE

- DIR-RACE-00: 置換のないディレクトリでは、元のJARだけをハッシュし完全なファイルシステム観測を返す（正常系対照）。
- DIR-RACE-01: 列挙直前に`plugins/`を別ディレクトリへ置換したとき、外部JARのハッシュを完全な観測として返してはならない。
- DIR-RACE-02: 列挙後・JAR open直前の置換でも同様に拒否する。
- どちらも一時ディレクトリ内でのみ実行し、本番ファイルは操作しない。

**Status: Draft, not Confirmed.** 現行PR #105の実装に対するRed回帰試験。実装修正・正式CAT Gate・Paper実機受入は未実施。

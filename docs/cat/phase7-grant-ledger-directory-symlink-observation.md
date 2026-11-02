---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.grant-ledger-ancestor-symlink-observation
status: candidate
process: simplecoins.admin
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: 発行台帳ディレクトリのシンボリックリンク境界（code-to-spec Candidate）

## 対象と観測

- 対象は `GrantLedger(Path directory)` と `DurableFile.write` の連携。固定元ソースはPR #2の `01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153`、`GrantLedger.java` Git blob `df93d2c042cf83601a6c9d7492601579a3bfe4fc`。
- 現行コンストラクタは `Files.createDirectories(directory)` の後に `directory.resolve("head")` を作成する。最終要素 `directory` 自体が既存の外部ディレクトリへのシンボリックリンクの場合、Java NIOは通常リンク先をたどる。
- `head` と `*.grant` の最終要素を保護しても、親ディレクトリがリンクなら書込み先が管理対象外になり得る。Pythonのオフライン診断器に関するPR #120とは別の、**Javaの実writer**の境界である。

## 独立した回帰ベクトル

| ID | 条件 | 期待する安全境界 | 証拠の段階 |
| --- | --- | --- | --- |
| LEDGER-DIR-01 | 通常の実ディレクトリ | 初期化と予約がそのディレクトリ内に閉じる | 正常系の直接テスト |
| LEDGER-DIR-02 | `issuance` 自体が管理外の既存ディレクトリへのsymlink | 初期化を拒否し、リンク先に `head` / `*.grant` を作成しない | 現行実装に対する期待Redテスト |

## 制約・未決

本書は既存コードから復元した**Candidate**であり、ユーザー承認済みのConfirmedではない。symlinkの可否は環境依存のため、非対応OSではテストをskipし成功扱いしない。Redの観測は実装欠陥の候補を示すが、修正方法を承認するものではない。実装前の正式CAT確認、必要な機械Gate、最小Green、回帰、Paper実機受入は別工程。現段階では製品Javaを変更しない。

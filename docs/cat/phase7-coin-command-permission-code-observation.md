---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.coin-command-permission-observation
status: candidate
process: simplecoins.admin
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/CoinCommand.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: CoinCommand 権限境界の実装観測（code-to-spec Candidate）

## 対象と証拠

固定リビジョン `01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153` の `CoinCommand.java`（Git blob `ad45f0e630f3ddf1446983b58b1ddba2a09201c4`）に観測された既存動作のみを記録する。これは利用者承認済みの規範仕様ではなく、現在のコードから復元した Candidate である。

## 観測された分岐

| ID | 既存コードの条件 | 観測される動作 | 検証 |
| --- | --- | --- | --- |
| CMD-PERM-OBS-01 | `onCommand` の呼出元が `simplecoins.admin` を持たない | 引数解釈・対象解決より先に拒否メッセージを返し、処理を終了する | 不許可送信者で金額give、財布give、無効引数を実行 |
| CMD-PERM-OBS-02 | `onTabComplete` の呼出元が同権限を持たない | 候補生成・オフラインプレイヤー列挙より先に空候補を返す | 不許可送信者で複数の補完段階を実行 |
| CMD-PERM-OBS-03 | 権限判定が拒否した場合 | `CoinItems`・`OfflineGrants`・`Bukkit` の実体を必要としない | 依存インスタンスを与えず早期終了を検証 |

## 限界と未決事項

このCandidateは**既存の権限チェック順序**のみを検証する。許可された管理者のgive、実際のPaper permission設定、オフライン予約、台帳の一回性、Nexo、JE/BE配布は証明しない。拒否メッセージの具体文言、権限ノードの正当性、許可済み操作の受入仕様は規範判断の対象であり、実装観測から自動昇格しない。

CAT machine `process-closure` / `model-conformance` / `implementation-conformance` は未実行。CandidateをConfirmedに変更せず、製品コードを変更しない。

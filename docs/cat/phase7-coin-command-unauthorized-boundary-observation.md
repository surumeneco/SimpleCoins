---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.coin-command-unauthorized-boundary-observation
status: candidate
process: simplecoins.admin
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/CoinCommand.java
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: 権限を持たない CoinCommand 送信者の既存境界（code-to-spec Candidate）

## 対象と観測範囲

現行 `CoinCommand`（Git blob `ad45f0e630f3ddf1446983b58b1ddba2a09201c4`）の `simplecoins.admin` 権限を持たない送信者の既存動作を記録する。既存の許可済み構文分岐Candidateとは独立した、無権限分岐のみの回帰観測である。これは新しい規範仕様の承認ではない。

| ID | 条件 | 現行ソースの観測 | 検証 |
| --- | --- | --- | --- |
| CMD-DENY-01 | 無権限送信者が通常のgiveを実行 | `No permission`を返し、発行予約・対象解決に進まない | 3引数の通常give、依存なしの監視用送信者 |
| CMD-DENY-02 | 無権限送信者が構文不正・追加引数のgiveを実行 | 構文・プレイヤー情報を返す前に同じ拒否へ進む | 0引数、不正verb、4引数を含む複数形 |
| CMD-DENY-03 | 無権限送信者が補完を要求 | 補完候補を返さず、オフラインプレイヤー列挙を呼ばない | 第1〜第4引数で空リストを確認 |
| CMD-DENY-04 | 上記の拒否分岐 | `CoinItems`・`OfflineGrants`・Bukkitの実体を必要としない | null依存と未期待呼出しで失敗するProxy |

## 限界・未実施

本Candidateは既存ソースの分岐観測であり、正式な権限プラグイン連携・Paper実機の権限設定、giveの成功、一回性、JE/BE/Nexo配布、管理照会の新規実装を検証しない。CAT規範確認、process-closure、model-conformance、implementation-conformance、Paper実機検証はnot-run。成功してもTASK-20261101-006全体をdoneにしない。

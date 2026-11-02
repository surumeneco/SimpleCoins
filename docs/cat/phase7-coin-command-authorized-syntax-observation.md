---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.coin-command-authorized-syntax-observation
status: candidate
process: simplecoins.admin
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/50bfb9eba4d3c26fc6963adacc7598ba4d37e335/src/main/java/co/surumene/simplecoins/CoinCommand.java
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: 許可済み CoinCommand の構文境界（code-to-spec Candidate）

## 対象・根拠

既存 `CoinCommand`（Git blob `ad45f0e630f3ddf1446983b58b1ddba2a09201c4`）の、`simplecoins.admin` を持つ送信者について、Bukkit/OfflineGrants/CoinItemsに依存しない分岐だけを観測する。これは規範仕様の採用や実装変更ではない。

| ID | 既存分岐 | 観測される結果 | 検証 |
| --- | --- | --- | --- |
| CMD-AUTH-SYNTAX-01 | 引数が3個未満、または先頭が `give` でない | Usageを送信して正常終了し、対象選択・発行予約を呼ばない | 0/1/2引数と不正verbを許可済み送信者で実行 |
| CMD-AUTH-SYNTAX-02 | 補完の第1引数が `give` のcase-insensitiveなprefix | `give` だけを返す | 空・小文字・大文字prefix |
| CMD-AUTH-SYNTAX-03 | 補完の第1引数が非prefix | 空候補を返す | 非prefix入力 |
| CMD-AUTH-SYNTAX-04 | 上記分岐 | `CoinItems` / `OfflineGrants` / Bukkitの実体を必要としない | null依存と監視用CommandSenderで確認 |

## 限界

本Candidateは現行ソースの分岐観測であり、利用者が承認した仕様・Confirmedではない。許可済みの実give、オフライン予約、再起動時一回性、Nexo/JE/BE/Paperの実機動作、権限設定、発行量の正当性は未検証。正式CATのprocess-closure / model-conformance / implementation-conformanceはnot-runであり、本テストの成功を代替証拠にしない。

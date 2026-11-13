---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.cross-player-wallet-uuid
status: candidate
process: simplecoins.settlement.saved-player-reconciliation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/326
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# 複数プレイヤーにまたがる保存済み財布UUID照合 Candidate

02Bは同じ現物の二重計上禁止と正本全件走査を要求する。PR #326では単一playerdata内の財布UUID重複を拒否するが、異なるUUIDのプレイヤーA/Bの保存ファイルに複製された同一財布UUIDが存在しても、それぞれを独立に抽出すると二重計上される。

## 対象

| Ref | Aspect |
| --- | --- |
| player.scan | SettlementScanRunner.stageの全playerdataソース |
| identity | StoredItemValue.value内のcanonical wallet UUID |
| settlement | 重複していた場合、全件確定前にIOException |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| player-dup-01 | 2プレイヤーの異なる保存ファイルに同じ財布UUID | 必須ソース集合全体で財布同一性が衝突したら決算を失敗させる | 同じ財布UUIDを含む2 GZIP playerdata fixture |
| player-dup-02 | 2プレイヤーに別財布UUID | 2プレイヤー双方の保有額を従来通り計算する | 別ID2 GZIP fixture |
| player-dup-03 | 保存済みワールドコンテナとプレイヤー、ライブPaperデータで財布ID重複 | この変更のみでは全源統一を証明できない | 後続G0全源同一世代統合 |

## 境界

- ひとつのSettlementScanRunner.stage呼び出しで処理する**保存済みplayerdata集合**のみを同一性スコープとする。World region/entity保存データやPaperメモリの全源統合を受入済みとみなさない。
- 不正入力で一部IDが登録された場合でも、staging全体を破棄し、部分計算を公開しない。
- formal CAT Gate・実機JE/BEは別判定。Candidateは未承認、未リリース。
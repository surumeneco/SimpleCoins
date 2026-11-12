---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.cross-saved-live-wallet-identity
status: candidate
process: simplecoins.settlement.wallet-physical-identity
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/340
refs: []
semantic_contract: markdown-v2
language: ja
---

# 保存NBT・ライブ資産を跨ぐ財布UUIDの一意性 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| saved-stage | `SettlementScanRunner.stage` が収集する保存済み財布のUUID |
| live-universe | `PaperLiveUniverseAssets.scan` が収集するライブ財布のUUID |
| shared-generation | 同一の暫定決算世代で両走査へ受け渡す一つの可変Set |
| saved-live-partition | 保存・ライブの同一実体を選択する独立した閉包規則 |

## 観測

保存NBT側は`SettlementScanRunner.stage`内部のSetで重複を検出し、Paper側の`PaperLiveUniverseAssets.scan`は別のSetで重複を検出する。両走査に同じ財布UUIDが現れるとどちらの系統も単独では検出できない。02Bでは同一の実在価値は一つの主体へ一度だけ計上することを要求している。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| cross-wallet-01 | 同一UUIDを含む保存playerdataを読む前にライブの財布UUIDが登録された | IOExceptionで保存ステージング全体を拒否する | 圧縮playerdata fixture |
| cross-wallet-02 | 保存playerdataの財布UUIDが既に登録されたあとライブを読む | Paperライブ走査が同一UUIDを検出し拒否する | Paper World実財布 |
| cross-wallet-03 | 保存とライブの財布UUIDが異なる | 両入力を正しく受理し、Setに2件保持する | 分離したUUIDの検証 |
| cross-wallet-04 | 既存の単独呼出し | 新引数を要求せず従来の局所Setで処理する | 既存全回帰 |
| cross-wallet-05 | いずれかの入力読取・所有者判定・世代同期が失敗 | Setと部分的な集計を破棄し、決算確定・入室開放しない | エラー時の契約 |
| cross-wallet-06 | 保存ライブの両方の財布を読めた | 物理UUIDの同一性だけを検査し、同時点性・対象範囲・全件収支の完成とはしない | isCompleteSettlementSource=false |

## Boundary

これは複数データソースでの一意性を共有するためのAPI拡張である。呼出し順序・同一世代性・保存/ライブの選択・BlockLocker・BAN/WWC/WWW・仕訳再適用は決算Coordinatorが管理する。失敗後に共通Setを再利用してはならない。正式CAT Process/PI/TCEのConfirmedやGate合格は主張しない。

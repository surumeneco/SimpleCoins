---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-player-inventory
status: candidate
process: simplecoins.settlement.live-player-assets
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/331
refs: []
semantic_contract: markdown-v2
language: ja
---

# ログイン中のプレイヤー保存先に対するライブ資産抽出 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| player-owner | オンラインPlayerのUUID |
| live-storage | PlayerInventory storage contents の実物ItemStack |
| live-armor-extra | PlayerInventory armor/extraの実物ItemStack（収納と重複させない） |
| live-ender | EnderChestの27スロット |
| wallet-identity | 複数Player、コンテナ、Mob間で共有する物理財布UUID一意性 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| player-01 | プレイヤーに通常所持・防具・オフハンド・エンダーチェストの貨幣がある | ItemStackから正確な金額を読み取り、保存区画とスロットを重複なく報告する | JVM上のAPI存在＋実オンライン参加試験 |
| player-02 | 一つのPlayerInventoryのStorage/Armor/Extraが重なる | PlayerInventory.getContents()の別名を再集計せず、独立の三配列のみ走査 | 物理スロット件数・キー検査 |
| player-03 | 二つのプレイヤーまたは別の保管源に同一財布UUID | 共有集合で重複を拒否し、部分結果を公開しない | UUIDコピー／同一Generation試験 |
| player-04 | 非メインスレッド、オフライン、Slot上限・シリアライズ上限・破損PDC | fail-closedとして処理を中止する | Paper API boundary＋JUnit |
| player-05 | まだライブ/保存データの一世代整合、BAN情報と全保管先を確定していない | 入室許可・確定資産統計を公開せず、部分結果のまま保持 | isCompleteSettlementSource=false |

## Observation

`SavedPlayerAssets.extract`は`Inventory`・`EnderItems`・`equipment`を計数する。Paperの既存`PaperLiveItemValue.inventory`は指定Inventoryだけを合計し、プレイヤー特有のStorage/Armor/ExtraとEnderChestを一括して物理スロットに分けるAPIが未実装。ロード済みChunkはPlayer LivingEntityを明示的に除外するため、ライブPlayerは後段の独立ソースを必要とする。

## Boundary

本CandidateはライブPlayerの読取専用の技術実装であり、ログイン前／オフラインのplayerdata、ログアウト中の保存整合、読み取られたプレイヤーの現在BAN・playerdata存否、イベント世代、登録主体、保存/ライブ二重計上排除、通常仕訳とpublicationを実現しない。CAT Confirmedと各形式Gateは未達で、プレイヤー入室を解禁しない。既存Paper読み取りルールに従いメインスレッドでのみ呼び出す。

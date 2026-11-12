---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-paper-item-valuation
status: candidate
process: simplecoins.settlement.live-item
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/320
  - https://github.com/surumeneco/SimpleCoins/pull/322
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PaperライブItemStackの資産額抽出 Candidate

現行02Bでは、ロード済みのPaper状態を保存済みNBTより優先して同一実体を重複計上しない。本技術Candidateはその前提のうち**単体ItemStackを元の値のままNBTへ読み戻して金額化する層**に限定する。CAT Confirmedではなく、Paper実機での全件走査が完成したことを意味しない。

## 対象

| Ref | Aspect |
| --- | --- |
| live.item | Paperプライマリスレッドで実在するItemStackの保存形式 |
| saved.value | 既存StoredItemValueの通貨PDC・財布・入れ子の価値判定 |
| live.inventory | 指定Inventoryの全スロットに対する限定的な価値和 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| live-01 | 正規のPAPER貨幣9額面・財布をPaperで生成 | ItemStack.serializeAsBytesのGZIPまたは非圧縮NBTを復号し、保存側と同じ額を返す | Paper26.2 build134実API |
| live-02 | AIR/null・通常素材 | 価値0、通常のスロットは計上しない | 空スロット・通常紙 |
| live-03 | 不正PDC・モデル不一致・NBT破損 | 不明価値を0に丸めずIOExceptionとして呼出元へ伝える | 改ざん貨幣fixture |
| live-04 | スロット数・NBT圧縮長・展開長が上限外 | 走査を失敗させる | 不正予算の試験 |
| live-05 | Paper主スレッド外での読み出し | Bukkit API操作を拒否する | 契約上の例外 |
| live-06 | 既にロード済みのChunkと同じ場所のSavedデータが存在 | この単体抽出は二重計上の排除を主張しない | 後段でChunk・player所有の権威を決定 |

## 検証境界

- ここではライブ個別ItemStack/Inventoryの読み取りのみ。オンラインプレイヤー、ロード済みチャンク、Entity、BlockLocker、WWC/WWWの全候補走査や保存側との一意化・世代整合は別Work。
- Inventory#getContentsはスロット集合を返すだけで、異なるInventory間の重複や時間をまたぐ状態不変性を証明しない。
- 正式CAT Gate・全Phase統合・develop反映・JE/BE操作は未実施。
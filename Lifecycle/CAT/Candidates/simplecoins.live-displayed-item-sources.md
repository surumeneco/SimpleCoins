---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-displayed-item-sources
status: candidate
process: simplecoins.settlement.loaded-chunk-assets
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/329
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paper ItemFrame・ItemDisplayのライブ貨幣抽出 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| displayed-entity | 額縁・発光額縁・ItemDisplayの現行表示ItemStack |
| physical-source | World名とEntity UUIDからなる単一保持実体の位置 |
| shared-wallet-id | チャンク間で共有する財布ID識別集合 |
| scan-budget | 貨幣・空表示も含むスロット走査予算 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| display-01 | ロード済みチャンクのItemFrame・GlowItemFrameが貨幣を保持 | Paper実体の金額をUUID別に一回だけ取得 | JDK25コンパイル・公式Paper実体 |
| display-02 | ItemDisplayが貨幣・財布を表示 | 実ItemStackの金額と財布IDを同じ読取器で認識 | Paper26.2のItemDisplayプローブ |
| display-03 | 空表示または無貨幣の表示 | 金額0とし資産Entryを生成しないが、走査件数・スロット予算には含む | 空表示の回帰 |
| display-04 | 同じ財布UUIDを別のContainerや表示Entityも保持 | 二重計上せずIOExceptionで走査失敗 | wallet ID重複fixture |
| display-05 | サポート対象外Mob・保有者・Entityを含む | 読取完了を全件決算と解釈しない | isCompleteSettlementSource=false |

## Observation

PR #328の`PaperLoadedChunkAssets`はItem Entity以外のEntityをすべて未処理扱いとする。他方、保存NBT側には`StoredItemFrameAssets`と`StoredItemDisplayAssets`が既に存在し、両ソースが資産対象とされる。Paperライブ正本の抽出範囲に非対称性がある。

## Boundary

本Candidateは描画上の保持ItemStackを読み取る技術実装。BlockLocker・WCL/WWC/WWWの帰属、Mob装備・内部Inventory、未ロードチャンク・保存データとの重複排除、全世界の同一時点性、統計公開・入室許可までは証明しない。正規CAT Process Closure/Model/Implementation Conformanceと実プレイヤー操作は別検証。

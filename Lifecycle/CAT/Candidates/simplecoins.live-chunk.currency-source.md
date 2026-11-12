---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-chunk.currency-source
status: candidate
process: simplecoins.settlement.loaded-chunk-assets
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/325
  - https://github.com/surumeneco/SimpleCoins/pull/327
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paperロード済みチャンクのコンテナとItem Entity抽出 Candidate

02Bでは稼働中のロード済みチャンクのPaperメモリを保存NBTより優先し、全実体から資産を再構築する。本Candidateは、その一部であるロード済みContainerの物理スロットとドロップItem Entityを**安全に列挙する読み取り専用アダプター**を試す。CAT Confirmedや完全な全件決算ではない。

## 対象

| Ref | Aspect |
| --- | --- |
| chunk | 現在ロード中のPaper Chunk。新規チャンクの強制ロードは行わない |
| container | Chunk#getTileEntitiesで取得した実BlockStateのInventory |
| entity | Chunk#getEntitiesで取得したorg.bukkit.entity.Item |
| currency | CoinItemCodecに準拠したPDCとWalletItemCodecの価値 |
| location | ワールド名、Container座標＋スロット、またはEntity UUID |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| live-chunk-01 | ロード済み単一チャンクに貨幣ContainerとItem Entityが存在 | 価値と物理位置の一意なEntryを返す | Paper26.2にチェストとItem Entityを生成 |
| live-chunk-02 | ダブルチェストの片側を走査 | 各半分の27スロットのみ計上し、54スロット同時参照による重複を避ける | 二重化防止構造検査 |
| live-chunk-03 | 二つのスロットに重複財布UUID、又はUUID重複Entity | IOExceptionで走査を破棄 | 不正UUID・wallet fixture |
| live-chunk-04 | 主スレッド以外、アンロード中、過大なタイル・Entity・スロット数 | 走査を拒否する。Chunkを自動ロードしない | fail-closedの範囲検証 |
| live-chunk-05 | BlockLocker所有者・Mob装備・ItemDisplay等の未対応保持源 | 単一チャンクの出力で完全決算を名乗らず、後段の必須ソース統合へ残す | 現行契約での未対応明示 |

## 境界

- この独立スライスではチェストを含むInventoryHolder BlockStateとドロップItemのみが対象。プレイヤー、Mob、額縁、ItemDisplay、レジは未含有であり、完全なスナップショットにもP/F/D帰属にもなっていない。
- 読取側のスレッド制約はメインスレッド。多数のチャンクを一度に実行するスケジューラと世代不変性は別工程。
- saved/live同一チャンクの重複排除は後続G0で実施し、本変更だけで入室許可してはいけない。
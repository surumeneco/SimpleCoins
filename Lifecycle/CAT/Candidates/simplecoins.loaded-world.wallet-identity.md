---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.loaded-world.wallet-identity
status: candidate
process: simplecoins.settlement.loaded-world-assets
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/328
refs: []
semantic_contract: markdown-v2
language: ja
---

# ロード済みチャンク横断の財布UUID・ソース識別 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| loaded-world | 明示的に選択された一つのPaper Worldに属するロード済みChunkの集合 |
| live-chunk | PR #328の読取専用のContainer/Item Entity抽出 |
| wallet-identity | 各財布の安定UUIDを共有する走査内一意性検査 |
| physical-source | ブロック座標・スロットおよびItem Entity UUIDの識別 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| loaded-world-01 | 二つのロード済みチャンクに異なる財布UUIDがある | 各ソースの正の金額を一度ずつ計上する | Paper26.2でチャンク2つに財布を配置 |
| loaded-world-02 | 異なるロード済みチャンクに同一財布UUIDがある | どちらも合算した結果を返さずIOExceptionで中断する | 同一財布コピーの再現 |
| loaded-world-03 | チャンク配列・Entity UUID・物理スロットが重複、または予算を超える | 全体結果を返さず失敗する | 範囲異常・重複入力の検査 |
| loaded-world-04 | 一部のChunkが未ロード・ワールド不一致、主スレッド以外 | 自動ロードをせず全体結果を返さず失敗する | 主スレッド／存在条件の検査 |
| loaded-world-05 | ロード済みContainerとItem以外にも資産保有源がある | 部分結果を完全決算として公開しない | isCompleteSettlementSource=false |

## Observation

PR #328の`PaperLoadedChunkAssets.scan`はチャンク呼び出しごとに`walletIds`と`entityIds`を新規作成するため、同一UUIDの財布が異なるロード済みチャンクに配置された場合を検出できない。02A/02Bは全保管先を横断した単一資産一回計上と、未読取・不整合時の未確定扱いを要求する。

## Boundary

- これは現在ロード済みのコンテナと地面Itemを扱う**技術Candidate**であり、CAT Confirmed仕様・同一時点スナップショット証明ではない。
- 未ロードの保存チャンク、Mob・ItemFrame・Display・Player、BlockLocker/WWC/WWW、BAN/所有者帰属、通常仕訳との統合、保存/ライブ重複排除は未実装。決算受付は引き続き禁止する。
- 不整合時には走査内の共有集合を破棄する。途中で走査した部分結果を再利用しない。
- 一回のメインスレッド読取の上限を指定し、ロード済みの全チャンクを新たに強制ロードしない。

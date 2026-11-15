---
cat_version: '0.3'
kind: tce
id: simplecoins.world-source-discovery.tce.manifest
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/429
decision_ref: ''
refs:
  - simplecoins.world-source-discovery.pi.manifest
semantic_contract: markdown-v2
language: ja
---

# TCE: 共有プレイヤーデータ・ソース集合閉包 (Draft)

網羅要求: 部分

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| d0 | 発見 | `入力.実ルート有効 == 真 かつ 入力.プレイヤー正本一意 == 真 かつ 入力.孤立バックアップあり == 偽 かつ 入力.記号リンクあり == 偽 かつ 入力.探索予算以内 == 真` | 1 | `出力.Manifest受理 = 真; 出力.失敗通知 = 偽` |
| d1 | 発見 | `入力.実ルート有効 == 偽 または 入力.プレイヤー正本一意 == 偽 または 入力.孤立バックアップあり == 真 または 入力.記号リンクあり == 真 または 入力.探索予算以内 == 偽` | 1 | `出力.Manifest受理 = 偽; 出力.失敗通知 = 真` |
| r0 | 集合再検証 | `入力.ソース集合不変 == 真 かつ 入力.実ルート有効 == 真 かつ 入力.プレイヤー正本一意 == 真 かつ 入力.孤立バックアップあり == 偽 かつ 入力.記号リンクあり == 偽 かつ 入力.探索予算以内 == 真` | 1 | `出力.Manifest受理 = 真; 出力.失敗通知 = 偽` |
| r1 | 集合再検証 | `入力.ソース集合不変 == 偽 または 入力.実ルート有効 == 偽 または 入力.プレイヤー正本一意 == 偽 または 入力.孤立バックアップあり == 真 または 入力.記号リンクあり == 真 または 入力.探索予算以内 == 偽` | 1 | `出力.Manifest受理 = 偽; 出力.失敗通知 = 真` |

## エビデンスと留保
- 実Paper 26.2のDimension別`World#getWorldPath`に対し、現developは共有`players/data`をゼロ件とし、[#429](https://github.com/surumeneco/SimpleCoins/pull/429)は同じ実ルート由来の共有ディレクトリを別発見する。featureのJava独立再現とPaper smoke CIは成功し、利用者のCAT規範判断は未実施。
- `網羅要求: 部分` は意図的であり、同一会計世代や全ソース形態への完全閉包を意味しない。このDraftの構造合格を正式のModel Gate成功に読み替えない。

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-loaded-chunk-set-stability
status: candidate
process: simplecoins.settlement.paper-live-source-coverage
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/346
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paperロード済みChunk集合の走査前後同一性 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| world-chunks | Paper `World.getLoadedChunks()`のワールド別座標集合 |
| source-generation | 一回のLIVE走査が取得対象として固定した全Chunk |
| counted-assets | 一回の走査から得た保存先別貨幣明細 |

## 観測

現行`PaperLoadedWorldAssets.scan`は最初に`getLoadedChunks`を取得し、その配列だけを読取対象とする。走査中に別Chunkがロード・アンロードされても、最後に再照合しないため、増減した保管先を「存在しないもの」と扱う恐れがある。Paperのメインスレッド上でも他APIにより副次的な読み込みが発生する可能性を除外できない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| chunkset-01 | 走査開始と完了でロード済み座標集合が同じ | 走査が完了候補になれる | 集合一致テスト |
| chunkset-02 | 件数が同じだが別座標へ入れ替わった | 片方の変化を見落とさずIOException | 差替座標テスト |
| chunkset-03 | 新しいChunkが追加ロードされた | 走査した範囲と異なるためIOException | 増加テスト |
| chunkset-04 | 走査したChunkがアンロードされた | 途中計上結果を使用せずIOException | 減少テスト |
| chunkset-05 | 最終配列に重複座標・他World・未ロードChunkがある | 一致件数であっても拒否する | 不正ソースチェック |
| chunkset-06 | 走査前後の座標が一致する | 中間瞬間でのアンロード/再ロード、現物変更、他プラグイン変化、未ロードNBTとの時点整合性までは未証明 | admission閉維持 |

## Boundary

本候補はロード済みChunkの所属集合を固定する一歩であり、世界時間の完全凍結ではない。オンラインPlayer集合は既存の別検査で照合する。非同期全件決算との競合・ログの通常仕訳は引き続き別工程。CAT CandidateからConfirmedへの自動昇格は禁止。

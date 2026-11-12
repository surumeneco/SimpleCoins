---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-captured-worldscope-gating
status: candidate
process: simplecoins.settlement.live-source-scope
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/346
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paperライブ決算のワールド集計設定と正本選択の境界 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| selected-live-chunks | Paper単回走査で実際に捕捉されたワールド別Chunk |
| accounting-scope | config由来の`WorldScope`デフォルト・明示ON/OFFリスト |
| captured-wallets | 保存/ライブの財布UUID共有Set |
| candidate-stage | `PaperCapturedHybridSettlement.stageCandidate` |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| scope-01 | 捕捉したLIVE Chunkが集計OFFワールドに存在 | その資産を默って0Cへ捨てず、IOExceptionで候補を拒否する | Live 200C、WorldScope OFF |
| scope-02 | 捕捉したLIVE ChunkがONワールドに存在 | 通常通りSourceを登録し、所有者解決後に集計する | Live 200C、WorldScope ON |
| scope-03 | ワールドがOFFでもオンラインPlayerのworld非依存Inventoryがある | そのPlayer sourceを除外せず、正常に集計する | Player分離テスト |
| scope-04 | 集計ON/OFFリストに競合がある | `WorldScope`既存のデフォルトに従い、外側がその判定を勝手に上書きしない | scopeの契約テスト |
| scope-05 | 結合が成功 | 即時に原子的結果公開・入室許可へ進めない | fail-closed |

## Boundary

このチェックは入口の誤ったワールド集合指定を拒否する。正しい呼出し元は集計OFFワールドをPaper読取対象に含めず、ワールド非依存Playerは常時集計する。これは同一時点性、全保存データ・所有者・BAN、仕訳、正式CAT Gateを代替しない。CAT Candidate、Confirmedではない。

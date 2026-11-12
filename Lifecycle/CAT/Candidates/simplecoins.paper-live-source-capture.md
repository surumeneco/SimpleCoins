---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-live-source-capture
status: candidate
process: simplecoins.settlement.paper-live-inventory
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/343
refs: []
semantic_contract: markdown-v2
language: ja
---

# PaperライブChunk・Player決算入力のソース別捕捉 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| paper-chunk | `PaperLoadedChunkAssets.Slice` の値・world・chunkX/Z |
| paper-online | `PaperLivePlayerAssets.OnlineSet` の保管先UUIDと金額 |
| observed | `PaperLiveUniverseAssets.Report` の従来集計と同じ読取から派生したソース別記録 |
| hybrid-stager | `HybridLiveSourceStage` の`ChunkSource`/`PlayerSource` 入力 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| paper-source-01 | ライブワールドを走査する | 実際に読んだ各Chunkごとにsource IDと金額エントリを保持する | Paper実Chunk fixture |
| paper-source-02 | オンラインPlayerを走査する | Worldとは独立にPlayer UUIDごとのsourceを生成する | Paper実Player fixture |
| paper-source-03 | 貨幣が0Cの空Chunk/Playerを走査する | 金額エントリなしの必須ソースとして残す | 空ソース契約 |
| paper-source-04 | 従来の集計とソース別明細が不一致 | value総額・Chunk件数・Player件数・source ID重複を拒否する | synthetic Report回帰 |
| paper-source-05 | 任意のワールド横断で同一財布UUIDを複製する | 従来通り共有Setで重複を拒否する | 既存Paper統合 |
| paper-source-06 | 走査が成功 | 読取したことだけを示し、Worldの静止・Owner/BAN・保存NBT全件・仕訳整合は保証しない | admission は常に閉 |
| paper-source-07 | Chunk/Player別Source総額は一致するが、どちらかの額が移された | 各Reportの実物physicalKeyと金額を**sourceごとに照合**し、総額一致のみでは成功扱いしない | Chunk 100→75/Player 50→75の入替テスト |
| paper-source-08 | ソースのphysicalKeyだけを別のキーに差替 | 金額が同じでも現物Slot不一致として拒否する | source/Reportの厳密なキー比較 |

## Boundary

既存`PaperLiveUniverseAssets.scan`の公開結果は維持し、ソース別の明細を新しい`scanWithSources`で追加する。二重走査・二重計上を避けるため、一回のmain thread走査で同時に集める。各Chunk内の明細は追加の仮定なしに実物`Slice`から作る。現段階ではCAT Candidateであり、正式Gate通過や入室解禁を意味しない。

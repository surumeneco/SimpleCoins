---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-hybrid-stage-composition
status: candidate
process: simplecoins.settlement.paper-hybrid-source-stage
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/342
  - https://github.com/surumeneco/SimpleCoins/pull/343
  - https://github.com/surumeneco/SimpleCoins/pull/344
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paperライブ資産と保存NBTの未確定決算結合 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| paper-capture | `PaperLiveUniverseAssets.Observed` の一回の走査結果 |
| source-closure | `SavedLiveSourcePartition` のChunk/Player集合 |
| saved-nbt | `WorldStorageDiscovery.Manifest`、`SettlementScanRunner` |
| live-owners | `HybridLiveSourceStage.Classifier` による確定帰属 |
| shared-wallets | 同一Paper走査の財布UUID集合を保存NBT走査へ引き渡す |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| hybrid-compose-01 | 事前に一回取得したPaper Chunk・Player source | source集合からPartitonを一意に形成し、全部live観測済みと印す | 2ソース・正本分配 |
| hybrid-compose-02 | LIVE資産が正しく帰属した | 保存ディスク値とLIVE資産を**同じ**Accumulatorへ投入し、欠損なければ未公開Result候補を返す | 700C Chunk+500C Player fixture |
| hybrid-compose-03 | 予定外/重複LIVE源 | IOExceptionで全候補を拒否し、部分結果を公開しない | 多重source |
| hybrid-compose-04 | LIVE walletと保存財布が複製 | 一個のSetを走査前から引継ぎ、保存ステージで発見した際にIOExceptionを返す | 合致UUIDを含む保存fixture |
| hybrid-compose-05 | 保存world scopeがLIVE worldを対象外にする | ステージングを拒否し、黙って例外世界の貨幣を無視しない | WorldScope違反 |
| hybrid-compose-06 | 値の読取と所有者分類だけが成功した | 同一時点性/完全な外部Owner/BAN/WCL/WWC/WWW/仕訳を証明しない | guard閉維持 |

## 安全境界

このクラスは候補`Staged`を組み立てる**接続器**である。入力`Observed`がPaper main-threadの単回走査から取得されたか、途中でメモリと保存データが変動していないか、所有者分類が外部プラグインの契約に従うかは呼出者が別途検証する。デフォルトの所有者推測・0C補間・入室許可・原子的結果の本番公開を行わない。CAT Candidateであり正式Gateの合格を主張しない。

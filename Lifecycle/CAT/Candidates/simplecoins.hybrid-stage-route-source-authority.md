---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.hybrid-stage-route-source-authority
status: candidate
process: simplecoins.settlement.saved-live-source-routing
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/338
  - https://github.com/surumeneco/SimpleCoins/pull/341
refs: []
semantic_contract: markdown-v2
language: ja
---

# 全件保存ステージングでの保存/ライブ正本ルーティング Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| manifest | 走査対象全ワールドのregion/entitiesおよびplayerdata |
| live-plan | 同一世代の選択済みLoaded ChunkとOnline Player UUID |
| saved-assets | Free・Personal・Deadへステージする保存中の物理貨幣 |
| identity-context | 全保存・ライブ資産間で共有する財布UUID集合 |

## 受入規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| hybrid-01 | ワールドのロード済みChunkが保存済region/entitiesにも存在 | 保存ファイルの物理/内容検証を実行するが保存貨幣をステージしない | 既知コンテナを含む本物のMCA fixture |
| hybrid-02 | オンラインPlayer UUIDに対応する保存playerdataがある | GZIP playerdataを読み内容検証するが保存貨幣をステージしない | 既知額面のplayerdata fixture |
| hybrid-03 | 別の未ロードChunkやオフラインPlayerdataがある | 正本として保存貨幣をステージする | 個人とfreeの両経路 |
| hybrid-04 | 選択したLIVE Chunk/Playerを事前読取していない | `IOException`で全保存ステージングを拒否する | source closure regression |
| hybrid-05 | 保存側とライブ側で同じ財布UUIDを持つ | 単一財布UUID集合で保存側を拒否する | 圧縮NBT・共通Set回帰 |
| hybrid-06 | 旧ディスク専用呼出し | 従来と同じ仕様・戻り値、暗黙のLIVE例外なし | 既存テスト |
| hybrid-07 | 正本選択のみ成功 | ライブ実体金額・所有者・BAN・仕訳・同時点性はまだ検証されない | 入室許可を開かない |

## 注意事項

この候補は別クラスで保持されていた`SavedLiveSourcePartition`を`SettlementScanRunner.stage`まで接続する。選択済みLIVEソースは本メソッドの外で**実際に検証済み**でなければならず、ただ`liveChunk`/`livePlayer`を呼ぶ行為は証拠にならない。メモリ内資産を`SettlementAccumulator`へ同じ正本世代として加える工程は未接続であり、この段階で完全決算・入室開放を許可しない。

本ArtifactはCAT Candidate。Confirmed仕様・Process Closure・Model Conformance・Implementation Conformanceへの自動昇格をしない。

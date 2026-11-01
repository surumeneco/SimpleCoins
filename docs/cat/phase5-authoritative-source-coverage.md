---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.authoritative-source-coverage
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs:
  - simplecoins.snapshot.source-set-closure
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 権威保存sourceの網羅性（CAT Draft）

## 目的と既存契約との境界

このDraftは、**見つけたファイルを全て読めたこと**と、**本来読むべき保存sourceが全て列挙されたこと**を区別する。前者の走査中変更検出と前後source集合一致は `phase5-source-set-closure.md` が扱う。後者は、走査対象のworld・dimension・playerdataの権威一覧をcallerがどう証明するかという別の受入条件である。既存の `WorldStorageDiscovery.discover` はcallerが渡したrootだけを探索するため、callerがrootを一つ欠落させても、それだけでは検出できない。

## 既存の決定済み要求（02B・実装計画G0/Phase 5からの導出）

- 全件決算は選択された対象worldの既存terrain BlockEntity・保存Entityと、world非依存playerdata（インベントリ・装備・エンダーチェスト）を含める。WorldScopeで除外したworldのworld固有sourceは除外し、playerdataを除外しない。
- Minecraft 26.xのdimension別 `region` / `entities`、`players/data`、従来の配置、Multiverseの実際のworld rootを考慮する。保存形式を一つの固定パスに限定しない。
- 読めない・未知・欠損した権威sourceを「資産0」として成功確定しない。同一の物理資産を異なる保存経路・メモリ経路から二重計上しない。部分的な走査結果を決算統計へ公開しない。

## 受入に必要な証拠と責務

| ID | 境界 | 要求される証拠 | 未確定なら |
| --- | --- | --- | --- |
| coverage.world-inventory | Paper/Multiverseから渡されるworld集合 | 決算対象の全worldの権威root一覧とWorldScopeの適用結果 | 保存走査の完了証明に使わない |
| coverage.dimension-layout | 各対象worldのdimension別保存領域 | 有効なlegacy/26.x保存配置とterrain/entityのsource種類を判別できること | 未認識領域を空とみなさない |
| coverage.playerdata | world非依存のプレイヤー保存領域 | 現行playerdata正本の所在とUUIDの一意性、複数候補の衝突判定 | 任意の一方を選ばない |
| coverage.source-read | manifestに含めた各保存source | 有界読取、NBT/外部sidecarの読取成功、同一物理sourceの重複排除 | 全体Reportを成功にしない |
| coverage.loaded-vs-disk | ロード済み状態との境界 | 同じ物理保有資産を一度だけ計上する時点整合の根拠 | Phase 6の決算公開を許可しない |

`coverage.source-read` は既存のPhase 5走査が部分的に検証済み。他の行をこのDraftだけで実装済み・検証済みとはしない。

## 受入テスト候補（未生成・未実行）

| Vector | 条件 | 期待する安全上の結果 |
| --- | --- | --- |
| SC-COVERAGE-01 | authoritative world catalogに2worldあるがcallerが1worldしか渡さない | 全件網羅の証拠が欠けるため決算成功として公開しない |
| SC-COVERAGE-02 | 対象worldのterrain sourceは存在するが、同じdimensionの保存Entity sourceが未調査 | Entityが0件と確定したことにせず、未確認として扱う |
| SC-COVERAGE-03 | 同一UUIDのplayerdataがlegacyと26.xの両配置に存在 | 権威の衝突を拒否し、二重計上も任意選択もしない |
| SC-COVERAGE-04 | world固有sourceをWorldScopeで除外し、playerdataが存在 | 除外worldを走査せず、playerdataは引き続き対象 |
| SC-COVERAGE-05 | manifestの保存sourceは前後一致するが、ロード済みの同じ資産が別経路に存在 | 保存source closureだけでは全件決算を公開しない |
| SC-COVERAGE-06 | callerが正しい権威world一覧を渡し、対象sourceを全件読み、各種検証も完了 | 保存source網羅性についてのみ合格候補。決算全体の成功を意味しない |

## 実装前に決定・検証する事項

1. world catalogと実rootの権威取得方法、Multiverse等の独自worldの列挙時点。callerが自称するroot一覧だけでは全world網羅の独立証明にならない。
2. 保存sourceのディレクトリ不存在が「その版では正当な空」と「想定sourceの未検出」のどちらかを識別する規則。空ディレクトリを必須とする仕様はここでは採用しない。
3. legacyと26.xの同時存在時に、別実体・別保存版・同一物理資産を区別する権威選択。任意の優先順位をこのDraftから導入しない。
4. loaded-vs-diskの同一時点性・APIスレッド制約・停止/保存バリアはPhase 6の正本で決定する。ここで凍結方式や再適用方式を勝手に確定しない。

## Gate・完了状態

Status: **Draft**。正式CAT process-closure / model-conformance / vectors / Queue / current-item生成は**not-run**。本書は未解決の権威境界と検証可能な候補を明示する成果物であり、実装・全source網羅の証明・Paper実機成功・統計公開・merge・releaseを意味しない。

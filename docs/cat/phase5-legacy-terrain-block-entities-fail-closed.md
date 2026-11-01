---
cat_version: '0.3'
kind: tce
id: simplecoins.terrainlegacy.tce.guard
status: draft
process: simplecoins.terrainlegacy
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs:
  - simplecoins.terrainlegacy.pi.chunk
semantic_contract: markdown-v2
language: ja
---

# TCE: legacy terrain TileEntities fail-closed

網羅要求: partial

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | --- | --- |
| legacy.root.nonempty | 走査要求 | `入力.root_tile_entities == "nonempty"` | 1 | `出力.visit = 偽; 出力.error = 真` |
| legacy.root.malformed | 走査要求 | `入力.root_tile_entities == "malformed"` | 1 | `出力.visit = 偽; 出力.error = 真` |
| legacy.level.nonempty | 走査要求 | `(入力.root_tile_entities == "absent" または 入力.root_tile_entities == "empty") かつ 入力.level_tile_entities == "nonempty"` | 1 | `出力.visit = 偽; 出力.error = 真` |
| legacy.level.malformed | 走査要求 | `(入力.root_tile_entities == "absent" または 入力.root_tile_entities == "empty") かつ 入力.level_tile_entities == "malformed"` | 1 | `出力.visit = 偽; 出力.error = 真` |
| legacy.empty | 走査要求 | `(入力.root_tile_entities == "absent" または 入力.root_tile_entities == "empty") かつ (入力.level_tile_entities == "absent" または 入力.level_tile_entities == "empty")` | 1 | `出力.visit = 真; 出力.error = 偽` |

## 適用境界と未決定事項

- terrain `Kind.REGION` chunkのroot `TileEntities` と `Level.TileEntities` を対象とする。非空または非List値は、visitor呼出し前に`IOException`として拒否する。空Listは通過する。既存modern `block_entities` adapterには影響させない。
- SC-LEGACY-BLOCK-01/02: root / Levelの非空Listは拒否し、visitor呼出しは0件。
- SC-LEGACY-BLOCK-03: root / Levelの非List値は拒否し、visitor呼出しは0件。
- SC-LEGACY-BLOCK-04/05: 空Listは通過し、modern `block_entities` は従来のadapterで処理する。
- rootとLevelの双方が不正な場合の優先順位は規範未確定。ここでは「どちらかが不正なら拒否」という安全要件のみ記述し、詳細なerror文字列をoracleにしない。
- これは不完全な旧形式を黙ってゼロ計上しない防御境界であり、legacy block-entity decoder、Paper live/disk整合、完全なsource inventory、settlement公開を実装・保証しない。
- StatusはDraft。利用者の規範確認、正規process-closure / model-conformance / vectors / Queue / current-item生成Gateはnot-run。既存JUnit GreenやPRのCIを正式CAT Gate合格の根拠にしない。

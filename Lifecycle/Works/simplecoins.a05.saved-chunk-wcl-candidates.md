---
cat_version: '0.2'
kind: work
id: simplecoins.a05.saved-chunk-wcl-candidates
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A05 — 保存済みEntityチャンクからWCL収納を未公開候補化

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/539 |
| https://github.com/surumeneco/SimpleCoins/pull/540 |
| https://github.com/surumeneco/SimpleCoins/pull/541 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.saved-chunk-wcl-candidates | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| Paper |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.saved-chunk-wcl-candidates.md;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Scope and provenance

- 02A/02Bと既存Confirmed A05 Process/PI/TCEの範囲で、実装済みの保存PDC再構成（#541）、実WCL読取（#537）、収納額評価（#540）を一つの**未公開・読取専用・保存チャンク局所**処理に結線する。
- 入力は、上位がdiscover/traverseVerifiedで同定して**SAVEDとして選択済み**のEntityチャンク、実world UUID、継続するEntity UUID重複検出集合。入力を受け取ったこと自体で認証・同一世代性・全ソース走査済みとはみなさない。
- WWC/WWWの保存PDC名前空間・markerを検索し、該当Entityの実UUID/種別を厳格に検証する。対象名前空間が存在するのにmarker・型・種別が不整合なら保留（IOException）。単なるバニラEntityは対象外として継続し、WCLのNotFound/Failure/INCOMPLETEも0Cとして採用しない。
- 正常な対象はWCLのworld/entity/provider/relationships/slot付きitemsを受け取り、共有wallet UUID集合で入れ子貨幣価値を各slotに算出する。返すのはsource-localで未公開の候補のみ。名前文字列からownerを決定しない。
- 候補はchunk座標、world UUID、Entity UUID、provider、WCL関係者、slotごとの金額、物理source provenanceを持つ。重複Entity UUID・不正なPDC・WCL読取失敗・不正な収納バイトはfail-closedで、上位は全候補を破棄する。

## Existing authoritative references

- Confirmed: `docs/cat/confirmed/a05-wcl-consumer-process.md`, `a05-wcl-consumer-pi.md`, `a05-wcl-consumer-tce.md`。その正式TCE 8件のGateは局所純粋判定であり、この保存チャンク候補化に自動拡張しない。
- Original WCL APIs: `PaperWonderfulReaderRegistry`, `PaperWonderfulLoadedEvidence`, `PaperWonderfulSavedPdc`, `PaperWonderfulSnapshotAssets`; source discovery: `WorldSnapshotTraversal`.
- Technology: Java 25, Paper 26.2; branch `feature/*` from `develop`; Issue #423.
- Authorized paths: this Work; `src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java`; `tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java`; `.github/workflows/paper-wcl-services-probe.yml`.

## Verification and stopping boundary

- 実配布WCL/WWC 2.1.1/WWW 1.1.2のPaper 26.2で、実Anvilの未ロードfarチャンクからWWC収納10,000C／WWW収納15,000Cを**製品候補Collectorが自動列挙**し、Entity/world/slot/relationshipsが独立証拠と一致すること。非対象のバニラEntityを読取成功0として誤認しない。
- 複数ソースにまたがる同一Entity UUID、曖昧・壊れたmarker、失敗したWCL応答について、欠落や重複を無視せず拒否する。対象ソースの保存状態・未ロード状態を変更しない。
- CIは同一PR HEADでPaper、Domain、Phase6を実測し、正規CAT Work構造検査を実行。新規TCEのConfirmed/正式Implementation Conformance passedは主張しない。
- これだけではSAVED/LIVE/バニラ装備の重複排除、全writer排他、同一会計世代、帰属証拠・BAN/playerdata、全件決算原子的公開、G3配布ACK、入室許可は**未成立**。上位G0から未公開のままにしておく。

---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-terrain-observation-producer
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-20:A04-saved-BlockLocker-CAT-approved
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

# A04 — 選択されたSAVED地形全体からのBlockLocker収納観測候補生成

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/562 |
| https://github.com/surumeneco/SimpleCoins/pull/574 |
| https://github.com/surumeneco/SimpleCoins/pull/577 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-terrain-observation-producer | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit; official Paper 26.2 + BlockLocker 1.15 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-terrain-observation-producer.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java;src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndex.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookup.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndexTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java;src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndex.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookup.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java | src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndexTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Acceptance and bounded scope
- Implement a **read-only producer** that uses a real discovered Manifest and the existing complete `WorldSnapshotTraversal.traverseVerified` source traversal. Collect selected SAVED REGION Chunks by world, find *all* chest/trapped_chest BlockEntities in those selected Chunks and use existing `SavedBlockLockerChestSigns.observe` to identify attached PRIVATE/MORE_USERS/[Everyone] signs, including crossing Chunk boundaries and reciprocal double chests. Deduplicate each physical double-chest group; no skipped candidate or invented Free owner.
- Reject malformed physical chest BlockEntities, inconsistent Chunk/world/source or missing neighboring SAVED Chunk needed for a complete local observation, multiple copies, and any explicit memory budget overrun with IOException. Preserve provenance of original selected source files and the verified saved-content witness. Never mutate the real Paper world.
- Return only immutable *unpublished observations* with their original selected sources and `ContentWitness`. Do not mint `ExternalProof(true,...)`, local owner index, P/F/D balances, writer exclusion, G0 publication/Admission or G3 ACK. No evidence of a source generation after the witness's limited observation window is implied. A witnessed saved file may still differ from Paper LIVE.
- Use the existing real Paper 26.2+BlockLocker 1.15 stopped-world fixture: expected five protected chests + one nonprotected chest, seven physical chest blocks, double chest across two chunks, and no synthesized protected UUID when proof is unknown. The saved producer must report the same effective attached sign profiles as the single-chest observer. Preserve all existing JUnit/Domain/Phase6 regression and real Paper fixture markers.
- Memory bound: the initial producer has an explicit `maxSavedTerrainChunks` cap; exceeding it stops the entire candidate rather than spilling/inventing completeness. This bounded staging unit is **not** a production-scale G0 integration. A future windowed/streaming design must be selected with actual data volume and writer-freeze evidence.
- The normative A04 Process/PI/TCE remain unchanged. Formal CAT status, Green/Red, feature integration, and G0 global writer-epoch state must be reported distinctly.

## Same-source local owner-index assembly (still unpublished)
- Reject the **forged-observation loophole**: `Snapshot` is a completed capture receipt and must be constructible only by `SavedBlockLockerTerrainObservations.capture`, never by external callers attaching hand-authored chest/sign claims to a real `ContentWitness`. Keep public immutable read-only accessors and unchanged validation of the genuine captured source. This does not make saved source immutably frozen.
- Permit an actually completed, genuinely discovered SAVED world with **zero chests** to produce an empty physical owner index. Every subsequent lookup of a chest not physically observed must still reject; unknown/incomplete proof for a nonempty observation or failed builder must still reject. A zero-chest result is not evidence that whole-world G0 is ready.
- Bind an actually completed `SavedBlockLockerTerrainObservations.Snapshot` to its **identical discovered Manifest**, using the stored `WorldSnapshotTraversal.ContentWitness`; reject a different/hand-created source Manifest, unselected or duplicate physical Region provenance, and stale on-disk bytes before and after assembling the index.
- For every saved chest observation, call the existing `SavedBlockLockerPhysicalOwnerIndex.Builder.accept` exactly once with a **per-observation proof provided by the caller**. This stage must never synthesize positive `SavedBlockLockerOwnerCandidate.ExternalProof` flags; unknown proof, conflicting signs, duplicate storage members and partial failure leave no constructed index.
- A test-only independently supplied positive proof can validate local OWNER/FREE representation after authentic compressed Anvil read; it is not a G0 writer epoch or production authorization. Preserve the existing global admission, snapshot publisher, G3 ACK and real-VPS settings unchanged.

## Authentic capture → BAN-aware source-scoped staging adapter (still unpublished)
- Provide a direct, **read-only** factory from the authentic `SavedBlockLockerTerrainObservations.Snapshot`, its **same discovered Manifest**, independent per-chest proof, and complete declared `BanSourceCoverage` into the existing `SavedBlockLockerSettlementOwnerLookup.sourceScoped()` adapter. Reject incomplete proof, substituted discovery or changed selected SAVED files before returning the adapter; do not synthesize full-writer quiescence or change the existing explicit `fromIndex` diagnostic seam.
- Exercise the *actual compressed Anvil* terrain and a real stored monetary ItemStack across two adjacent Region files: `discover → capture → assembled owner lookup → source-scoped settlement staging → local unpublished Result`, plus representative fail-closed negative cases. This is a local candidate only and not evidence of global G0/Paper writer closure; do not publish, ACK, admit, or deploy.
- Reuse the existing A04 Confirmed Process/PI/TCE, `SettlementScanRunner`, `StoredItemValue`, and `BanSourceCoverage`. No new normative decision, no newly invented owner class, and no redundant standalone settlement runner.

## Full SAVED BlockLocker and WCL source composition (same local candidate)

- Under the already user-Confirmed A04 and A05 classifications, connect the **existing authentic terrain Snapshot** plus its per-chest independent proofs to the existing source-scoped BlockLocker owner adapter, and run the **existing real WCL hybrid saved traversal** with a single `BanSourceCoverage`, Manifest and live-source generation. Expose only a typed, one-shot **unpublished** method; do not add a parallel settlement evaluator, manual owner index, global writer-complete assertion, G0 publication or G3 ACK.
- A04's Confirmed SAVED owner verdict is **chest-specific**. A full real `REGION` source may also hold barrels, furnaces, hoppers, etc. The combined product router must verify each selected BlockEntity position **against its actual source Chunk palette** and dispatch supported physical chests to the proven A04 source-scoped resolver; all other inventory-containing BlockEntity kinds require a separately supplied `SourceBlockOwnerLookup` with independently established source/ownership facts. No silent `FREE` fallback, no misclassification as an invalid chest, no omission of otherwise valid empty containers or natural generated chests. For this isolated Paper fixture only, the explicit non-chest provider returns FREE under the observed no-BlockLocker-installed/zero-player assumptions after source match. That fixture provider is **not** a deployed completeness proof.
- Even on invalid terrain proof, changed selected Manifest/witness, missing BAN information or failure to stage WCL, the original Paper capture is consumed and cannot be reused to retry a weaker candidate. Check both the terrain capture witness and saved candidate witness before returning. The existing `fromIndex` diagnostic callback is never accepted as equivalent to this authentic producer.
- One isolated actual Paper26.2 + shipped WCL/WWC/WWW test phase persists four real WCL fixtures (50,000C) and an **additional real physical 5,000C chest** in a far, unloaded selected terrain Chunk. The chest is known non-protected under the **test-only** installed-plugin/fixture assumptions, with separately declared positive local proof, not a global quiescence certificate. Full stage must count WCL 50,000C as D and chest 5,000C as F exactly once; 50,000C + 5,000C cannot be mistaken for 50,000C by omitting a whole source. Witnesses and world/Chunk provenance must remain aligned.
- A new candidate with UNKNOWN chest proof must fail closed; separately missing one known WCL owner BAN source must fail the entire candidate, not silently downgrade the protected/free or WCL monetary value. All original 3-JVM WCL regression, source provenance, actual saved CURRENCY PDC, and admission-guard checks remain mandatory.
- Exact fixture BAN/provider and no-BlockLocker assumption are only isolated CI inputs; this Work establishes **neither the full deployed BlockLocker/JAR configuration, all writer exclusion, full IP/external BAN coverage, valid CAT Implementation Conformance, nor G0/G3 publication or release**. No main or server deployment.

### 実配布BlockLocker 1.15とWCLの同一Paper世代検証

- 既存のWCL+WWC+WWW+Paper26.2三段階CIの実保存fixtureを流用しつつ、**別の独立隔離ワールド**を真正なBlockLocker 1.15 JARと共に起動する。5,000C入りPRIVATE chestを保存済みの同じ未ロード遠方Chunkに置き、さらに1,000C入り[Everyone] chestを置く。単独A04/JUnitテストや単独A05の成功を合成せず、同じ実Paperの保存ManifestでWCLと両保護チェストを一括走査・原資産候補化する。
- PRIVATEは実導入版BlockLockerのLIVEプロファイルによる正しいUUID所有者を保存前に検査し、SAVEDでは地形/看板表裏・同一Region・物理位置・有効プロフィール・主所有者を独立照合。Everyoneは不明主所有者でも適用される有効な表示・保存ルールに従ってFreeとする。BAN/providerの完全性はテスト限定の宣言であり、本番の収集を偽装しない。
- 当該試験の期待集計はSAVED WWC/WWW全4個体=Dead 50,000C、PRIVATE胸=Dead 5,000C、Everyone胸=Free 1,000Cで、合計56,000C。未ロード維持、元データの再読込、その他自然生成収納の漏れ・重複がないことを既存全件候補経路で検証。PRIVATE保護証拠欠損、Everyone表示矛盾、該当所有者BAN証拠欠損については採用せずHOLD。
- 既存のBlockLocker未導入3段階・55,000C経路と実BlockLocker個別再起動回帰を維持する。G0全writer捕捉・G3永続ACK・全外部BAN・Pluginの未知な非協調writer・グローバル公開・入室許可の証明には昇格させない。CATのConfirmed規範本文を変更せず、既存Workの境界内に限る。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-tce.md | — | — | — | @work/.cat-flow/cache/a04-saved-owner-model.json | @work/.cat-flow/cache/a04-saved-owner-vectors.json | @work/.cat-flow/cache/a04-saved-owner-queue.json | @repo:SimpleCoins/tests/cat/a04-saved-owner-junit-binding.json | @work/.cat-flow/generated/A04SavedOwnerGeneratedTest.java | @work/.cat-flow/cache/a04-saved-owner-obligations.json | — | false |

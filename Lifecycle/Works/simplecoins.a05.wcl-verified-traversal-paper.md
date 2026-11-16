---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wcl-verified-traversal-paper
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/pull/445
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A05 — 全件決算候補の実トラバーサルと保存WWC/WWW Readerの同一ファイル証拠

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/445 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/AnvilRegionReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wcl-verified-traversal-paper | simplecoins.a05.unloaded-stored-provider-readback |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; pinned WGL 1.0.1, WCL 0.1.0, WWC 2.1.0, WWW 1.1.1; real entity MCA |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wcl-verified-traversal-paper.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43819120679 (real Paper saved traversal, no CAT normative Gate) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Independent bounded test

- Reuse actual first Paper JVM generated, saved WWC-typed Chicken and WWW-typed Wolf, both containing authentic SimpleCoins items in separate saved `entities/r.1.1.mca`, with known UUID witnesses. No real user, existing server or live registry mutations.
- On independent Paper JVM startup, require Chunk(40,40) remains unloaded. Traverse exactly that known genuine saved Entity Anvil file through production `WorldSnapshotTraversal.traverseVerified`, whose source-stamp preflight observes actual file bytes and whose built-in `SavedEntityUuidIntegrity` fails on duplicate entity UUID. Require the source, file count, target chunk coordinates, and exact WWC/WWW Entity UUIDs and types to match the first JVM.
- While that immutable `ContentWitness` is retained, run the existing production `AnvilRegionReader` → recognized PDC conversion → actual WCL `WonderfulStorageReader.readStored` pathway for both individual sources, checking WWC nested 5000×2 and WWW owner/commander + 10000 weapon + 1000×5 cargo.
- After both provider callbacks, require the `ContentWitness` to still match the disk source bytes and physical file identity; recheck that Chunk(40,40) was never loaded. Require genuine Paper + pinned distribution CI marker. Do not assume stable bytes are a writer-free global epoch, or that an ad-hoc single-file Manifest has completed discovery provenance.
- Validate non-production test Work structure only; do not alter SimpleCoins runtime source, admission, money, real VPS, CAT Confirmed or formal Gates.

## Verified Paper 26.2 result

- At exact PR HEAD `045c6ffc9a30b8821131ea62504c09baf12cbb72` official Paper 26.2 build 134 CI [#43819120679](https://github.com/surumeneco/SimpleCoins/actions/runs/43819120679) passed. Domain foundation and Phase6 static CI at this HEAD also succeeded. CAT Work structural validation returned `contract_valid:true`, `semantic_approval_verified:false`, not formal Gates.
- Two independently identified real Paper Entity fixtures created in process A were saved to `entities/r.1.1.mca`. In the next Paper JVM, `WorldSnapshotTraversal.traverseVerified` read exactly that known saved file and confirmed the WWC Chicken and WWW Wolf's **distinct, unique saved UUIDs and types**, also running its built-in global duplicate Entity UUID validator and creating a physical file/content witness.
- The existing distinct `AnvilRegionReader.scan` path then decoded WWC's true stored ShulkerBox coin count (5000×2) and WWW's v3 stored owner/commander and genuine weapon/cargo coins (10000 + 1000×5), invoking the pinned shipped `WonderfulStorageReader.readStored` API. The initial witness `requireUnchanged()` succeeded **after** these calls; no source bytes/physical identity drift was observed. `world.isChunkLoaded(40,40)` remained false.
- Source marker `PAPER_A05_VERIFIED_SAVED_TRAVERSAL_TWO_WCL_PROVIDERS_PASSED uniqueEntityIds=2 knownRegionFiles=1 chunkNeverLoaded=true` and previous `PAPER_A05_UNLOADED_WWC_WWW_SAVED_NBT_TO_REAL_WCL_PASSED source=entities/r.1.1.mca entities=2 regionChunk=40,40 totalCoins=25000` both present.
- **Only a manually constructed one-file Manifest was used**. It lacks full directory discovery provenance, all-world atomicity and real genome individual lifecycle; positive data and witness consistency are not G0 publication authorization.

## Scope boundary

Known-file, two-fixture evidence only. No complete region enumeration, consistent-world capture, player or container de-duplication, real genome managed WWC/WWW lifecycle, IP BAN, global wallet ownership, generation lock, publish/guard authority, or production settlement integration is claimed.

---
cat_version: '0.2'
kind: work
id: simplecoins.a05.paper-discovered-source-closure-proof
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/455
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A05 — Paper実WorldFolderのソース集合を発見し、保存WWC/WWW Readerと突合

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/455 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.paper-discovered-source-closure-proof | simplecoins.a05.wcl-verified-traversal-paper |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; pinned WCL/WWC/WWW/WGL; real World#getWorldFolder; saved MCA |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.paper-discovered-source-closure-proof.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43823225949 (real Paper source-set closure, CAT normative Gates not-run) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Observed official Paper 26.2 source closure

- Tested branch source HEAD `d383872514126a4856a0c3f294b679f21a2482fa` on isolated official Paper build 134, with all pinned distributed WGL/WCL/WWC/WWW and default-deny admission. Exact-head [real Paper CI #43823225949](https://github.com/surumeneco/SimpleCoins/actions/runs/43823225949), Domain foundation #43823225953 and Phase6 static #43823225923 all succeeded. Work contract structure was valid; `semantic_approval_verified:false` and formal CAT Gates not-run.
- The **real Paper `world.getWorldFolder()`** rooted the existing `WorldStorageDiscovery.discover` with a true `DiscoveryOrigin`, actual region/entity files and an explicit WorldScope. The second JVM discovered **9** genuine saved `.mca` source files and visited **1,601** chunks through `WorldSnapshotTraversal.traverseVerified`. It visited **0** saved playerdata files in this specific empty-player isolation. Unlike the former one-file Manifest, real bounded source-set re-enumeration and source identity stamps were exercised.
- Both expected WWC Chicken and WWW Wolf were present exactly once in far Chunk(40,40); file-scoped Entity global UUID duplicate screening and content stamps were exercised. The registered pinned `WonderfulStorageReader.readStored` APIs still decoded authentic WWC nested 5000×2, WWW 10000 weapon+1000×5 cargo and owner/commander. Both `ContentWitness.requireUnchanged()` and `Manifest.requireUnchangedDiscovery()` succeeded after readback. No far Chunk load occurred.
- Exact log `PAPER_A05_REAL_PAPER_WORLDFOLDER_DISCOVERY_CLOSURE_PASSED world=world regionFiles=9 chunks=1601 playerFiles=0 provenance=true farChunkUnloaded=true`. Previous total 25000 and unique Entity identities also passed.
- **Boundaries:** This real discovered manifest includes one test world and no playerdata. It is *not* proof of shared Paper `players/data` discovery with existing user data (#429 remains Draft/unmerged), all Multiverse worlds, global LIVE/saved coherence, provider-managed genome lifecycle, user wallet global deduplication or release readiness.

## Bounded actual-Paper verification

- Reuse isolated official Paper 26.2 two-JVM persistence experiment with genuine saved WWC/WWW typed Entity fixtures, real SimpleCoins coins and pinned shipped reader JARs. Keep admission default-deny.
- In Paper's second JVM, before loading far Chunk (40,40), use actual `World#getWorldFolder` to discover authoritative `region`, `entities` and independent `players/data` source files via the existing production `WorldStorageDiscovery.discover`, with a bounded visit budget and explicit `WorldScope`. Do not fabricate a manual Manifest for this stage.
- Confirm the known saved Entity MCA is present **exactly once** and all discovered sources have unique physical keys; require a non-null `Manifest.origin`. Run `WorldSnapshotTraversal.traverseVerified` over the complete discovered manifest for this **one isolated Paper World**, visiting all region and entity files, all playerdata sources, and detecting duplicate saved Entity UUIDs globally. Validate the original WWC/WWW entity identity and type exactly once in that traversal.
- Use the existing independent `AnvilRegionReader` and pinned registered `WonderfulStorageReader.readStored` to restore WWC & WWW money and owner/commander. After readback, recheck the captured `ContentWitness.requireUnchanged()` **and** source discovery set, with far Chunk still not loaded.
- If the world is writing unrelated region files during the test, fail rather than report a stable generation. A successful proof means only **the bounded test World's observed disk files** were complete and stable over the test window; it never proves all Multiverse worlds, external plugin writer suspension, simultaneous LIVE values or G0 complete snapshot.
- Extend this **same existing verification Work** beyond separate WCL reads to one actual whole-manifest `PaperCapturedHybridSettlement.stageCandidateWithSavedWcl` invocation using the saved WWC/WWW source from the real second Paper JVM. Select LIVE chunks from the actual Paper World and route the far unloaded Entity chunk only through SAVED; require one shared physical wallet context and every discovered WCL holder (both persisted near and far WWC/WWW) in the same source set, with the authentic total 50,000C counted exactly once. The known 25,000C far-source subtotal is not an excuse to omit the independently persisted 25,000C near-source entities; missing saved vanilla tame-owner or mainhand physical evidence must hold, not be erased from discovery. Real WCL provider bytes/owner and vanilla WWW mainhand mirror must agree. Require zero unproven candidate publication/ACK/Admission, unchanged manifest/witness, and far Chunk still unloaded.
- In a separate new capture of the **same real fixture**, pass a deliberately bounded fixture BAN provider set to the existing `stageCandidateWithSavedWclAndBanCoverage`: verify both documented fixture owner UUIDs retain the entire 50,000C as D because real playerdata is absent, and that omitting one owner's BAN evidence **fails** instead of silently classifying them CLEAR. These deliberately declared fixture providers are NOT evidence that Paper IP BAN or external plugins were observed; no production BAN completeness or G0 writer epoch is claimed.
- Exercise real Paper+current distribution JARs in the existing WCL 3-JVM CI with the existing no-login guard. Keep this Work as `issue-work` verification, not a new user Confirmed decision, and do not add production code solely for this test.
- No runtime implementation change, no admission opening, no valid normative CAT promotion; this is independent issue-work verification only.

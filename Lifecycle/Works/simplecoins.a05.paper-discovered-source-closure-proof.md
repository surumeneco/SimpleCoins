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
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded actual-Paper verification

- Reuse isolated official Paper 26.2 two-JVM persistence experiment with genuine saved WWC/WWW typed Entity fixtures, real SimpleCoins coins and pinned shipped reader JARs. Keep admission default-deny.
- In Paper's second JVM, before loading far Chunk (40,40), use actual `World#getWorldFolder` to discover authoritative `region`, `entities` and independent `players/data` source files via the existing production `WorldStorageDiscovery.discover`, with a bounded visit budget and explicit `WorldScope`. Do not fabricate a manual Manifest for this stage.
- Confirm the known saved Entity MCA is present **exactly once** and all discovered sources have unique physical keys; require a non-null `Manifest.origin`. Run `WorldSnapshotTraversal.traverseVerified` over the complete discovered manifest for this **one isolated Paper World**, visiting all region and entity files, all playerdata sources, and detecting duplicate saved Entity UUIDs globally. Validate the original WWC/WWW entity identity and type exactly once in that traversal.
- Use the existing independent `AnvilRegionReader` and pinned registered `WonderfulStorageReader.readStored` to restore WWC & WWW money and owner/commander. After readback, recheck the captured `ContentWitness.requireUnchanged()` **and** source discovery set, with far Chunk still not loaded.
- If the world is writing unrelated region files during the test, fail rather than report a stable generation. A successful proof means only **the bounded test World's observed disk files** were complete and stable over the test window; it never proves all Multiverse worlds, external plugin writer suspension, simultaneous LIVE values or G0 complete snapshot.
- No runtime implementation change, no admission opening, no valid normative CAT promotion; this is independent issue-work verification only.

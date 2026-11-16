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
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Independent bounded test

- Reuse actual first Paper JVM generated, saved WWC-typed Chicken and WWW-typed Wolf, both containing authentic SimpleCoins items in separate saved `entities/r.1.1.mca`, with known UUID witnesses. No real user, existing server or live registry mutations.
- On independent Paper JVM startup, require Chunk(40,40) remains unloaded. Traverse exactly that known genuine saved Entity Anvil file through production `WorldSnapshotTraversal.traverseVerified`, whose source-stamp preflight observes actual file bytes and whose built-in `SavedEntityUuidIntegrity` fails on duplicate entity UUID. Require the source, file count, target chunk coordinates, and exact WWC/WWW Entity UUIDs and types to match the first JVM.
- While that immutable `ContentWitness` is retained, run the existing production `AnvilRegionReader` → recognized PDC conversion → actual WCL `WonderfulStorageReader.readStored` pathway for both individual sources, checking WWC nested 5000×2 and WWW owner/commander + 10000 weapon + 1000×5 cargo.
- After both provider callbacks, require the `ContentWitness` to still match the disk source bytes and physical file identity; recheck that Chunk(40,40) was never loaded. Require genuine Paper + pinned distribution CI marker. Do not assume stable bytes are a writer-free global epoch, or that an ad-hoc single-file Manifest has completed discovery provenance.
- Validate non-production test Work structure only; do not alter SimpleCoins runtime source, admission, money, real VPS, CAT Confirmed or formal Gates.

## Scope boundary

Known-file, two-fixture evidence only. No complete region enumeration, consistent-world capture, player or container de-duplication, real genome managed WWC/WWW lifecycle, IP BAN, global wallet ownership, generation lock, publish/guard authority, or production settlement integration is claimed.

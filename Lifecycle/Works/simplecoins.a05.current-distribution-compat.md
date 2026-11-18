---
cat_version: '0.2'
kind: work
id: simplecoins.a05.current-distribution-compat
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
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

# A05 — 現在の配布候補JARで既存WCL製品読取を再検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/530 |
| https://github.com/surumeneco/WonderfulCoreLibrary/blob/develop/wcl-plugin/src/main/java/co/surumene/wcl/plugin/inventory/WonderfulStorageReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.current-distribution-compat | simplecoins.a05.live-loaded-reader;simplecoins.a05.stored-reader |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; pinned WGL/WCL/WWC/WWW release-JAR bytecode; Bukkit ServicesManager |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.current-distribution-compat.md;tests/paper-wcl-services-probe/*;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/*;.github/workflows/paper-wcl-services-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

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

## Verification scope and exact deployed-candidate identity

- The historical Paper proof uses pinned `MineCraftXPlayServer@90aa2d0e0a5ea80c705fd67d717f463decaf2a32` with WWC 2.1.0/WWW 1.1.1; current public `develop@79e4afdbc530c9f8151c44bb390d06f7ff8fcf6e` holds WWC 2.1.1 and WWW 1.1.2 instead. [PR #530](https://github.com/surumeneco/SimpleCoins/pull/530) established exactly two replacements and 22 other JARs unchanged in the static 24-jar source tree. This Work explicitly retests the *current* binary candidates without claiming the actual running VPS uses them.
- Preserve the independent existing official Paper 26.2 build 134 WCL/WWC/WWW/WGL integration probe, the product`PaperWonderfulReaderRegistry` and `PaperWonderfulLoadedEvidence` usage, zero-login admission gate and the 3 JVM baseline/persist/reload phases. Replace only the locked checkout/ref, distributed WWC/WWW JAR names and their Git blob hash assertions in the workflow. All historical Phase5 WWC/WWW fixture behavior is the acceptance oracle.
- Require both provider registration identities to remain valid and the new distribution to preserve loaded chicken shulker/owner evidence, saved far WWC/WWW two-entity Anvil read and source witness, product `readStored`, wrong-provider NotFound, corrupt schema failure behavior, and the no-chunk-load guard. Failure is a compatibility Red to be diagnosed; do not silently skip old fixture assertions.
- This independently verified source/version compatibility is **not** the full CAT-TDD A05 Implementation Conformance, current VPS loaded-JAR identity, a complete world/entity inventory, G0 writer quiescence, G3 ACK protocol, permission to publish values or admission authorization.

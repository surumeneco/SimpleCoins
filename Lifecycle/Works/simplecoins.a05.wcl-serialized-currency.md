---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wcl-serialized-currency
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A05 — WCL収納のシリアライズ済み貨幣金額の読取専用評価

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/537 |
| https://github.com/surumeneco/SimpleCoins/pull/538 |
| https://github.com/surumeneco/SimpleCoins/pull/539 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wcl-serialized-currency | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| Paper |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wcl-serialized-currency.md;src/main/java/co/surumene/simplecoins/paper/PaperLiveItemValue.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSnapshotAssets.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperLiveItemValue.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSnapshotAssets.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Acceptance
- Existing approved 02A/02B currency/wallet and nested-container semantics and existing Confirmed A05 Process are authority; this is a **bounded technical bridge**, not a newly confirmed TCE or a formal Gate pass.
- Input is one caller-selected, source-verified `PaperWonderfulLoadedEvidence.Snapshot` with actual WWC/WWW `StoredItem` serialized bytes; the upstream collector remains solely responsible for entity/provider/saved-or-live/generation identity.
- Evaluate each occupied provider slot using the existing physical NBT money parser, preserving the per-slot identity, counting nested shulker coins and wallets only once per source and failing on a cloned wallet UUID. No display-name-based recognition or silent invalid-source-to-zero conversion.
- Fail on duplicate provider slot, malformed serialized bytes or invalid monetary NBT. Intact non-monetary items may carry value zero; a successfully decoded value zero does **not** prove any undiscovered source is empty.
- Isolated real Paper 26.2 uses current distributed WCL/WWC/WWW JARs; require WWC shulker 10,000C, WWW weapon/cargo 15,000C, together 25,000C in unloaded saved Entity sources. Do not load far chunks or mutate source data.
- No final owner/BAN/P/F/D classification, cross-source deduplication, true global generation closure, journal commit, snapshot publication, grant ACK, admission, main merge or deployment. Preserve these fail-closed boundaries.
- Formal CAT current-item Red/Green for an independent valuation-specific TCE is **not claimed** by this technical adapter. Existing A05 verdict TCE 8/8 does not silently become valuation coverage. Record actual Paper/CI/Java evidence separately.

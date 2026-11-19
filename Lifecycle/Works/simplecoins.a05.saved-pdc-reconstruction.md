---
cat_version: '0.2'
kind: work
id: simplecoins.a05.saved-pdc-reconstruction
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

# A05 — 保存済みEntity AnvilからWCL Reader向けPDCを読取専用復元

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/537 |
| https://github.com/surumeneco/SimpleCoins/pull/538 |
| https://github.com/surumeneco/SimpleCoins/pull/540 |
| https://github.com/surumeneco/WhatAWonderfulChicken/blob/develop/src/main/java/co/surumene/whatawonderfulchicken/service/WonderfulChickenStorageReader.java |
| https://github.com/surumeneco/WhatAWonderfulWolf/blob/develop/src/main/java/co/surumene/www/persistence/WonderfulWolfStorageReader.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.saved-pdc-reconstruction | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| Paper |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.saved-pdc-reconstruction.md;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedPdc.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedPdc.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
- This work is bounded by the already approved 02B/A05 read-only saved source/WCL relationship contract; no new normative valuation/identity TCE is Confirmed or claimed as tested here.
- From exactly one **caller-discovered** real saved Entity NBT object, verify the expected Entity UUID and vanilla species against the requested WWC/WWW provider before copying only the documented WCL storage-facing BukkitValues fields to a detached in-memory Paper PersistentDataContainer.
- WWC: strict typed marker, optional legacy data version (when present only 2/3/4), optional `follow_target`, serialized `equipment_carpet`, `equipment_shulker_box`, `equipment_head`; for v4 require canonical `genome_ref` identity. WWW: strict typed marker, schema 1 or 2, mandatory nonempty serialized `runtime_state`; for schema 2 require canonical `genome_ref` identity. Strictly refuse wrong provider/species, UUID mismatch, unknown schema, malformed type, missing required values, excessive bytes or noncanonical UUIDs. Valid absent optional fields are different from unknown/unreadable sources.
- Source file/manifest/witness, all entity discovery, live-over-saved authority, owner/BAN, storage subtype version and global generation remain the responsibilities of the caller. Recreating only the fields read by the pinned WCL provider MUST NOT be represented as a complete copy of every PDC field or a guarantee that an unrelated future provider writer is covered.
- Real pinned Paper 26.2 WCL/WWC/WWW JVM must reconstruct from the actual saved Anvil Entity map in an **unloaded chunk** and compare provider-returned UUID/world/relationship/serialized bytes and existing calculated money to the separately reconstructed test fixture; chunk remains unloaded and manifest witness unchanged.
- No inventory save, file mutation, snapshot publication, grant ACK, admission, main merge or deployment. Bounded Paper CI is technical integration evidence; formal implementation-conformance is not inferred or artificially marked passed.

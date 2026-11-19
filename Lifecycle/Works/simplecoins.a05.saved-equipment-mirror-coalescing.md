---
cat_version: '0.2'
kind: work
id: simplecoins.a05.saved-equipment-mirror-coalescing
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A05 — SAVED装備のWCL/バニラ同一現物確認と重複せず一回だけ計上する

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-pi.md |
| https://github.com/surumeneco/SimpleCoins/pull/548 |
| https://github.com/surumeneco/SimpleCoins/pull/549 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.saved-equipment-mirror-coalescing | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.saved-equipment-mirror-coalescing.md;src/main/java/co/surumene/simplecoins/domain/WclSavedEquipmentMirrorProof.java;src/main/java/co/surumene/simplecoins/domain/WclSavedSlotSupplement.java;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/test/java/co/surumene/simplecoins/domain/WclSavedEquipmentMirrorProofTest.java;src/test/java/co/surumene/simplecoins/domain/WclSavedSlotSupplementTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/WclSavedEquipmentMirrorProof.java;src/main/java/co/surumene/simplecoins/domain/WclSavedSlotSupplement.java;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java | src/test/java/co/surumene/simplecoins/domain/WclSavedEquipmentMirrorProofTest.java;src/test/java/co/surumene/simplecoins/domain/WclSavedSlotSupplementTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Confirmed A05 source and bounded acceptance
- 02A/02B and approved Confirmed A05 Process/PI/TCE remain normative: the same physical item/wallet is attributed **once**. This Work is a bounded technical SAVED equipment mirror proof; it changes no accepted product meaning or publication condition. Work remains draft and cannot self-confirm other normative semantics.
- The ordinary `ResolvedEntitySettlementImport` must first stage the selected SAVED Entity equipment into the SAME uncommitted `SettlementAccumulator`, source file and LIVE/SAVED-selected chunk as the WCL `readStored` result. The existing `SavedLiveSourcePartition`, actual Manifest and saved content witness are mandatory; never invent an extra monetary source.
- The positive WCL WWW weapon mirrors vanilla Entity `equipment.mainhand`, and WWC head mirrors `equipment.head`. A positive mirror is accepted **only if all of**: WCL provider snapshot identity/slot success; exact selected saved Entity UUID and canonical species; saved equipment NBT; WCL bytes decoded as an ItemStack with bounded NBT; full structural item equality across map/list/arrays (not merely equal amount, type, display or digest), except the WCL serialized root `DataVersion` wrapper is ignored **only** when equal to the current Paper server DataVersion; all other keys and nested data must still match exactly; and native accumulator asset with the same source, normalized physical key, amount **and P/F/D owner/BAN/playerdata classification**. Any mismatch, missing item, extra key, stale source, unobserved native value or independent/wrong owner rejects. Positive mirror must never be added as a second asset.
- The actual provider `readStored` may be called only once per identified SAVED Entity. Its immutable serialized StoredItem bytes must be retained in the already verified `PaperWonderfulSavedChunkCandidates.Candidate` at the same time as its amount. Do not call the external service a second time to "confirm" equality.
- `SettlementAccumulator` gains a narrowly scoped, read-only exact-existing-asset assertion while the source is still open. It never marks source complete, creates a new asset or silently substitutes an unknown duplicate. Already completed/unknown sources, distinct classifications and values are rejected. Standard nonmirrored WCL shulker/carpet/cargo remains unchanged; positive mirrors without this evidence keep their prior HOLD failure.
- Wallet UUID double views remain fail-closed where earlier shared UUID valuation already rejected the repeat; this bounded change enables the **non-wallet item** positive mirror only, not general wallet coalescing. Similar-looking items at two independent physical positions are not merged.
- JUnit tests: identical saved Entity equipment vs WCL NBT item succeeds only once for P/F/D; unequal count/item PDC/nested payload, missing/multiple owner entity, nonexistent native monetary slot, unmatched amount/source/owner/BAN or mismatched slot all reject; preexisting backward-compatible overload still rejects positive unproved mirrors.
- The official Paper 26.2 saved fixture exposed a real serialization-only difference: `ItemStack.serializeAsBytes()` includes `DataVersion=4903` at root while saved vanilla equipment omits it. The Domain legacy strict proof must not strip this unverified wrapper; the main-thread Paper adapter must supply its actual runtime DataVersion, and mismatched, forged, or extraneous keys remain errors. This narrow conversion is not a new product specification.
- Run pinned official Paper 26.2 with WCL/WWC/WWW and existing saved fixtures; preserve independently observed saved WWW owner contradiction HOLD and WWC 10,000C dead; test bounded actual provider item bytes vs saved NBT shape without claiming proven positive equivalence where fixture does not have an installed native weapon. A JUnit Green alone is not full source coverage, writer freeze or global settlement.
- Full A05 vectors/Queue/generated tests/implementation-conformance, G0 writer exclusion/same epoch/atomic publication and G3 ACK/JE-BE/admission remain separate, unresolved and fail-closed. Do not release or weaken guard.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-saved-equipment-mirror-model.json | @work/.cat-flow/cache/a05-saved-equipment-mirror-vectors.json | @work/.cat-flow/cache/a05-saved-equipment-mirror-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05SavedMirrorGeneratedTest.java | @work/.cat-flow/cache/a05-saved-equipment-mirror-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.

---
cat_version: '0.2'
kind: work
id: simplecoins.a05.saved-world-metadata-identity
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

# A05 — Paper 26.2の保存World metadataからSAVED WCL UUID正本を検証する

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
| simplecoins.external-storage-provider-integration | simplecoins.a05.saved-world-metadata-identity | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.saved-world-metadata-identity.md;src/main/java/co/surumene/simplecoins/domain/PaperSavedWorldMetadataWitness.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/test/java/co/surumene/simplecoins/domain/PaperSavedWorldMetadataWitnessTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/PaperSavedWorldMetadataWitness.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java | src/test/java/co/surumene/simplecoins/domain/PaperSavedWorldMetadataWitnessTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
- Normative authority remains the user-confirmed 02A/02B and A05 Process/PI/TCE. This technical Work establishes disk-derived World UUID *evidence* for the existing unpublished SAVED WCL callback; it does not modify any accepted P/F/D, live/save, snapshot, grant or writer contract, nor self-promote a draft into Confirmed.
- The Paper 26.2 metadata source is the dimension root's `data/paper/metadata.dat`, whose `data.uuid` is stored by PaperWorldMetadata as UUIDUtil.CODEC int[4]. Legacy world-root `uid.dat` is NOT the authority for the current Paper 26.2 layout. This exact source is established by the pinned Paper source and must be verified with the real 26.2 Paper runner.
- Only a `WorldStorageDiscovery.Manifest` returned by real discovery with `origin.worldFolders()` may prove saved World provenance. Caller-supplied UUID Maps are **not** authority. Resolve every discovered world root (including those with no Entity MCA), require canonical complete name-to-UUID coverage, distinct nonzero UUIDs, regular non-symlink metadata path, bounded GZIP/NBT decode and exact 4-int UUID. Missing, malformed, incorrect or conflicting identities must fail rather than silently fabricate source zero.
- Compare the resolved disk UUIDs to every supplied expected UUID and, on the actual Paper main thread, to any currently loaded corresponding `World.getUID()` and `World.getWorldFolder()`. Unloaded worlds have no live Bukkit object and are checked from disk only; a manually built Manifest is not promoted into a discovered source.
- Retain a byte-level metadata fingerprint/witness and validate it before and after SAVED WCL read and after the complete one-shot Hybrid stage returns, including after LIVE owner callbacks; this is a source-drift detector, *not* a global writer lock or ABA-proof epoch. Rewrites, disappearance, symlink substitution, duplicate UUIDs, wrong World name/UUID, and malformed data reject.
- Both existing `stageCandidateWithSavedWcl` overloads must use the new witness without changing their public API. The non-WCL `stageCandidate` path remains untouched. The actual saved WCL adapter must never treat a manually supplied map as independently verified.
- JUnit pure read tests: a real gzip PaperWorldMetadata-shaped NBT file yields expected World ID; distinct worlds and empty-world origin are accounted; mismatched/duplicated world UUID, missing/symlink/malformed/unknown metadata and same-length restored-mtime byte mutation reject; no-origin manifests cannot claim authority.
- Pinned official Paper 26.2 integration: use the actual `World.getWorldFolder()` and real generated metadata bytes, capture World identity evidence, check UUID equals live Bukkit world UUID, verify the independent SAVED WWC 10,000C candidate remains D; prove that a provided false UUID fails and no saved World is invented. Require a specific Paper success marker in reload phase.
- This is only a technical *local world-identity precondition*. It proves neither all World roots are enumerated nor complete LIVE/SAVED/writer epoch, physical equipment equivalence, atomic publication, G3 durable ACK, admissions or full CAT Implementation Conformance. All existing fail-closed protections remain.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-world-identity-model.json | @work/.cat-flow/cache/a05-world-identity-vectors.json | @work/.cat-flow/cache/a05-world-identity-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05WorldIdentityGeneratedTest.java | @work/.cat-flow/cache/a05-world-identity-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.

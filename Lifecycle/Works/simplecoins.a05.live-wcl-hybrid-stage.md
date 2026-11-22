---
cat_version: '0.2'
kind: work
id: simplecoins.a05.live-wcl-hybrid-stage
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

# A05 — 同一Capture内で実WCL収納を既存Hybrid候補に追加し、衝突・未確認を拒否する

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
| simplecoins.external-storage-provider-integration | simplecoins.a05.live-wcl-hybrid-stage | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.live-wcl-hybrid-stage.md;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/domain/WclHybridLiveOverlay.java;src/test/java/co/surumene/simplecoins/domain/WclHybridLiveOverlayTest.java;src/test/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlementWclTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/domain/WclHybridLiveOverlay.java | src/test/java/co/surumene/simplecoins/domain/WclHybridLiveOverlayTest.java;src/test/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlementWclTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Approved source and bounded acceptance
- Normative sources: confirmed A05 Process/PI/TCE and adopted 02A/02B. This Work only provides an **unpublished** evidence/staging adapter and does not change any normative monetary, ownership, source-generation or publication semantics.
- Use the exact PaperCapturedHybridSettlement generation's existing mutable wallet identity context for all additionally selected LIVE WCL readLoaded candidates; select an actual loaded Entity and its loaded Chunk on Paper main thread and check Entity UUID/world/current position and source membership. Never assume WCL is absent because no callback ran.
- When an explicitly selected WCL candidate is present, attach every strictly positive, independently stored WCL slot to its already required LIVE Chunk source; do not invent another source or add amount twice. For aliases pointing to already scanned vanilla equipment or display projections, fail closed on collisions, including equal-amount aliases, and do not silently suppress either view.
- WCL owner HOLD, invalid selected Chunk, duplicate WCL entity, unresolved mirror mainhand/head item, duplicate physical key, malformed slot or unknown provider must fail and discard the whole candidate generation. Verified FREE/OWNER may stage only with explicit BAN/playerdata evidence and the same source identity. Empty Success is local-only; does not prove all WCL entities were scanned.
- The existing stageCandidate no-WCL path remains behavior-compatible. If WCL candidate was explicitly captured, it must be staged or rejected, never silently omitted. Ensure single-use capture and fail-closed after an error.
- Add JUnit tests for a unique WWC shulker amount in selected loaded Chunk, owned vs FREE vs dead BAN/playerdata classification, collision with WWC display and WWW weapon, invalid source chunk, HOLD and duplicate Entity; use real Paper WCL probe for loaded selected Entity binding where feasible.
- **Required local freshness guard for the same confirmed candidate boundary**: retain the first successful WCL read's concrete Entity/World, verified owner/relationships, slot values, physical aliases, and serialized ItemStack bytes. In the real Paper path, re-read the identical enrolled live WCL sources near the end of saved/loaded staging (after saved source witness confirmation), rejecting and consuming the entire candidate if source identity, currently loaded chunk, owner, relationships, slot values, aliases, or original ItemStack bytes differ. The one-shot verified capture is never reusable after rejection. A nullable or absent original observation cannot be reinterpreted as a verified zero. Test-only `fromObservedWithWcl` has no real Paper identity and may not claim this live recheck.
- **Native equipment mirror local freshness**: even with identical WWW/WCL owner, cargo, weapon snapshot and monetary total, an independent Paper equipment writer can replace the actual vanilla mainhand after the first capture. The initial WCL-vs-native equality proof must remain valid at the end of staging for every explicitly verified mirrored slot (`www/weapon→mainhand`, `wwc/head→head`). Re-read the real loaded Entity equipment on Paper main thread and compare with original native bytes and the original one-read WCL ItemStack; reject the consumed candidate upon mismatch, null, unreadable or missing gear. A real Paper probe changes only vanilla gear after capture while WCL storage stays unchanged, requires rejection, and restores the fixture. This is source-local A05 evidence and NOT the G0 global writer fence.
- **Actual multi-plugin Red/Green**: the real distributed WWW plugin's `saveAndRegister` changes the commander's UUID after a genuine initial WCL owner/monetary capture but before staging. Its real 10,000C native weapon and 5,000C cargo remain byte-identical. The stale candidate must be rejected rather than returning 15,000C attached to the old commander; preserve the normal unchanged WCL candidate success and all current protected saved-source regressions. WWC's product writer similarly changes `follow_target` without monetary value change (observed separately in A01).
- This recheck is a **necessary local stale-source detector**, not a sufficient G0 world epoch: a writer can mutate immediately after the recheck and before final publication. No global writer admission/drain, source-closure, normal journal handoff, atomic publishing, G3 ACK or admission changes are authorized.
- **Not granted by this Work**: autonomous all-WCL discovery/coverage, all-writer freeze, exact LIVE/SAVED epoch, global asset completeness, saved WCL staging, atomic publish, G3 receipt or admission. The returned Staged remains unpublished even when an isolated accumulator can commit.
- Keep CAT Process Closure/Model Conformance distinct from full Implementation Conformance and do not promote Draft to Confirmed.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-live-hybrid-model.json | @work/.cat-flow/cache/a05-live-hybrid-vectors.json | @work/.cat-flow/cache/a05-live-hybrid-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05LiveHybridGeneratedTest.java | @work/.cat-flow/cache/a05-live-hybrid-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.

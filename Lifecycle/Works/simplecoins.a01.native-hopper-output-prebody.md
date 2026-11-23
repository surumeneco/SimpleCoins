---
cat_version: '0.2'
kind: work
id: simplecoins.a01.native-hopper-output-prebody
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/644
  - https://github.com/surumeneco/SimpleCoins/pull/635
  - https://github.com/surumeneco/SimpleCoins/pull/644
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.real-hopper-output-writer-boundary
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/G0 — protect observed NMS Hopper output before physical transfer

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/644 |
| https://github.com/surumeneco/SimpleCoins/pull/635 |
| https://github.com/surumeneco/SimpleCoins/pull/644 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.native-hopper-output-prebody | simplecoins.a01.real-hopper-output-writer-boundary;simplecoins.a01.native-hopper-pull-prebody |

## Technologies
| Technology |
| --- |
| Java25 ClassFile Agent; official Paper26.2 build134 NMS HopperBlockEntity; real Chest/Hopper; pinned WWC/WWW/WCL/WGL |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.native-hopper-output-prebody.md;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/java/**;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml | writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java | src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/java/**;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Confirmed A01 meaning, measured source and bounded implementation

Confirmed A01 G0 Process/PI/TCE requires all world monetary writers to be excluded during a drained sealed generation. PR #635 protected Paper26.2 NMS Hopper **pull**, but PR #644 proved actual NMS Hopper **output** still moves genuine 5000C from Hopper 2→0 to Chest 0→2 while the same limited-family Agent remained LIMITED_TRACKING and the product fence SEALED/drained. This is a concrete failure of that bounded writer protection, not a new product semantic decision. PR #644 captured the exact real native stack:
`HopperBlockEntity.hopperPush(Level,Container,Direction,HopperBlockEntity)Z → callPushMoveEvent` (called from `ejectItems` during normal tick). The already Confirmed A01 normative meaning remains unchanged.

Before any production change, expand the isolated Java25 stub's exact `hopperPush` signature, add a PASSIVE and OPEN/normal-return positive control, then demand a sealed call return `false`, leave the physical-method body count unchanged and fault the Agent and the *same* product fence. Run this test on the unchanged production Agent and capture the **expected reason Red** (unprotected hopperPush body executes). Do not fake an unrelated assertion failure.

Then add only the measured `hopperPush(Level,Container,Direction,HopperBlockEntity)Z` to the existing HOPPER ClassFile transformation, with two exact signatures in that NMS class. Keep its existing pre-body `beforeHopper` fail-closed boolean refusal, afterHopper normal return, exceptional fault, thread/frame bookkeeping, PASSIVE execution and matched-method fail-stop. Do not hook a broad `ejectItems` scan or any unobserved native method. Add no runtime activation.

For actual Paper Green, require a separate opt-in test JVM to first prove the same PASSIVE Hopper→Chest transfer and genuine native stack, then re-seed and demand after 60 native ticks the **Hopper still contains 2 authentic coins, Chest 0, Agent+product both FAULTED**, without server-tick crash. Preserve the prior read-only classification mode and original Hopper pull test. Check Java25 transformed bytecode, full A01 real Paper, WWC/WWW/WCL, Domain and Phase6 CI on the same final feature HEAD. Successful bounded pre-body protection does **not** constitute complete native/Plugin/direct file/async writer closure, G0 atomic publication or G3 ACK; do not enable Admission or change main/VPS/release. If Paper proves pre-body `hopperPush` is still after a physical mutation, stop this implementation and revise the technical entry using actual stack evidence rather than claiming Green.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-hopper-output-model.json | @work/.cat-flow/cache/a01-hopper-output-vectors.json | @work/.cat-flow/cache/a01-hopper-output-queue.json | — | — | @work/.cat-flow/cache/a01-hopper-output-obligations.json | — | false |

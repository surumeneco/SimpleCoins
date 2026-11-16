---
cat_version: '0.2'
kind: work
id: simplecoins.a01.live-writer-coherence-model
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/470
  - https://github.com/surumeneco/SimpleCoins/pull/471
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

# A01/G0 — LIVE writerを含む決算候補の同時点保証方式の必要条件

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/470 |
| https://github.com/surumeneco/SimpleCoins/pull/471 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.live-writer-coherence-model | simplecoins.a01.paper-hybrid-staged-stale-live;simplecoins.a01.paper-live-captured-value-drift |

## Technologies

| Technology |
| --- |
| Python 3 standard-library unittest; isolated deterministic state-machine model; independent actual Paper 26.2 evidence #470/#471 |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.live-writer-coherence-model.md;tests/a01_consistency_interleavings.py;.github/workflows/a01-consistency-interleavings.yml | — | tests/a01_consistency_interleavings.py;.github/workflows/a01-consistency-interleavings.yml |

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

## Accepted game guarantee and technical question

Drive 02B requires settlement from one globally coherent accounting generation, including saved NBT, LIVE chunks/players and externally owned assets. Publication/admission remains forbidden without proof. In real Paper 26.2, #470 demonstrated unchanged disk SHA/mtime while a physical LIVE Chest increased from 10,000 to 15,000 C. #471 then staged a **complete but unpublished** product hybrid candidate containing the stale 10,000 C observation while actual LIVE held 15,000 C. These are concrete Paper experiments, not merely a model.

This verification Work asks whether **extra finite rechecks** or **a complete writer barrier** can logically suffice for the LIVE portion, and which assumptions make the statement true. It does **not** add another speculative read-only helper, alter the server, move funds, produce a new runtime freeze mechanism, or claim all-world G0.

## Falsifiable model and acceptance

`tests/a01_consistency_interleavings.py` is a tiny, explicit interleaving model, not a surrogate for Minecraft. It evaluates every insertion position of one LIVE writer around capture, saved read, saved-witness check, optional recapture, and publication, with two changes: monetary value increase and value-conserving owner/slot transfer. The saved bytes stay fixed, as witnessed in real Paper #470/#471. It checks the following bounded implications:

1. Saved-source witness unchanged does not prove that the captured LIVE value is current.
2. Equal total currency does not prove unchanged owner P/F/D or physical source attribution.
3. Even a final LIVE rescan has a publication gap if an uncooperative writer may run after it.
4. A writer barrier covers the modelled LIVE gap only if **every** modelled writer obeys it throughout capture and publication.
5. A writer bypassing that barrier leaves stale-candidate publication possible.

Acceptance is five deterministic tests Green and exact source/Work CI, preserving the distinction between model proof obligations and real Paper observations. It is not acceptable to weaken G0 by claiming that no online players, immutable `Observed`, same saved SHA, final sum equality, or one post-stage rescan is an atomic-world guarantee.

## Technical conclusion to carry to design decision (not adopted architecture)

The current observed read-only bridge cannot safely transition from an *unpublished candidate* to an authoritative complete settlement. To make such a transition, the production design must provide a genuine globally coherent generation: either a proven barrier/epoch protocol covering **all** relevant LIVE, saved, async, third-party and receipt writers, or an independently verified alternative with equivalent closed-world assumptions. A barrier that covers only main-thread Paper events does not automatically cover arbitrary plugin code or external saved-file writes. A list of cooperating adapters is not proof of completeness unless unregistered/uncooperative writers are excluded by an enforceable system boundary. If such a boundary is infeasible for the configured server, maintain fail-closed admission and request an explicit user decision on revising the guarantee/supported environment; do not self-promote CAT Draft or downgrade integrity without approval.

**Model limits:** one writer insertion and two deterministic monetary mutations; disk source contents are fixed; no demonstration of actual Paper lock enforcement, full NBT/World/Entity enumeration, multiwriter transactions, persistence, lock-free epochs, performance or OS power-loss durability. The models are necessary-condition counterexamples and a bounded cooperative-protocol illustration, **not a proof that any real G0 implementation is complete**. This Work is `issue-work` verification, not `spec-implementation`; CAT formal Gates remain not-run.

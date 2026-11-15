---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-save-commit-verification
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# Work: simplecoins.a02.paper-save-commit-verification

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java |
| https://github.com/PaperMC/Paper/blob/ver/26.2/paper-server/patches/sources/net/minecraft/world/level/storage/PlayerDataStorage.java.patch |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-save-commit-verification | simplecoins.grants.saved-file-readback;simplecoins.grants.receipt-authenticity |

## Technologies

| Technology |
| --- |
| Paper 26.2, Java 25, JUnit 5, live-playerdata persistence and crash injection |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-save-commit-verification.md;tests/paper-grant-save-probe/*;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/*;.github/workflows/paper-grant-save-commit-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

## Verification questions and bounded acceptance

- Establish the actual Paper 26.2 save path. Distinguish `Player#saveData()` returning, playerdata file visibility, receipt and inventory belonging to one NBT generation, file/directory durability, and post-crash reload. None proves another automatically.
- Reproduce with disposable real-Paper playerdata at least: disabled player saving; save-write failure; player logout or disconnect; full inventory; interrupted replacement; post-save/pre-ack crash. Keep admission closed and never modify real user data.
- Capture for every case the authentic reservation, before/after playerdata hash and physical identity, physical coin slots, player PDC receipt, journal state, and restarted result. A failed or ambiguous physical save must not permit delivered acknowledgement.
- Existing `GrantLedger.acknowledge(Grant, long)` trusts a caller-supplied receipt value. An isolated Java filesystem probe showed that passing a matching sequence can mark the grant delivered without ever creating playerdata. This is an unproven service-boundary obligation, not evidence of a live issuance bug: the production issue/delivery service is not connected.
- Evaluate a durable transaction boundary binding acknowledgement to verifiably current saved-file transition proof; reject fabricated or stale proof. If Paper's save operation cannot establish durability, compare a dedicated serialized writer or other supported transaction protocol. Keep the adopted same-playerdata inventory/receipt requirement and exactly-once recovery behavior.
- Produce a reproducible failure matrix, the strongest actually supported guarantee, counterexamples, and a bounded feasible implementation choice. If the accepted guarantee must change, return that semantic change for legitimate confirmation before production changes. Do not turn readback alone into a claimed OS crash-durability proof.

## Authority and boundaries

- This is `issue-work` verification with an intentionally empty Check list until an executable declared check exists. No production source, physical inventory, durable record, admission flag, main release, or user account may be changed by this Work.
- `draft` and empty `decision_ref` do not constitute a Confirmed Process/PI/TCE; the official CAT Process/Model/Implementation gates are not-run. Ad hoc Java probes and conventional CI cannot replace them.

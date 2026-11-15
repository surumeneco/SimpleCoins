---
cat_version: '0.2'
kind: work
id: simplecoins.a02.saved-file-readback
status: draft
process: simplecoins.grants.saved-file-readback
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.a02.saved-file-readback

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/PlayerDataReader.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.saved-file-readback | simplecoins.grants.file-proof | simplecoins.grants.saved-transition-integrity;simplecoins.grants.saved-playerdata-drift |

## Technologies

| Technology |
| --- |
| Java 25, Paper 26.2 GZIP/NBT, JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.saved-file-readback.md;src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadbackTest.java | src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadbackTest.java |

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

## Source-grounded acceptance

- In Paper 26.2 `CraftPlayer.saveData()` delegates to `PlayerDataStorage.save`, which can silently return when `SpigotConfig.disablePlayerDataSaving` and swallows IO errors after logging. Therefore only a fresh readback of the canonical saved `.dat` content can disconfirm silent no-save; the API returning does not guarantee persistence.
- Implement strictly read-only single-player saved file verification bound to an actual discovered WorldStorageDiscovery.Manifest and UUID, with bounded compressed/inflated reads, SHA-256 before/after content witness, and `SavedGrantDeliveryReadback.verifyTransition` of preserved pre-save root against saved poststate.
- Reject missing file, unchanged/old receipt, corrupted or non-canonical saved coin, lost existing stacks, a mismatched source UUID, manually fabricated manifests and a file replacement during read. No on-disk writes, no ledger acknowledgement, no inventory mutations, no admission.
- This is not a durability guarantee, OS fsync, crash proof, or completed Paper `saveData` atomic transaction. A source-set scan or two hashes alone cannot prove world-wide temporal coherence or ABA absence. Normative CAT Gates remain not-run.

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

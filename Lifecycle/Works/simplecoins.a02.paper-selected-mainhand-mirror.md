---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-selected-mainhand-mirror
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/478
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

# A02 — Paper selected-hotbar persisted mainhand mirror transition

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/actions/runs/43999486854 |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/surumeneco/SimpleCoins/pull/479 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-selected-mainhand-mirror | simplecoins.a02.paper-invsee-save-veto;simplecoins.grants.saved-file-readback |

## Technologies

| Technology |
| --- |
| Paper 26.2 build 134, Java 25, actual compressed NBT, selected hotbar zero, production SavedGrantDeliveryReadback / SavedGrantDeliveryFileReadback |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-selected-mainhand-mirror.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml |

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

## Observed risk and bounded test

A real Paper 26.2 integration attempt in CI #43999486854 tried to place one authentic 5000 C grant into storage hotbar index 0 while the player's selected mainhand slot was index 0, then saved physical item and receipt together. The strict production transition verifier rejected with `An unplanned Ender Chest or equipment asset changed`. The same test passed after preserving the selected mainhand physical slot and placing the coin in storage slot 1; that is a **test-fixture workaround**, not a proof that delivery to selected slot 0 is forbidden by the adopted game requirement.

Reproduce the original scenario on a **third independent disposable Paper 26.2 server**: baseline empty selected hotbar index 0 with unrelated STONE in slot 9, reserve a real pending grant, plan the 5000C coin into slot 0, persist with Paper `saveData()` plus the matching completed grant PDC. Verify that the **single saved root** supports authentic playerdata coin+receipt proof, but that the **before→after transition** readback rejects because an equipment view also changed. Preserve the exact saved equipment before/after diagnostic in CI logs. A positive observation is an existing **false rejection / incompleteness**, not successful issuance.

Design for later remediation must distinguish the selected-mainhand **mirror of a granted storage item** from independent equipment slots, permit only the former to change when and only when the saved selected slot and NBT contents match the approved grant plan. Keep all independent armor, offhand, Ender Chest, unrelated storage, malformed/counterfeit items, shifted selection, and altered wallet UUID fail-closed. Do not broadly ignore the equipment map. Any real algorithmic repair requires legitimate CAT specification authority and formal Gates before product code modifications. This Work does not self-confirm semantics or open admission.

## Acceptance

- Full real Paper 26.2 saved root proves exact 5000 C authentic physical coin and matching PDC receipt for first vacant slot zero.
- Genuine production readback transition reports the observed equipment-view rejection with original slot selection unchanged.
- Journal remains pending and no low-level ACK is executed; no live client or server data.
- The existing unmitigated and InvSee++ veto test modes remain green in their own independent JVMs.
- No product source change, no CAT Normative Gate claim.

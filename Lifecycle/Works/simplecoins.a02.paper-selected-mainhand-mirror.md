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
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-selected-mainhand-mirror.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml;src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadbackTest.java;docs/cat/drafts/a02-grant-delivery-process.md;docs/cat/drafts/a02-grant-delivery-pi.md;docs/cat/drafts/a02-grant-delivery-tce.md | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml;src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadbackTest.java |

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

A real Paper 26.2 integration attempt in CI #43999486854 placed one authentic 5000 C grant into storage hotbar index 0, the player's selected mainhand slot, and saved physical item and receipt together. The strict production transition verifier rejected with `An unplanned Ender Chest or equipment asset changed`. **The subsequent independent Paper #480 CI run #44001573298 logs the exact evidence: `beforeEquipment=null afterEquipment={}`.** This proves only that the optional equipment compound was absent before save and present but **empty** afterward. It does **not** prove that a mainhand *ItemStack* was mirrored or that any equipment asset changed. The same scenario succeeds after retaining unrelated STONE in selected slot zero and placing the coin in storage slot 1; that is a test-fixture workaround, not a game restriction.

Reproduce the original scenario on a **third independent disposable Paper 26.2 server**: baseline empty selected hotbar index 0 with unrelated STONE in slot 9, reserve a real pending grant, plan the 5000C coin into slot 0, persist with Paper `saveData()` plus the matching completed grant PDC. Verify that the **single saved root** supports authentic playerdata coin+receipt proof, but that the **before→after transition** readback rejects because an equipment view also changed. Preserve the exact saved equipment before/after diagnostic in CI logs. A positive observation is an existing **false rejection / incompleteness**, not successful issuance.

Design for later remediation must first treat only the two **zero-equipment** representations (absent `equipment` and exactly empty `equipment={}`) as candidate logical equivalents. The observed CI case does not authorize suppressing a populated `mainhand`, head/armor/offhand slot or a changed `SelectedItemSlot`. Keep all independent equipment, Ender Chest, unrelated storage, malformed/counterfeit items, altered wallet UUID and actual inventory changes fail-closed. Use the original real Paper roots as evidence and the new `SavedGrantDeliveryReadbackTest` characterization to check both: genuine 5000 C same-root money+receipt passes, and **current production transition rejects only because of null-versus-empty equipment**; test-only removal of the empty marker passes, while an actual head/offhand item change remains rejected. This is **not a production normalization fix**. Any real algorithmic change still requires legitimate CAT specification authority/formal Gates, with 02B/03/05 semantics unchanged. This Work does not self-confirm or open admission.

## Acceptance

- Full real Paper 26.2 saved root proves exact 5000 C authentic physical coin and matching PDC receipt for first vacant slot zero.
- Genuine production readback transition reports the observed equipment-view rejection with original slot selection unchanged.
- Journal remains pending and no low-level ACK is executed; no live client or server data.
- The existing unmitigated and InvSee++ veto test modes remain green in their own independent JVMs.
- Two deterministic JUnit characterizations additionally distinguish absent→empty `equipment` (false rejection in current code) from non-empty head/offhand mutation (correct rejection). Under test-only exact empty-marker removal, the original valid money+receipt transition succeeds.
- No product source change, no CAT Normative Gate claim.

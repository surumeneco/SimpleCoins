---
cat_version: '0.2'
kind: work
id: simplecoins.a02.cooperative-veto-fsync-ack
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/479
  - https://github.com/surumeneco/SimpleCoins/pull/483
  - https://github.com/surumeneco/SimpleCoins/pull/484
  - https://github.com/surumeneco/SimpleCoins/pull/485
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

# A02/G3 — InvSee++協調的保存拒否からfsync・ACK・再起動までの結合検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/479 |
| https://github.com/surumeneco/SimpleCoins/pull/483 |
| https://github.com/surumeneco/SimpleCoins/pull/484 |
| https://github.com/surumeneco/SimpleCoins/pull/485 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.cooperative-veto-fsync-ack | simplecoins.a02.paper-invsee-save-veto;simplecoins.a02.paper-playerdata-fsync-abrupt-recovery;simplecoins.a02.advisory-playerdata-lock-invsee |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; shipped InvSee++ 0.31.22; SpectatorInventorySaveEvent; genuine Paper playerdata; FileChannel.force(true); original SimpleCoins GrantLedger; independent guard; two JVMs |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.cooperative-veto-fsync-ack.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-cooperative-veto-fsync-ack.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-cooperative-veto-fsync-ack.yml |

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

## Rationale and independent verification boundary

The existing #479 test cancels a real InvSee++ offline save and leaves the grant pending; #483/#484 separately prove real playerdata file/directory force and post-/pre-ACK restart; #485 demonstrates that a native advisory lock on the old UUID.dat inode does not stop the actual InvSee++ writer. The conjunction **cooperative veto during an attempted destructive save → force actual UUID.dat and containing directory → fresh source-authenticated physical+receipt readback → original durable ACK → cold-restart verification** has not been proven by those separate cases.

Use a brand-new loopback-only Paper 26.2 world with zero online players, pinned shipped InvSee++ and an independent default-deny admission guard. An isolated observer reserves exactly one genuine 5000C grant with one unrelated STONE item, saves Paper's original compressed UUID.dat and authenticated receipt, then attempts an InvSeeAPI save that would remove the coin. An observer-side event veto must reject that exact recipient's save, leaving the physical coin and signed completed receipt intact and the ledger pending. While the veto remains registered, explicitly force the real UUID.dat and parent directory, obtain a new production saved-file transition readback, and ACK the **same existing reservation** once. Reopen the durable ledger, verify delivered exactly once/pending zero, saved physical bytes and other item unchanged, and that admission remains default-deny. The first JVM terminates with Runtime.halt(139) immediately after verified durable ACK, without shutdown hooks. A second independent Paper JVM loads the same world and confirms no duplicate coin, no lost receipt, exactly one ACK, no pending grant and closed admission.

This Work tests **one cooperative InvSee++ API writer**, not a general-purpose writer fence. Paper saveData, an unrelated plugin, filesystem replacement, deferred writers, asynchronous writes and OS power failure may still invalidate the readback-to-ACK interval. It does not install a product coordinator, promote Draft Process/PI/TCE to Confirmed, assert formal CAT Gates, modify production source, enable admission or authorize release. Do not treat a positive result as G3 completion.

## Acceptance

- Pinned Paper+InvSee++ actual JARs; real authenticated saved playerdata+receipt and one pending GrantLedger reservation.
- Actual InvSee++ save receives notSaved due to the matching UUID event veto; no physical item, receipt or unrelated STONE change.
- Force actual file and directory before fresh successful production readback and one durable ACK; original file bytes are unchanged by ACK.
- First JVM exits abnormally with code 139 after ACK; second cold JVM replay reads original physical 5000C+receipt and exactly one acknowledged grant; admission stays blocked throughout.
- Existing non-veto, veto-only, advisory-lock and forced-sync tests remain green; exact-head CI evidence is separate from CAT normative Gates.

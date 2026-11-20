---
cat_version: '0.2'
kind: work
id: simplecoins.a02.invsee-two-native-hooks-closed-replay
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
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

# A02/G3 — 二つのPaper native保存入口を閉じてもInvSee++は同じplayerdataを書換可能か

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/580 |
| https://github.com/surumeneco/SimpleCoins/pull/581 |
| https://github.com/surumeneco/SimpleCoins/pull/500 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.invsee-two-native-hooks-closed-replay | simplecoins.grants.paper-save-commit;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| Java25 Class-File API and test-only javaagent; official Paper26.2 build134; pinned real InvSee++ 0.31.22; real UUID.dat and signed receipt |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.invsee-two-native-hooks-closed-replay.md;tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-invsee-both-native-hooks-closed.yml | — | tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-invsee-both-native-hooks-closed.yml |

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

## Experiment and stop criterion
The exact real Paper `CraftPlayer.saveData()V` and `PlayerDataStorage.save(Player)` calls can each be intercepted in isolated real Paper tests. This says nothing about InvSee++'s own asynchronous `FakeCraftPlayer.saveData()` or direct file IO. Determine whether **the actual installed InvSee++ writer** is outside both intercepted entrances before expanding to more methods.

1. Run the existing disposable loopback Paper 26.2 + pinned InvSee++ fixture with the **same test-only agent** instrumenting both real Paper entrances. Never alter server/VPS, plugin production JAR, Admission or ACK.
2. Retain the previously verified original 5000C coin + completed signed receipt + unrelated STONE + `GrantLedger` pending1 in actual UUID.dat. Let InvSee++'s actual API load its offline inventory **while the gate is open**. Then remove only the copied 5000C item from InvSee's fake inventory, assert a valid pending grant and fresh valid saved proof, close the `a02.nativeAgent.closed` gate **before** invoking the genuine `api.saveInventory`.
3. Require both native Paper class-transform markers in the same JVM. If this third-party save succeeds, and the *actual file* becomes coinless while retaining receipt and STONE, confirm a genuinely reached uninstrumented writer path, force file and directory, halt abruptly, and check cold recovery in a separate official Paper JVM with the Agent **absent**. Keep pending grant unACKed and `AdmissionGuard` default-deny. If the save is rejected by either hook and file is unchanged, report that actual positive result instead; do not invent an external bypass.
4. Distinguish a triggered native hook from general native-hook availability. Require the InvSee save event is actually observed and is **not** cancelled by a cooperative event-veto; otherwise this is not evidence about unconstrained plugin save.
5. Preserve the existing positive two-entry Paper save interception [PR #581] and original InvSee veto and false-ACK negative tests. Run same-HEAD Domain/Phase6 and real Paper regression. Only a test fixture, workflow and verification Work are in scope, never production code.

**Stop criterion**: A single real writing route that bypasses both native hooks invalidates any proposal that these two hooks establish G3 writer closure. Do **not** synthesize a product writer proof from hook markers, static Java callrefs or finite readback. Even if both hooks block this one InvSee operation, the full 24-JAR/Paper G0/G3 writer set, in-flight drain, LIVE mutations and atomic publication are still unproved. This is issue-work verification, not a formal A02 Process/Model/Implementation conformance or authorization to change the approved business guarantee.

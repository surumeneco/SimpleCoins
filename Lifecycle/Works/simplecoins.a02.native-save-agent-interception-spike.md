---
cat_version: '0.2'
kind: work
id: simplecoins.a02.native-save-agent-interception-spike
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

# A02/G3 技術実験 — Paper native save entryをJVM Agentで捕捉できるか

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/505 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.native-save-agent-interception-spike | simplecoins.grants.paper-save-commit;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| JDK25 Java Instrumentation and standard java.lang.classfile API (test-only); official Paper26.2 build134; InvSee++ 0.31.22; disposable loopback server |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.native-save-agent-interception-spike.md;tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-native-save-agent-entry-spike.yml;.github/workflows/paper-a02-native-inflight-drain-gap.yml | — | tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-native-save-agent-entry-spike.yml;.github/workflows/paper-a02-native-inflight-drain-gap.yml |

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

## Bounded hypothesis and rejection criterion
Existing real Paper probes prove that InvSee++ event veto, one known Future drain and a stable saved-file readback fail to exclude Paper's own `CraftPlayer.saveData()` between verification and durable ACK. Holding the old inode's advisory lock likewise did not bar an actual writer. Instead of repeating these rejected hypotheses, test whether a **JVM startup Instrumentation transformer** can intercept the real Paper 26.2 `CraftPlayer.saveData()` entry itself.

- **Baseline**: Use the existing disposable official Paper/InvSee fixture to persist canonical 5000C+signed receipt+STONE in a real UUID.dat, with exactly one genuine pending GrantLedger reservation. The original uninstrumented Paper scenario erases the coin despite InvSee's cancelled event; preserve that pre-existing separate CI proof.
- **Intervention**: A dedicated **test-only** Java Agent instruments the exact `CraftPlayer.saveData()V` bytecode on class load. Instrumentation must report exactly one target method and emit an observable marker. When the test sets `a02.nativeAgent.closed=true` only after the legitimate initial file save and successful fresh readback, an attempted real Paper `saveData()` from the same object must be rejected *before its body runs*. If the transformer does not run, the target method is absent, or the attempted write changes the physical file, the isolated experiment fails.
- **Verification**: Re-read genuine playerdata: 5000C+signed receipt+unrelated STONE persist, the InvSee veto count is unchanged, and the ledger stays pending/unACKed. Force UUID.dat and parent directory, then `Runtime.halt(143)` and recover on a fresh real Paper JVM without the Agent. Both real persisted money and receipt survive without duplicate delivery or ACK; startup AdmissionGuard remains default deny.
- **Explicit limits**: This captures **only one verified entrypoint**. It does not capture NMS `PlayerDataStorage.save`, FakeCraftPlayer overrides, async writes already in flight, direct filesystem I/O, any other plugin, all world/Chunk/Entity mutations, writer admission/drain, OS power loss, G0 or G3. Even a Green real-Paper run is **feasibility evidence, not an implementation authorization or complete writer fence**. The Agent requires a process launch flag (Java 25 Class-File API; no JDK-internal ASM exports); do not ship it in SimpleCoins or change server startup/main/VPS. No normative Draft→Confirmed promotion, no ACK or Admission enablement.
- **Next decision**: If interception is feasible, assess whether the *whole* actual writer set could be captured/drained by an equivalent, lifecycle-safe mechanism. If not, reject this mechanism and compare supported server-extension/fork or source-generation alternatives; guarantee changes or operational server architecture changes are escalated to the user rather than adopted silently.

## Follow-up — already admitted native writer must be drained, not just reject NEW entries

**Bounded negative feasibility test; no normative approval or production component.** The verified CraftPlayer entry hook in #580 rejects a fresh save after `closed=true`, but did not examine the interval in which that same method has *already passed* its open-state entry check and has not yet written the playerdata. The G0/G3 invariant in approved Drive 02B/04 requires both new-entry denial **and** proof that previously admitted writers finished before accepting a candidate/ACK; these are independent observations.

Run on the existing isolated official Paper26.2 build134 + actual InvSee++ 0.31.22 with the normal real 5000C coin+signed completed receipt+STONE and one unACKed GrantLedger reservation. First cancel an authentic InvSee save through the existing cooperative test-only listener; verify fresh readback before the negative attempt. For this one scenario, enable a bootstrap-visible **test-only pause after the actual instrumented `CraftPlayer.saveData()V` entry has passed the open/closed check but before its real body**. Reuse the existing detached Paper Player that made the authentic saved file; remove only its in-memory coin (preserve receipt and STONE).

An independent same-JVM control thread must observe the actual in-flight-entry marker, verify the saved source still contains the 5000C while held, set `a02.nativeAgent.closed=true`, and release the held actual save. The main-thread entry had already passed, so its real body is expected to overwrite the same UUID.dat *after closure* with no coin. Verify an unchanged InvSee veto count, same authentic completed receipt and STONE, pending=1, delivered=0, **fresh readback rejects** and no ACK. Force playerdata+directory and `Runtime.halt(149)`; verify the coinless receipt+pending persisted after a separate Paper JVM restart without Agent. The actual observed ordering and timeout/failure conditions must be machine-checkable (missing hold marker, write before closure, no post-close write, unexpected ACK or absent saved file are failures). This is a **negative counterexample**, not a correct candidate or safe publication.

Use a separate CI workflow `paper-a02-native-inflight-drain-gap.yml`, reusing the original test-only agent/hook and Paper/InvSee fixture. Preserve #580/#581/#582 positive cases. Do not alter production SimpleCoins class/JAR, deploy a startup Agent, change Admission/ACK or `main`; the test-only gated hold must be off by default. A successful run proves only **this single known entry** needs both admission denial and in-flight drain, not that all Paper/external writers are capturable or G0/G3 is solved. If it fails, inspect actual Paper log and reject the hypothesis rather than deem a red workflow a successful source proof. Formal CAT Gates stay not-run for this independent issue-work verification.

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
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.native-save-agent-interception-spike.md;tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-native-save-agent-entry-spike.yml;.github/workflows/paper-a02-native-inflight-drain-gap.yml;.github/workflows/paper-a02-native-drain-accounting.yml;.github/workflows/paper-a02-native-nested-save-drain.yml;.github/workflows/paper-a02-independent-nms-root-drain.yml;.github/workflows/paper-a02-native-family-atomic-ack.yml | — | tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-native-save-agent-entry-spike.yml;.github/workflows/paper-a02-native-inflight-drain-gap.yml;.github/workflows/paper-a02-native-drain-accounting.yml;.github/workflows/paper-a02-native-nested-save-drain.yml;.github/workflows/paper-a02-independent-nms-root-drain.yml;.github/workflows/paper-a02-native-family-atomic-ack.yml |

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


## Follow-up — actual native save return-counted admission/drain (bounded positive proof)

The verified #592 counterexample shows that a save already admitted through the real `CraftPlayer.saveData()V` entry can write *after* `closed=true`. Test a distinct, limited **test-only mechanism**, not the product G0/G3 coordinator or a universal writer registry.

- **Before changing the Agent** test-only Probe expects an observable *atomic* entry/exit accounting protocol. On a disposable official Paper 26.2 build134 with the real InvSee++ 0.31.22 distribution and actual product/independent default-deny guard, first save authentic 5000C+signed receipt+STONE and keep the actual grant ledger pending. Verify the previously established canceled InvSee event and fresh saved-source proof.
- Only after fixture setup, enable tracking for a single real `CraftPlayer.saveData()` with a test-only bootstrap hook. This original main-thread invocation must be counted as *entered* **before** the controllable hold and its actual method body. An independent control thread observes the held entry, closes the test gate **atomically with the admission counter**, and asserts `active=1` and a bounded `awaitDrained` **does not succeed while held**. It releases the original call and then waits for `active=0` only after a **bytecode-instrumented actual normal method return** (not after hook entry, not after the control thread lets go).
- With no untracked second writer to this fixture, the original in-memory coinless save remains permitted because it was already admitted and will erase the physical 5000C. It must retain signed receipt and unrelated STONE, leave `GrantLedger` pending/ACK=0, and cause new readback to reject. Once drain is proven, a *new actual* `CraftPlayer.saveData()` must be denied at method entry with no file change and no counter leak. Verify the normal-exit count matches the single admitted call. `force` UUID.dat and directory, `Runtime.halt(150)`, then an independent agent-free Paper JVM must observe the coinless receipt+pending and default-deny guard.
- **Exception/unknown-completion is fail-closed**: the test-only tracker may decrement only for an observed normal method return; an exception path that cannot be proven complete must not report `drained`. A zero counter from some unrelated fixture or before startup must not be accepted. Add strictly bounded timeouts/markers and reject missing hook transformation, premature zero, nonzero after genuine normal return, duplicate release, unexpected ACK, or changed existing negative/positive probes. The test-only gate must be disabled by default and preserve all earlier agent cases.
- CI runs this as independent `.github/workflows/paper-a02-native-drain-accounting.yml`, reusing pinned real Paper/InvSee JARs, probe, Java25 Class-File API Agent, and actual saved UUID.dat; additionally run JUnit/Domain, guard, and existing A02 native writer regressions. No production code, no Agent on the actual server, no `main`, no release, no player admission or durable ACK.
- **Interpretation limit**: this is admission/normal-exit drain evidence for exactly one synchronous Paper-native method. It does **not** prove in-flight async work inside or after a method return, exceptional completion, NMS/direct filesystem writers, other plugins, LIVE world/chunk/entity mutations, all-writer coverage, atomic G0 publication or G3 ACK. CAT Work remains an independent `issue-work` Draft verification, not Confirmed or any formal CAT Gate pass. The result selects no production architecture.

## Follow-up — real CraftPlayer→NMS nested save after admission closure

**Issue #423 / G0–G3 independent test-only feasibility question; no product fence or normative approval.** PR #593 counts real CraftPlayer normal returns, and #581/#582 prove a separately callable NMS `PlayerDataStorage.save(Player)` entry. Their relationship during a real CraftPlayer invocation is unverified. In particular, closing **both** entry gates before a previously admitted CraftPlayer's original body can reach NMS may reject the nested work; treating every NMS entry as independently unauthorized can break progress, while blindly allowing any NMS entry on a thread can let a new untracked writer through.

- In the existing isolated official Paper 26.2 build134 / pinned InvSee++ 0.31.22 fixture, keep genuine saved 5000C+signed receipt+STONE, one pending grant and default-deny; test actual nested call structure rather than infer from method names. Enable both exact native method transformations **before class loading**, but keep the new family gate off until initial correct save and canceled InvSee save have been physically verified.
- Track one actual `CraftPlayer.saveData()V` invocation. Its entry must increment a *shared* active-scope count under the same gate lock. Hold after that entry before its real method body. Independent control thread observes active=1, closes new-root admissions atomically with scope count, verifies bounded `awaitDrained` stays false while held, then releases the existing admitted root.
- While the original CraftPlayer method runs after closure, instrument the **actual** NMS `PlayerDataStorage.save(Player)` entry and normal return. Determine whether it is a **same-thread nested descendant** of the admitted Craft scope. If so, allow only that causally nested NMS entry as part of already admitted work and count it as an additional in-flight scope. Demand exact one nested NMS entry and normal return (if the actual Paper call graph differs, the scenario **fails with evidence**, not a claimed positive proof). The combined drain succeeds only after both actual normal-return hooks and shared active=0, not at release or NMS return alone.
- After closure, call both **new** root CraftPlayer save and an independently resolved **actual NMS** `PlayerDataStorage.save(Player)` on the original detached player. Both must be refused before their bodies; persisted UUID.dat bytes, normal-return counts and active count remain unchanged. The admitted save is expected to erase only the old 5000C, preserving receipt+STONE, pending1/ACK0. Fresh readback must reject stale evidence. Force file+parent directory and `Runtime.halt(151)`; cold agent-free Paper JVM independently verifies coinless signed receipt+pending and default-deny.
- A nested save under an **untracked** root, on another thread or after the parent scope returned must never gain inherited admission. The test-only entry/progress tracking fails closed for unknown/exceptional exits and is disabled without the exact `a02.nativeAgent.trackNativeFamily` flag. Keep existing Craft-only gate, two native entry regression, InvSee event/Future tests unchanged. No recursion beyond this observed two-method case, no asynchronous task/context propagation, no JAR production changes, no main/VPS/deployment/Admission/G0 publication/G3 ACK. A success proves only the real **synchronous** method chain in this fixed Paper build, not all writers or full writer-epoch closure.
- Verify by separate `.github/workflows/paper-a02-native-nested-save-drain.yml` actual Paper CI and full existing scoped regressions. Work status remains `draft`/`issue-work` `verification`; CAT `validate` is syntax/contract only, not semantic approval or formal Gate.

## Follow-up — one native Craft/NMS family gate spanning authentic readback→ACK

**Independent test-only G3 feasibility experiment**, not a production coordinator and not CAT Confirmed. Previous #594/#595 prove that the two real Paper native synchronous entries can be tracked and drained, but both keep a coinless, unACKed negative fixture. The remaining question is whether, in the **positive saved 5,000C + signed receipt fixture**, one real root save can be observed and the closed family gate kept across fresh readback, filesystem force and a **single** genuine pending grant ACK.

1. On the existing official Paper26.2 + genuine InvSee++ 0.31.22 loopback/zero-player fixture, seed the genuine signed 5,000C receipt+STONE playerdata and one pending grant. Cancel the genuine InvSee++ save and verify unchanged physical file. **Before altering the detached Paper player's in-memory slot**, enable the test-only family tracker, execute the original CraftPlayer.saveData; require actual Craft root and NMS child normal returns, pending/receipt/5,000C preserved.
2. Under the shared native-family lock atomically close new Craft and NMS root admissions, observe active zero, and deny an additional actual CraftPlayer.saveData with no on-disk change. Introduce only a test-only method that performs a short callback **under the identical native family mutex**, requiring closed=true, active=0, real Craft+NMS return evidence. Fresh `SavedGrantDeliveryFileReadback.verify`, file+directory force, and `GrantLedger.acknowledge` occur inside that callback in this isolated experiment. If readback/ACK fails, leave admissions closed and no false success marker. No stale proof may be substituted.
3. Prove genuine `UUID.dat` still contains exactly 5,000C and the signed completed receipt with unrelated STONE, while one durable ledger history is delivered and pending=0. Abruptly stop with a unique halt code and independently reboot **without Agent** to read the same physical file and ledger. Product admission remains default deny; no listener/give or normal journal activated.
4. Scope strictly to these two verified synchronous Paper entries and separately vetoed InvSee future; cannot prove arbitrary direct filesystem write, other plugin, async continuation, exception scope completion, OS sudden power loss, global G0 or full product G3 coordinator. Existing #510 false-ACK counterexample remains required; do not relax snapshot or delivery semantics. Run a separate `paper-a02-native-family-atomic-ack.yml` CI and retain existing A02 modes and startup/Domain regressions. Do not modify main/VPS/production Java, publish or ACK in product.

## Follow-up — independent NMS root admitted before closure

**Independent bounded `issue-work` verification for Issue #423, not a production change or normative approval.** #594 verified the exact CraftPlayer→NMS nested synchronous call. It did not test an independently invoked NMS root that has already entered the actual instrumented `PlayerDataStorage.save(Player)` while writer admissions are still open. If its body executes after closure, a gate that only waits for CraftPlayer roots can announce a false zero.

- In the existing loopback/zero-player official Paper 26.2 build134 + pinned genuine InvSee++ fixture, first persist the real 5000C coin, signed receipt and unrelated STONE in the same physical `UUID.dat`; establish `GrantLedger` pending=1/ACK=0, cancelled InvSee save and valid fresh readback. Leave the test-only family tracker disabled during setup. Do not touch product code, Paper distribution or server configuration.
- Resolve the actual NMS `PlayerDataStorage.save(Player)` and detached `ServerPlayer` from runtime, and invoke this **root directly without CraftPlayer.saveData()**. Enable both existing Java25 Agent hooks and the same synchronized family gate; after the root NMS passes open admission and increments active=1, hold before its real body via a **new opt-in NMS-only latch**, disabled in every existing probe.
- A separate control thread checks the real saved bytes remain unchanged during the hold, Craft returns=0, NMS returns=0, nested NMS admissions=0 and active=1. Close admissions atomically, prove a bounded drain does **not** return true prematurely, release the original root, and verify drain only after the real NMS bytecode's normal-return hook (NMS returns=1, Craft returns=0, nested admissions=0, active=0).
- Check the old 5000C was overwritten with 0C by the already-admitted root, while signed receipt, STONE, pending=1/ACK=0 remain intact, and the original delivery readback is now rejected. Both a fresh independent NMS root and a fresh Craft root after closure must be denied without changing file bytes or the counters. `fsync` actual file+parent, `Runtime.halt(152)`, then cold-restart in an **Agent-free** Paper JVM to verify the same coinless/unACKed state and independent default-deny guard.
- Failure modes include uninstrumented target, missing real entry hold, zero before real return, unexpected nested Craft entry, lost receipt/STONE, spontaneous ACK, changed saved file after denied new roots, or missing cold-restart proof. This can establish only that one synchronous independent NMS root is admitted and drained; it proves **neither** all Paper/third-party/async writers nor G0 publication, G3 product coordinator, or operational Agent adoption.
- Run `.github/workflows/paper-a02-independent-nms-root-drain.yml` for actual Paper and regression evidence. The CAT Work remains Draft; structural validation is not Process Closure, Model Conformance or CAT Confirmed. Admission remains blocked; no `main` merge, VPS deployment, ACK or release.


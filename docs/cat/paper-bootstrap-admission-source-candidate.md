# Paper bootstrap / admission wiring — code-to-spec Candidate

Status: **Candidate (observed implementation only; not Confirmed)**  
Work: `TASK-20261108-001`  
Repository baseline: `surumeneco/SimpleCoins@f7ebdc33227eb279d44461e9d7e6bf2d86598b23` (`develop`)  
Flow: `code-to-spec`; this document is a source-bound observation, **not** a CAT Process Closure, Model Conformance, or approval for production changes.

## Verified source observations

| Source (Git blob) | Observed behavior |
| --- | --- |
| `src/main/java/co/surumene/simplecoins/SettlementAdmissionListener.java` (read at baseline) | Paper `PlayerConnectionValidateLoginEvent` handler only kicks when the event is currently allowed and either the barrier or the bound snapshot publisher cannot admit. It does not call `event.allow()`. The compatibility constructor has no publisher and therefore cannot admit. Constructor rejects a publisher bound to another barrier. |
| `src/main/java/co/surumene/simplecoins/domain/AdmissionBarrier.java` (`718fa4f9893c119e33d037cc1f0c0076b69a0750`) | Default state is BLOCKED. `begin()` transitions BLOCKED→SCANNING and returns a generation token. `complete()` accepts only matching SCANNING generation with verified snapshot and zero online players; otherwise fails closed. `stop()` moves to STOPPED. |
| `src/main/java/co/surumene/simplecoins/domain/SettlementSnapshotPublisher.java` (`f036496c627a8934c78f6739c883fd540b8b6972`) | `mayAdmit()` requires a published result and READY barrier; publication validates accounting totals and dead-asset locations; failures revoke admission. |
| `build.gradle.kts` (`945251b72e63194188829f74df045b76a77920f3`) | Java 25 toolchain, Paper API `26.2.build.134-stable` compileOnly/testImplementation, JUnit Jupiter 5.11.4. |

At this exact baseline, the recursive Git tree contains **no `plugin.yml`** and no class extending `JavaPlugin`. The existing listener source itself states it is not registered until the authoritative lifecycle is wired. Therefore a loadable Paper plugin, registration, and an actual join-path denial are **not established** by these domain classes alone.

## Proposed verification obligations (not implemented)

1. A loadable plugin descriptor points to an existing `JavaPlugin` entrypoint, and the built JAR contains both.
2. Before a verified complete settlement is published, the actual registered login listener denies an otherwise allowed login; it never overrides a denial from another plugin.
3. A missing publisher, initialization exception, or failed/aborted startup cannot accidentally enable admission.
4. Repeated enable/disable and server restart do not duplicate listeners, scheduled tasks, or service registrations; shutdown revokes admission.
5. Paper main-thread boundaries, dependency readiness, and command delegation are checked by an actual Paper fixture rather than inferred from standalone domain tests.

## Ownership / decision boundary

The bootstrap lifecycle and listener registration belong to TASK-20261108-001. Settlement completion, snapshot authority, and admission release semantics belong to TASK-20261101-005; command business behavior belongs to TASK-20261101-006. The above observations are **not** an authorization to change their contracts. Existing game-spec decisions and formal CAT Artifact confirmation remain separate. No `Confirmed` status, CAT Gate pass, JUnit pass, or Paper startup pass is asserted here.

## Next formal step

Use this observed source mapping and the actual user-approved 02B/03/05 clauses as inputs to the canonical CAT `issue-work` and `spec-implementation` flow; require genuine authority/closure/model evidence before production wiring. This Candidate does not replace that flow.

---
cat_version: '0.2'
kind: work
id: simplecoins.a00.phase5-static-bytecode-structural-regression
status: draft
process: simplecoins.settlement.saved-source-inventory
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/270
  - https://github.com/surumeneco/SimpleCoins/pull/274
  - https://github.com/surumeneco/SimpleCoins/pull/278
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

# A00/G0 — 保存ソース静的配線検査器のJVM定数プール構造検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/270 |
| https://github.com/surumeneco/SimpleCoins/pull/274 |
| https://github.com/surumeneco/SimpleCoins/pull/278 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/tools/verify_phase5_saved_wiring.py |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.saved-source-inventory | simplecoins.a00.phase5-static-bytecode-structural-regression | simplecoins.settlement.default-deny;simplecoins.world-source-discovery |

## Technologies

| Technology |
| --- |
| Python 3 standard library; Java classfile JVMS §4.4.1/§4.4.5/§4.4.6; GitHub Actions; pinned CAT-TDD issue-work validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a00.phase5-static-bytecode-structural-regression.md;tools/verify_phase5_saved_wiring.py;tests/test_verify_phase5_saved_wiring.py;.github/workflows/a00-phase5-constant-pool-structure.yml | — | tools/verify_phase5_saved_wiring.py;tests/test_verify_phase5_saved_wiring.py;.github/workflows/a00-phase5-constant-pool-structure.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a00.phase5.classfile | tests | exit-zero | @repo:SimpleCoins | python3 -m unittest discover -s tests -p test_verify_phase5_saved_wiring.py -v | class-forward-reference;invalid-class-index;truncated-class-index;terminal-long-double;valid-reserved-slot;unique-main-descriptor | tests/test_verify_phase5_saved_wiring.py;tools/verify_phase5_saved_wiring.py | unittest exit/status | 60 |

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

## Observed defect and bounded acceptance

Existing `tools/verify_phase5_saved_wiring.py` is a conservative, **static-only** inventory of bytecode class references used to flag missing saved-source scanning hints. It is not a proof of runtime invocation, full LIVE/SAVED source closure, or G0/G3. The current `utf8_pool` and `class_references` walk JVM constants independently. Both increment the index twice for `CONSTANT_Long`/`CONSTANT_Double`, but do not reject a wide constant at the **last legal index**, which would require a nonexistent reserved second slot under JVMS §4.4.5.

Old [#274](https://github.com/surumeneco/SimpleCoins/pull/274) contains a narrowly targeted parser correction and two missing regressions: reject terminal wide constants in **both** functions; continue accepting valid wide constants with an actually reserved adjacent pool slot. [#270](https://github.com/surumeneco/SimpleCoins/pull/270) holds three other missing structural controls: a valid forward `CONSTANT_Class` Utf8 pool index must resolve; out-of-range class name index and truncated index must fail closed. An additional old [#278](https://github.com/surumeneco/SimpleCoins/pull/278) test proves another static-validation bypass: duplicate `main:` entries in `plugin.yml` currently allow the first declared class to pass even when the later entry could override Paper's actual entrypoint. Reject descriptors unless exactly one `main` key is present. **These six tests** are the bounded missing cases for this Work.

**Test-first requirement:** integrate the six original independent regression test methods as-is into the current Python unittest module and confirm an expected-reason failure on terminal-wide cases against the unpatched current parser. Then apply only the two narrow `index + 1 >= count` checks from #274 and the one duplicate-main guard from #278 to the current analyzer; require all six new tests plus all existing tests Green. Maintain the stricter exception type `InvalidClassFile`, and do not treat malformed `CONSTANT_Class` as missing/no-reference proof.

The actual product accounting engine, scanned plugin JARs, G0/G3 generation coherence, player admission and external Plugin writers remain untouched. The enhanced static verifier may reject malformed synthetic/classfile input earlier; this is a verification-tool integrity change, **not** a new approved product semantic or a CAT-generated oracle.

## Completion and nonclaims

Run pinned CAT `cat_flow.py validate` for this Work, dedicated Python unittest with **six exact named extra cases**, existing Domain/Phase6 exact-HEAD CI, and inspect GitHub diff/readback. Integrate only the Work+test+tool+CI paths into `develop`. The original stacked PR #270/#274/#278 may be closed superseded after verifying all unique files/semantics were carried forward; never merge their old source snapshots wholesale. This is `issue-work/verification`, not `spec-implementation`. CAT Process Closure, Model/Implementation Conformance, confirmed product semantics, full settlement and real Paper behavior remain **not-run/unverified**.

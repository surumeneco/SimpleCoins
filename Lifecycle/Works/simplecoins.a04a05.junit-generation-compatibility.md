---
cat_version: '0.2'
kind: work
id: simplecoins.a04a05.junit-generation-compatibility
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
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

# A04/A05 — 公開済み最新CAT原本によるJUnit生成・義務状態の実再検証

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/CAT-TDD/commit/a6ff19bd0cfdb7a8b007f4df62e25f154acbe643 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner;simplecoins.external-storage-provider-integration | simplecoins.a04a05.junit-generation-compatibility | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Python 3 standard subprocess; canonical CAT released main; JUnit renderer; GitHub Actions |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04a05.junit-generation-compatibility.md;.github/workflows/cat-a04-a05-canonical-junit-generation.yml | — | .github/workflows/cat-a04-a05-canonical-junit-generation.yml |

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

## Technical question and bounded acceptance

The installed Chat-only CAT skill pins `CAT-TDD@896cae83`, whose compiler dispatches test generation to Vitest and returns `blocked` for Process observation/action capabilities. The **actually released** upstream `CAT-TDD main@a6ff19bd0cfdb7a8b007f4df62e25f154acbe643` includes canonical `cat_compile_v2.py tests --framework junit`, which accepts an exact `capability_scope.kind=pi-only-unverified` declaration. Do not treat the earlier pinned old version as a permanent product blocker.

- Fetch this immutable, separately verified upstream release. Validate this issue Work and both **already legitimately Confirmed** A04/A05 Work structures with the actual current canonical runner. Run script-owned `process-closure`, `model-conformance`, Model, deterministic Vectors, Queue, and implementation obligations on the current SimpleCoins HEAD.
- Invoke the **canonical** released CAT compiler directly with `tests --framework junit --queue` for the exact current Queue case for each A04/A05, using existing exact JUnit binding JSON. Reinvoke `--check` to prove deterministic output. No handwritten reinterpretation of the TCE is permitted.
- Require generated tests to disclose that the Process observations/actions are **NOT VERIFIED** by PI-only Boolean drivers. These test files remain CI artifacts, not tracked product sources or a completed Queue of all cases.
- Evaluate `implementation-conformance` and explicitly preserve `inconclusive` and any missing implementation/cross-process evidence. A generated Java test **is not** a JUnit Green or an implementation-conformance pass. Neither old pinned runtime nor CI success can silently upgrade a CAT Gate.
- The production plugin, A04/A05 Confirmed sources, A01/G0 and A02/G3 Drafts, journal, Admission, G0 publication, G3 ACK, `main` and VPS are outside the change scope. This work is only a machine-verifiable correction to **current** CAT technology compatibility, for subsequent legitimate formal TDD implementation and cross-process evidence.

## Stop condition

Succeed only when the released canonical generator actually emits one current A04 case and one current A05 case with exact provenance and `--check` equality, while formal unverified capabilities remain marked. Do not add another custom renderer, recreate the CAT upstream in this repo, or assert that all model behavior or the real G0/G3 coordinator is finished.

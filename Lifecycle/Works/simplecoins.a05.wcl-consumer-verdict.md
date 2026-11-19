---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wcl-consumer-verdict
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A05 — WWC/WWW収納証拠の採用・保留の純粋判定

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wcl-consumer-verdict | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wcl-consumer-verdict.md;tests/cat/a05-wcl-consumer-junit-binding.json;src/main/java/co/surumene/simplecoins/domain/WclStorageEvidenceDecision.java;src/test/java/co/surumene/simplecoins/domain/A05WclEvidenceDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/*;tools/run_cat_a05_junit.py;.github/workflows/a05-wcl-consumer-verdict.yml | src/main/java/co/surumene/simplecoins/domain/WclStorageEvidenceDecision.java | src/test/java/co/surumene/simplecoins/domain/A05WclEvidenceDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/*;tools/run_cat_a05_junit.py;.github/workflows/a05-wcl-consumer-verdict.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a05-green-s0 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s0 | case.s0 | — | — | 300 |
| a05-green-s1 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s1 | case.s1 | — | — | 300 |
| a05-green-s2 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s2 | case.s2 | — | — | 300 |
| a05-green-s3 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s3 | case.s3 | — | — | 300 |
| a05-green-s4 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s4 | case.s4 | — | — | 300 |
| a05-green-s5 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s5 | case.s5 | — | — | 300 |
| a05-green-s6 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s6 | case.s6 | — | — | 300 |
| a05-green-s7 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a05_junit.py {report} case.s7 | case.s7 | — | — | 300 |

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

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-wcl-consumer-verdict-model.json | @work/.cat-flow/cache/a05-wcl-consumer-verdict-vectors.json | @work/.cat-flow/cache/a05-wcl-consumer-verdict-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05WclEvidenceGeneratedTest.java | @work/.cat-flow/cache/a05-wcl-consumer-verdict-obligations.json | — | false |

## Acceptance
- The confirmed 8 boolean input observations and TCE s0–s7 are the only behavioral authority. Do not turn unknown inputs into a verified empty provider or a verified FREE owner.
- Output is one of exactly three results: adopt an **unpublished** candidate, continue only a positively verified non-target, or hold settlement. These outcomes are mutually exclusive and exhaustive for the local input snapshot.
- This Work implements the deterministic decision **after** independent provider/source/owner/generation evidence has been verified by the upstream collector. It does not claim to collect WCL ServicesManager providers, read saved PDC, discover all entities, prove writer quiescence, or publish settlement.
- Produce source-derived per-Queue JUnit tests and actual selected-case Red/Green; never fabricate Red for cases already Green on a baseline. Record independently performed formal CAT Gates, implementation obligations, source hashes and CI; merge only a safe independent source unit to develop.
- Admission, G0/G3, source collection and publish remain disabled/unverified.
- CAT-TDD `develop@381af68c91a1cb4c3a0b908d7edd660d87bbb49b` provides an **explicit, limited** `pi-only-unverified` JUnit binding for Process observation/action declarations. The binding enumerates all four observations and two actions but deliberately does **not** test or claim either capability. Generate the current-item source-derived JUnit from the Confirmed PI/TCE and test the real pure decision; perform separate runtime/source-backed capability integration and Implementation Conformance later. A generated PI-only Green is not proof of G0 generation, WCL provenance, writer quiescence or settled/publicable assets. Do not weaken or edit Confirmed observation/action declarations.

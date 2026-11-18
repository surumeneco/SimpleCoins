---
cat_version: '0.2'
kind: work
id: simplecoins.a05.live-loaded-reader
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

# A05 — 実登録WCLからロード済み個体の未公開収納証拠を読む

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md |
| https://github.com/surumeneco/SimpleCoins/pull/534 |
| https://github.com/surumeneco/SimpleCoins/pull/535 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.live-loaded-reader | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.a05.wcl-consumer-verdict |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.live-loaded-reader.md;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLoadedEvidence.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLoadedEvidence.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| live-reader-proof | green | shell | @repo:SimpleCoins | bash -c 'gradle --no-daemon test jar && gradle --no-daemon -p tests/paper-wcl-services-probe jar' | a05.loaded-reader | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java | — | 600 |

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
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-loaded-reader-model.json | @work/.cat-flow/cache/a05-loaded-reader-vectors.json | @work/.cat-flow/cache/a05-loaded-reader-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05LoadedReaderGeneratedTest.java | @work/.cat-flow/cache/a05-loaded-reader-obligations.json | — | false |

## Acceptance and safe boundary

- This is an **independent read-only collection adapter** for already-loaded Paper entities, not the full confirmed A05 candidate-adoption decision or the higher-level G0 generation guarantee.
- Use an actual validated WCL/WWC/WWW registration. Resolve the loaded entity without loading/generating chunks, invoke `readLoaded` strictly on Paper's main thread, distinguish `Success`, `NotFound`, `Failure`, unavailable and identity mismatch. Return no candidate for any non-success result.
- For `Success`, compare entity UUID, world UUID and provider ID; copy and preserve slot-bearing serialized items and relationship evidence without monetary decoding, owner inference or any write.
- Use the existing pinned distribution JAR Paper probe, not newly fabricated service implementations. Validate actual standalone class Red/Green and actual Paper runtime success.
- Current canonical JUnit renderer does not support the Confirmed Process's observation/action capabilities; no source-derived JUnit/Queue Green or full Implementation Conformance is claimed for this bounded collector. Record that blocked formal step and preserve the wider Work unchanged.
- Never call this result a confirmed settlement, empty-world proof, G0/G3 proof, or player admission permission.

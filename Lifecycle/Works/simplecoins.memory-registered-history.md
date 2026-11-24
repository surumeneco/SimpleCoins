---
cat_version: '0.2'
kind: work
id: simplecoins.memory-registered-history
status: draft
process: simplecoins.accounting.memory-journal
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view#プレイヤー登録条件と統計対象
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.memory-accounting
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/5
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# Preserve registered UUIDs across reconstruction

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.memory-journal | simplecoins.memory-registered-history | simplecoins.accounting.memory-journal.pi.record |

## Technologies
| Technology |
| --- |
| Java; atomic local replacement of UUID history; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.memory-registered-history.md;src/main/java/co/surumene/simplecoins/domain/RegisteredPlayerHistory.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/domain/MemoryAccounting.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/RegisteredPlayerHistoryTest.java | src/main/java/co/surumene/simplecoins/domain/RegisteredPlayerHistory.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/domain/MemoryAccounting.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/RegisteredPlayerHistoryTest.java |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-process.md | @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-pi.md | @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-tce.md | — | — | — | @work/.cat-flow/cache/memory-registration-model.json | @work/.cat-flow/cache/memory-registration-vectors.json | @work/.cat-flow/cache/memory-registration-queue.json | @repo:SimpleCoins/tests/cat/memory-accounting-junit-binding.json | @work/.cat-flow/generated/GeneratedMemoryAccountingTest.java | @work/.cat-flow/cache/memory-registration-obligations.json | — | false |

## Acceptance

02A/02B distinguish previously registered UUIDs from all players who have joined. Persist only the UUID history (no balances). At full reconstruction merge previously registered UUIDs at zero only when the same source Manifest contains their playerdata and complete current BAN evidence says clear. Retain positive physical balances. Banned or missing players remain excluded from the active set; historical UUIDs do not confer current eligibility. Missing history is new installation; malformed existing history is an explicit startup failure. Remember new active UUIDs after a successfully confirmed memory operation and before exposing successful startup. A duplicate/rejected operation does not write. A history IO failure invalidates the runtime rather than publishing an inaccurate result. No per-operation balance WAL, no registration from login alone.

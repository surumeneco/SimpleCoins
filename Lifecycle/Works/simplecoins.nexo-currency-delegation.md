---
cat_version: '0.2'
kind: work
id: simplecoins.nexo-currency-delegation
status: draft
process: simplecoins.display.java-pack-delivery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/6
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#外部連携と非機能要件
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# Nexo optional currency display delegation

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/6 |
| https://github.com/surumeneco/SimpleCoins/issues/6 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.display.java-pack-delivery | simplecoins.nexo-currency-delegation | simplecoins.display.java-pack-delivery.pi.request;simplecoins.display.java-pack-delivery.tce.request;simplecoins.settlement |

## Technologies
| Technology |
| --- |
| Java; JUnit; Paper |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.nexo-currency-delegation.md;src/main/java/co/surumene/simplecoins/domain/NexoResourceInstaller.java;src/main/java/co/surumene/simplecoins/domain/ManagedResources.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/NexoResourceInstallerTest.java | src/main/java/co/surumene/simplecoins/domain/NexoResourceInstaller.java;src/main/java/co/surumene/simplecoins/domain/ManagedResources.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/NexoResourceInstallerTest.java |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| real-use | not-run | — |

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/java-pack-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/java-pack-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/java-pack-delivery-tce.md | — | — | — | @work/.cat-flow/cache/nexo-model.json | @work/.cat-flow/cache/nexo-vectors.json | @work/.cat-flow/cache/nexo-queue.json | @repo:SimpleCoins/tests/cat/java-pack-delivery-junit-binding.json | @work/.cat-flow/generated/NexoDelegationGeneratedTest.java | @work/.cat-flow/cache/nexo-obligations.json | — | false |

## Acceptance
- Reuse approved 03/05 and Confirmed Java delivery decision; Nexo owns merged pack generation and distribution. Use documented external pack ZIP under the enabled plugin data directory (https://docs.nexomc.com/configuration/resourcepack); preserve nine fixed models and selected images without registering a second Java delivery listener.
- Nexo enabled: generate input assets, stage exactly the Java archive to its external_packs path, request documented Nexo pack reload on changed content after startup, and remove only prior SimpleCoins-owned Geyser outputs. Unchanged input does not request reload. Never overwrite an unrelated or edited import. Nexo absent: existing direct Java/Geyser path remains unchanged. No admission or currency identity change.
- Verify generated nine definitions, byte-identical transfer, unchanged/changed re-stage, unrelated files and modified import refusal; actual Nexo merge, client JE/BE delivery and Scaffolding conversion remain external observations.

---
cat_version: '0.2'
kind: work
id: simplecoins.v2.currency-pack-generation
status: draft
process: simplecoins.presentation.currency-pack
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.v2.currency-pack-generation

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2 |
| https://github.com/surumeneco/SimpleCoins/tree/develop |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.currency-pack | simplecoins.v2.currency-pack-generation | simplecoins.currency.pdc-stable;simplecoins.settlement.default-deny |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 (resource pack format 88.0); ZIP; Java2D PNG; Geyser custom mappings v2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/domain/JavaCurrencyPack.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/JavaCurrencyPackTest.java;src/test/java/co/surumene/simplecoins/paper/CurrencyPackLiveProbe.java;tests/paper-pack-smoke/*;build.gradle.kts;.github/workflows/paper-currency-pack-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/JavaCurrencyPack.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/JavaCurrencyPackTest.java;src/test/java/co/surumene/simplecoins/paper/CurrencyPackLiveProbe.java;tests/paper-pack-smoke/*;build.gradle.kts;.github/workflows/paper-currency-pack-live-smoke.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Source-grounded design boundary

- Adopted SimpleCoins 1.0.0 currency names / stable PAPER PDC IDs and `simplecoins:coin_<denomination>` item models are authoritative. One PNG per denomination, default denomination-suffixed names (`1 Coin.png`, etc.), custom display names, normalization of spaces/underscore/hyphens and collision errors must not change the fixed item model paths.
- Existing `BedrockPack.write`, `CoinImageNames.uniquePng`, `PackImageSource` and `PackArchivePublication` in verified `develop` are reusable; don't duplicate unsafe old scripts. Official Java 26.2 client resource-pack format is 88.0, confirmed by Mojang release note. Unshipped WIP `JavaPackBuilder` in `feature/initial-domain-20261027` is historical reference only.
- This unit creates default PNG image files ONLY when missing and only for unrenamed default denomination display names; never overwrite user images or guess a renamed denomination's missing image. Validate image format/content and reject symlinks/path traversal and normalized ambiguous names. Generate JE ZIP with nine model/definition/texture triples and matching Bedrock pack/mapping under the plugin's private data directory; don't publish to Geyser or send resource packs in this unit.
- Startup may build artifacts only after journal verification and config validation, keeping default-deny admission. If Nexo is detected, avoid self-generation and do not assume delegated Geyser conversion exists. Existing JE/BE resource pack routes remain to be finished as separate units.
- JUnit tests cover default 9 PNGs, exact ZIP members, PNG decoding, rewritten custom display names, ambiguity, missing renamed PNG, traversal/symlink safety and no monetary ID mutation. Separate isolated Paper 26.2 CI must prove generated archives actually exist under guarded bootstrap and not in production JAR.
- This is an issue-work Draft. CAT normative Process/PI/TCE source confirmation, generated model/vectors and implementation conformance remain not-run. JE/BE clients, Nexo/Geyser deployment, hosting/HTTP URL, wallet imagery, world settlement, and monetary journal finality remain not-run.

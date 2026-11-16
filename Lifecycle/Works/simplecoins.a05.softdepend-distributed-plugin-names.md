---
cat_version: '0.2'
kind: work
id: simplecoins.a05.softdepend-distributed-plugin-names
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/WhatAWonderfulChicken/blob/develop/src/main/resources/plugin.yml
  - https://github.com/surumeneco/WhatAWonderfulWolf/blob/develop/src/main/resources/plugin.yml
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A05 — 配布WCL/WWC/WWWの実plugin名でsoftdependを宣言する

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/439 |
| https://github.com/surumeneco/SimpleCoins/pull/458 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/.github/workflows/paper-wcl-services-probe.yml |
| https://github.com/surumeneco/WhatAWonderfulChicken/blob/develop/src/main/resources/plugin.yml |
| https://github.com/surumeneco/WhatAWonderfulWolf/blob/develop/src/main/resources/plugin.yml |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.softdepend-distributed-plugin-names | simplecoins.a05.paper-stored-currency-providers;simplecoins.grants.startup-persistent-verification |

## Technologies

| Technology |
| --- |
| Paper 26.2 official 134; Java 25; four exact pinned server JAR blobs; plugin.yml softdepend and plugin-loader order |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.softdepend-distributed-plugin-names.md;src/main/resources/plugin.yml;.github/workflows/paper-wcl-services-probe.yml | src/main/resources/plugin.yml | .github/workflows/paper-wcl-services-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| actual-provider-manifest | regression | junit | @repo:SimpleCoins | gradle test | pinned JAR plugin.yml names; real Paper services | verified four provider JARs; production plugin.yml | GitHub CI log | 480 |

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

## Previously decided contract and bounded correction

- The accepted Drive 02B design requires SimpleCoins to integrate WCL-backed WWC/WWW stored asset providers where present without introducing a mandatory hard dependency or treating unknown provider state as zero. `softdepend` on real Paper plugin identities must permit predictable provider load order while allowing SimpleCoins to boot if a provider is absent.
- Existing product `plugin.yml` lists short aliases WWC, WWW, WCL, while the actual WWC/WWW plugin descriptors and WWC/WWW `depend` already name WhatAWonderfulChicken, WhatAWonderfulWolf and WonderfulCoreLibrary. Manifest names are plugin-loader identities, not Maven/Java package aliases.
- Test first: on the isolated Paper provider integration workflow that pins four real released JAR blobs by Git hash, unzip each actual `plugin.yml`, require the exact descriptor `name:` to appear in SimpleCoins' `softdepend` lines. The present aliases must fail the new check. Then correct only the three names in the production manifest, preserving Geyser/Nexo/BlockLocker and `depend: SimpleCoinsGuard`.
- Verify final-JAR descriptor, all three name checks, guarded Paper boot, actual WWC/WWW provider registration/reading, and no admission opening. No hard dependency, version bump, API semantics change, player data mutation, full G0 or CAT normative gate completion.

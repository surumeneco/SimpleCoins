---
cat_version: '0.2'
kind: work
id: simplecoins.a01.actual-loaded-writer-roster
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
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

# A01/G0・A02/G3 — 実Paper登録Pluginと配布JARの関係の読み取り

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/477 |
| https://github.com/surumeneco/SimpleCoins/pull/482 |
| https://github.com/surumeneco/SimpleCoins/pull/486 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit | simplecoins.a01.actual-loaded-writer-roster | simplecoins.a01.packaged-writer-entrypoints;simplecoins.a01.installed-writer-roster |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java25; packaged XPlayServer 90aa2d0e plugin JARs; live Bukkit PluginManager and ProtectionDomain CodeSource; SHA256 of actual loaded JVM codeSource; pinned upstream CAT issue-work validator |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.actual-loaded-writer-roster.md;tests/paper-runtime-writer-roster-probe/*;.github/workflows/paper-runtime-writer-roster-probe.yml | — | tests/paper-runtime-writer-roster-probe/*;.github/workflows/paper-runtime-writer-roster-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | 

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

## Justification and bounded technical claim

Issue #423 A01/G0 and A02/G3 require every asset writer to be included under an enforceable captured/live/saved generation contract before publication and grant ACK. The existing [#477](https://github.com/surumeneco/SimpleCoins/pull/477) 24-packaged-JAR bytecode inventory and [#482](https://github.com/surumeneco/SimpleCoins/pull/482) exact directory SHA256 comparison distinguish static candidate bytecode from installed binaries, but **do not prove which Paper plugins actually start or are enabled**. A missing/disabled plugin, unexpected code source or transformed/remapped source can make that static analysis inapplicable.

This independent investigation starts official Paper 26.2 build134 with real packaged 5-JAR subset drawn from pinned `MineCraftXPlayServer@90aa2d0e0a5ea80c705fd67d717f463decaf2a32`: InvSee++.jar, WonderfulGenomeLib-1.0.1.jar, WonderfulCoreLibrary-0.1.0.jar, WhatAWonderfulChicken-2.1.0.jar and WhatAWonderfulWolf-1.1.1.jar, plus actual SimpleCoins and independent default-deny admission guard. Compare expected plugin IDs extracted from real JAR `plugin.yml` descriptors against the live `Bukkit.getPluginManager().getPlugins()` registry **after enable**. Record each enabled plugin's version and actual JVM `ProtectionDomain.CodeSource` basename, SHA256 and location category; Paper may remap plugins into a distinct runtime copy, so DO NOT assert the codeSource SHA is identical to its original distribution JAR unless byte equality is measured.

The probe is isolated, read-only and fail-closed: loopback-only world, zero players, independent admission guard active; explicitly require all five pinned plugin IDs enabled and both SimpleCoins plus guard enabled. It emits one marker per actual ID with CodeSource hash and an aggregate marker after all checks. Never infer an actual plugin's writer method was executed from its existence, and do not infer that all **24** distribution JARs or the actual VPS were loaded from this five-plugin test. The test plugin is not part of the product JAR.

## Acceptance and non-claims

- Verify pinned server commit and exact five Git blob SHA; parse five real plugin descriptors and dynamically generate expected IDs; no hand-guessed plugin name counts.
- Boot official SHA256-verified Paper 26.2, require enabled live PluginManager IDs and real CodeSource each for all five, SimpleCoins and guard. Observe actual hashes and registered versions, no login or asset changes; all usual Domain/Phase6 regressions still green.
- Work validated with canonical CAT issue-work script; all formal CAT gates not-run.
- An enabled-plugin roster is **not** writer path completeness, dynamic class loading or application quiescence, actual VPS inventory, G0/G3, permission to ACK, or permission to open admission. Follow-up must compare complete intended distro/current VPS and enforce writer critical sections; do not stop with merely cataloguing names.

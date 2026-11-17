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
| Official Paper 26.2 build 134; Java25; packaged XPlayServer 90aa2d0e plugin JARs (previous five-plugin positive control and full 24-JAR observation); live Bukkit PluginManager and ProtectionDomain CodeSource; SHA256 of actual loaded JVM codeSource; pinned upstream CAT issue-work validator |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.actual-loaded-writer-roster.md;tests/paper-runtime-writer-roster-probe/*;.github/workflows/paper-runtime-writer-roster-probe.yml;.github/workflows/paper-runtime-24-writer-roster-probe.yml | — | tests/paper-runtime-writer-roster-probe/*;.github/workflows/paper-runtime-writer-roster-probe.yml;.github/workflows/paper-runtime-24-writer-roster-probe.yml |

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

The established five-plugin positive control starts official Paper 26.2 build134 with real packaged 5-JAR subset drawn from pinned `MineCraftXPlayServer@90aa2d0e0a5ea80c705fd67d717f463decaf2a32`: InvSee++.jar, WonderfulGenomeLib-1.0.1.jar, WonderfulCoreLibrary-0.1.0.jar, WhatAWonderfulChicken-2.1.0.jar and WhatAWonderfulWolf-1.1.1.jar, plus actual SimpleCoins and independent default-deny admission guard. Compare expected plugin IDs extracted from real JAR `plugin.yml` descriptors against the live `Bukkit.getPluginManager().getPlugins()` registry **after enable**. Record each enabled plugin's version and actual JVM `ProtectionDomain.CodeSource` basename, SHA256 and location category; Paper may remap plugins into a distinct runtime copy, so DO NOT assert the codeSource SHA is identical to its original distribution JAR unless byte equality is measured.

The probe is isolated, read-only and fail-closed: loopback-only world, zero players, independent admission guard active; explicitly require all five pinned plugin IDs enabled and both SimpleCoins plus guard enabled. It emits one marker per actual ID with CodeSource hash and an aggregate marker after all checks. Never infer an actual plugin's writer method was executed from its existence, and do not infer that all **24** distribution JARs or the actual VPS were loaded from this five-plugin test. The test plugin is not part of the product JAR.

## Acceptance and non-claims

- Verify pinned server commit and exact five Git blob SHA; parse five real plugin descriptors and dynamically generate expected IDs; no hand-guessed plugin name counts.
- Boot official SHA256-verified Paper 26.2, require enabled live PluginManager IDs and real CodeSource each for all five, SimpleCoins and guard. Observe actual hashes and registered versions, no login or asset changes; all usual Domain/Phase6 regressions still green.
- Work validated with canonical CAT issue-work script; all formal CAT gates not-run.
- An enabled-plugin roster is **not** writer path completeness, dynamic class loading or application quiescence, actual VPS inventory, G0/G3, permission to ACK, or permission to open admission. Follow-up must compare complete intended distro/current VPS and enforce writer critical sections; do not stop with merely cataloguing names.

## 24-JAR runtime population follow-up: observation without fabricated closure

The 5-JAR positive control in [#496](https://github.com/surumeneco/SimpleCoins/pull/496) proved 5/5 actual enabled `CodeSource` byte-equivalent to the fixed distribution. [#503](https://github.com/surumeneco/SimpleCoins/pull/503) audited all 24 exact packaged JARs (48,226 classes), including previously unclassified writable-file APIs: this is only a symbolic reference catalog. For writer coverage assessment, the next bounded question is whether **those same 24 JARs actually load and enable together** in one official Paper 26.2 build134 disposable JVM.

Reuse the existing test-only `RuntimeWriterRosterProbe` with **two explicit modes**. The existing default requires exactly five and must keep all existing `#496` test markers, enabled IDs and source SHA assertions. An opt-in `-Da01.runtimeRoster.distributionObservation=true` mode takes **exactly 24 unique IDs** parsed at runtime from *actual* packaged `plugin.yml` / `paper-plugin.yml` entries, still requires real SimpleCoins + independent default-deny guard and no online players. It records **enabled, disabled, missing and unexpected live plugin IDs separately** and, for each enabled plugin, actual JVM code-source path, SHA256 and size. The probe must not abort merely because a packaged plugin could not enable: such a result is important evidence of a **coverage gap**, and must not be converted to `0 writers`. But it **must fail** if it cannot enumerate the registry, load guard/product, read loaded CodeSource, or determine all configured 24 IDs.

The second CI requires official SHA-pinned Paper 26.2 build134, `MineCraftXPlayServer@90aa2d0e` exactly 24 packaged JARs copied as-is (new isolated plugins folder), actual independent guard and SimpleCoins, no connected players, TCP loopback, and the test-only observer. Verify source names and hashes for **every enabled** plugin against the exact distribution JAR, distinguishing an actual remapped CodeSource (not byte-identical) rather than silently claiming equality. Require a total outcome line with all expected and actual IDs and total counts. **A completed observation with some missing/disabled plugins is still a technical investigation success, not complete writer coverage**, and produces an explicit `NOT_ALL_ENABLED` marker. A fully enabled 24/24 subset would still not establish all reachable monetary asset writers, asynchronous quiescence, runtime VPS identity, Paper core/OS writes or the G0/G3 epoch boundary.

The two CI scenarios operate on distinct disposable Paper JVMs and never run on the real server. No product Java/config/world/account/grant/ACK/admission enablement or main change is permitted. Existing 5-JAR regression, Domain and Phase6 checks must pass at the same final HEAD; CAT Work status remains Draft with no normative Gate claim. Report actual enabled/missing/disabled and CodeSource mismatches to #423 rather than manufacture a passing 24/24 assertion.

---
cat_version: '0.2'
kind: work
id: simplecoins.v2.geyser-managed-pack-staging
status: draft
process: simplecoins.presentation.geyser-pack-staging
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

# Work: simplecoins.v2.geyser-managed-pack-staging

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://geysermc.org/wiki/geyser/custom-items/ |
| https://github.com/surumeneco/SimpleCoins/pull/406 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.geyser-pack-staging | simplecoins.v2.geyser-managed-pack-staging | simplecoins.settlement.default-deny;simplecoins.currency.pdc-stable |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; Geyser-Spigot; Geyser Custom Item Mappings v2; SHA-256 inventory |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/domain/GeyserResourceInstaller.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/GeyserResourceInstallerTest.java;.github/workflows/paper-geyser-managed-smoke.yml;src/test/java/co/surumene/simplecoins/paper/GeyserManagedLiveProbe.java;tests/paper-geyser-managed-smoke/*;build.gradle.kts | src/main/java/co/surumene/simplecoins/domain/GeyserResourceInstaller.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/GeyserResourceInstallerTest.java;.github/workflows/paper-geyser-managed-smoke.yml;src/test/java/co/surumene/simplecoins/paper/GeyserManagedLiveProbe.java;tests/paper-geyser-managed-smoke/*;build.gradle.kts |

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
| ci | passed | [Paper fake-Geyser provider staging](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842541); [Java/JUnit](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842389); [prior currency packs](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842380); [guarded startup](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842530); [BlockLocker](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842420); [profile BAN](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842478); [wallet](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842412); [coin](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842391); [static](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842487) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Observed source-backed implementation and CI

- Test-first head `a75cf209a8a0388034670b6165ba021481a230d7`: Java/JUnit [CI #43585582475](https://github.com/surumeneco/SimpleCoins/actions/runs/43585582475) failed compilation because `GeyserResourceInstaller` did not exist. This is ordinary source-observed Red, not CAT-generated normative Red.
- Current implementation uses only Geyser-Spigot's *actual Bukkit provider object* for a data folder; it never guesses a server path. When Nexo is absent, SimpleCoins verifies `gameplay.enable-custom-content=true`, then uses the pre-existing `ManagedResources` hash-owned manifest to stage only `packs/simplecoins-generated.mcpack` and `custom_mappings/simplecoins-generated.json` from the private #406 archives. A changed copy requires **a subsequent Geyser restart** to be loaded.
- When Nexo is enabled, SimpleCoins does not create/deliver a second Java/Bedrock pack and removes only verified previously managed Geyser files. JUnit confirms no unowned/modified Geyser pack is overwritten, invalid sources fail, re-staging is idempotent with respect to contents, Nexo cleanup preserves other plugins' packs.
- [Actual isolated Paper 26.2 build 134 #43585842541](https://github.com/surumeneco/SimpleCoins/actions/runs/43585842541) passed at code head `6c144ebbf231cc4846fe3f23eebbb74131f54d54`. A **test-only stub named Geyser-Spigot** supplied the Bukkit plugin/data-folder contract and `gameplay.enable-custom-content=true`; it is NOT a real Geyser engine. Server log `SIMPLECOINS_GEYSER_PACK_STAGED: changed=true`, then filesystem byte checks against generated pack and mapping with managed manifest, yielded `PAPER_MANAGED_GEYSER_STAGING_PASSED`; independent guard denied admission throughout.
- All thirteen CI families succeeded on that code head: Paper resource generation, fake-Geyser integration, guarded startup, journal/history/directory controls, owner/BAN probes, saved discovery, coin/wallet, JUnit and static verifier. This is *not* Geyser custom content loaded or actual BE client delivery.
- CAT normative gates, actual Geyser-Spigot JAR, Nexo/Scaffolding delegation and resource reload, Java URL hosting and client receipt, JE/BE visual correctness, physical settlement and financial finality remain **not-run**.

## Observed constraints and staged implementation boundaries

- The user-approved 01/03 specify SimpleCoins-owned JE and Bedrock generated content when Nexo absent, and Nexo-owned presentation when present. Geyser does **not** convert JE resource packs automatically. Geyser Custom Items v2 mapping JSON belongs in the Geyser-Spigot `custom_mappings` folder, custom Bedrock pack in `packs`, and both require a **Geyser restart** before use; `enable-custom-content` must be true according to Geyser docs.
- Merged PR #406 generates private staging under `plugins/SimpleCoins/geyser` only; this Work may install those artifacts using the existing `ManagedResources` identity/hash manifest to the **actual plugin's reported Geyser data folder**, never a guessed path. Refuse altered/unowned managed paths, symlinks, missing archives, oversized files, a false custom-content configuration and malformed mapping. Distinguish a newly installed source file from a confirmed Geyser load and never claim same-boot activation.
- With Nexo present, SimpleCoins must not double-install/serve its own pack. Clean up only its own verified previously managed files when safe; do not delete other plugins' resources. Nexo/Scaffolding delivery acceptance stays a separate unverified dependency.
- Keep the independent admission guard closed. A dummy test plugin or a source-level Paper integration test can exercise provider path/manifest handling, but MUST NOT claim the real Geyser-Spigot plugin has loaded and converted custom content without its actual JAR and a BE client.
- This `issue-work` Draft is not a confirmed Process/PI/TCE or CAT semantic authority; actual JE/BE visual tests, provider reload, external URL, physical currency accounting and atomic source snapshots remain not-run.

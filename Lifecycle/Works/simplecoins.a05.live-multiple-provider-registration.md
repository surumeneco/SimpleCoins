---
cat_version: '0.2'
kind: work
id: simplecoins.a05.live-multiple-provider-registration
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A05 — PaperでのWWC/WWW同時StorageReader登録と失敗伝播の検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/Lifecycle/Works/simplecoins.a05.packaged-storage-provider-api-audit.md |
| https://github.com/surumeneco/WonderfulCoreLibrary/blob/develop/wcl-plugin/src/main/java/co/surumene/wcl/plugin/inventory/WonderfulStorageReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.live-multiple-provider-registration | simplecoins.a05.packaged-storage-provider-api-audit |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; pinned WGL/WCL/WWC/WWW release-JAR bytecode; Bukkit ServicesManager |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.live-multiple-provider-registration.md;tests/paper-wcl-services-probe/*;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/*;.github/workflows/paper-wcl-services-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43731215531 (pinned distributed JARs on real Paper 26.2, Work structure only) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Technical acceptance and stop conditions

- The prior packaged-JAR ABI audit confirms the relevant class files and signatures in the pinned `MineCraftXPlayServer@90aa2d0e0a5ea80c705fd67d717f463decaf2a32` distribution. This Work verifies a separate **runtime service-registration fact**, not the current VPS/JAR deployment.
- Boot official Paper 26.2 build 134 on loopback with independent admission guard and no online players. Use only those same pinned release JARs: WGL 1.0.1, WCL 0.1.0, WWC 2.1.0, WWW 1.1.1, and SimpleCoins default-deny product (the latter is not a settlement engine).
- Require every expected plugin to be enabled before the probe. Use `Bukkit.getServicesManager().getRegistrations(WonderfulStorageReader.class)` rather than `load()` (which would hide a second provider); require exactly one `providerId=wwc` registered under WWC and exactly one `providerId=www` under WWW, no others, all enabled and identity-distinct.
- For a fresh random entity UUID in the loaded world, both `readLoaded` calls should return explicit `NotFound`, not a fabricated empty Success, Failure or exception. For a blank independent Paper PDC, both `readStored` calls should return explicit `NotFound`; this verifies only a deliberately absent entity case.
- Report whether full server boot, plugin enablement and both read calls succeed under the pinned packaged versions. If they do not, preserve the log and diagnose the dependency/configuration mismatch. Never convert failure to zero monetary assets or claim that a missing/disconnected provider is benign.
- This stage does **not** scan actual occupied wolf/chicken storage, parse offline entity NBT, prove nested Shulker/Bundle accounting, guarantee full provider source discovery or open any admission. Existing 02B owner/BAN rules are unchanged.

## Authority

`issue-work` source-backed verification Draft only; no production source, connected real players, actual VPS, monetary writer, CAT normative Confirmed or formal Gates.

## Real Paper 26.2 observed result (2026-11-15)

- CI [#43731215531](https://github.com/surumeneco/SimpleCoins/actions/runs/43731215531) ran official Paper 26.2 build 134 with independent default-deny admission and zero connected players. It loaded pinned server-repository JAR blobs for WGL 1.0.1, WCL 0.1.0, WWC 2.1.0 and WWW 1.1.1 plus SimpleCoins and a separate disposable probe.
- Both actual Bukkit services registered under the **same WCL `WonderfulStorageReader` class identity**. `Bukkit.getServicesManager().getRegistrations` enumerated exactly two distinct provider registrations, `wwc` and `www`, each owned by the correctly enabled plugin: `PAPER_A05_BOTH_PACKAGED_STORAGE_SERVICES_REGISTERED_PASSED providers=[wwc, www]`.
- For an unknown entity UUID, each provider returned explicit `StorageReadResult.NotFound` from `readLoaded` and from `readStored` when supplied a genuinely blank Paper PDC. This verifies absent-entity semantics only; missing live/offline **expected** individuals must still be treated as incomplete coverage.
- This is a direct Paper packaging/startup verification. It supersedes an unfounded assumption that the two existing APIs cannot coexist. It does **not** verify entity-specific snapshots, ownership fields, nested item monetary decoding, offline PDC discovery or synchronization with saved-world and BAN evidence. Current VPS-installed JAR identity remains unverified.
- Pinned `cat_flow.py validate` reported `contract_valid:true`, `semantic_approval_verified:false`, `status:passed`. No normative CAT Process/Model/Implementation Gate passed.

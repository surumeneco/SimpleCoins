---
cat_version: '0.2'
kind: work
id: simplecoins.a05.packaged-storage-provider-api-audit
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

# A05 — 現行サーバー配布JARの読取サービス実装検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/MineCraftXPlayServer/tree/develop/plugins |
| https://github.com/surumeneco/WonderfulCoreLibrary/blob/develop/wcl-plugin/src/main/java/co/surumene/wcl/plugin/inventory/WonderfulStorageReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.packaged-storage-provider-api-audit | simplecoins.o0.blocklocker-115-live-owner-evidence |

## Technologies

| Technology |
| --- |
| Java 25 JAR descriptor / javap; pinned MineCraftXPlayServer distributed JAR blob bytes; GitHub Actions |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.packaged-storage-provider-api-audit.md;.github/workflows/paper-packaged-storage-provider-api-audit.yml | — | .github/workflows/paper-packaged-storage-provider-api-audit.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

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

## Verification scope

- Treat pinned server repository Git blobs, actual deployed VPS JARs, and newest source code as **three different states**. This Work checks the pinned `surumeneco/MineCraftXPlayServer@90aa2d0e0a5ea80c705fd67d717f463decaf2a32` tracked plugin JAR files, not the VPS filesystem.
- Inspect `WonderfulCoreLibrary-0.1.0.jar`, `WhatAWonderfulChicken-2.1.0.jar` and `WhatAWonderfulWolf-1.1.1.jar` for the shared `WonderfulStorageReader` class and both concrete providers, and inspect actual bytecode signatures for `providerId`, `readLoaded` and `readStored`. The presence of a source class alone does not prove that an older distributable contains it.
- Read real `plugin.yml` descriptors to distinguish dependency and plugin load ordering. A packaged class and descriptor are not proof that the service successfully registers in live Paper; a separate real registration test is still required if matching classes exist.
- If any required class/signature is absent or incompatible, report the concrete distribution gap, do **not** automatically treat an unavailable provider as an empty asset source, and plan the update as a separate version-controlled unit. Do not overwrite installed JARs or change user deployment in this Work.
- Guard all outputs to exact pinned blob identities and report which branch was checked. Check other upstream API signatures only when they affect the identified mismatch.

## Authority

`issue-work` verification only, no production source/code or actual plugin deployment. Formal CAT gates not-run, no self-Confirmed, no monetary publication and no admission change.

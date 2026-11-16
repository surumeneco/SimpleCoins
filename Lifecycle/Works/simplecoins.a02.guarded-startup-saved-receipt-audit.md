---
cat_version: '0.2'
kind: work
id: simplecoins.a02.guarded-startup-saved-receipt-audit
status: draft
process: simplecoins.grants.startup-guarded-evidence
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A02 — 起動時の保存済み発行受領ユニバース監査

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/462 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/Lifecycle/Works/simplecoins.grants.saved-receipt-universe-audit.md |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.startup-guarded-evidence | simplecoins.a02.guarded-startup-saved-receipt-audit | simplecoins.grants.saved-receipt-universe-audit;simplecoins.grants.startup-persistent-verification |

## Technologies

| Technology |
| --- |
| Paper 26.2 build 134; Java 25; persisted GrantLedger; Paper saved Manifest; GZIP NBT; default-deny |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.guarded-startup-saved-receipt-audit.md;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;.github/workflows/paper-guarded-bootstrap-smoke.yml;tests/paper-guarded-bootstrap/SeedAcknowledgedWithoutPlayerdata.java | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java | .github/workflows/paper-guarded-bootstrap-smoke.yml;tests/paper-guarded-bootstrap/SeedAcknowledgedWithoutPlayerdata.java |

## Checks

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
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Previously decided requirements and bounded implementation

- Existing Drive 03/02B decisions: a completed grant is physical currency only after actual saved inventory and receipt agree; a missing/rolled-back saved player file for a journal-delivered grant must not be accepted as an empty current balance or trigger duplicate issuance.
- At startup SimpleCoins already opens an authentic durable GrantLedger before saved-source discovery. After A01 PR #462, PaperSavedSourceDiscovery captures the level-shared Paper playerdata sources in its saved Manifest. The independent read-only SavedGrantReceiptUniverseAudit.scan exists but is not connected to that one guarded bootstrap.
- Test first in an isolated actual Paper 26.2 JVM: an authentic ACKed journal record created by the real GrantLedger with **no corresponding playerdata** must cause product-driven safe Paper shutdown while the separate admission guard stays default-deny. This is distinct from corrupting the journal head. Normal empty issuance journal and valid saved playerdata continue to boot guarded, without opening admission.
- Bind the existing read-only audit as part of the saved-source asynchronous worker **before** publishing even an UNPUBLISHED source inventory. The audit runs with bounded compressed/inflated NBT reads (16 MiB/64 MiB per player), uses its existing SHA-256/ctime content witness and discovery provenance, and does not use Bukkit API off-thread. Any failure goes through existing main-thread safe-stop. A watchdog timeout also remains fail-stop. Do not create, acknowledge or deliver any grant in the product.
- The matched report is observational only. No G0 same-generation settlement, true exactly-once delivery, OS power-loss durability, inventory mutation, stats publication or admission release is claimed. Current CAT formal Gates and Confirmed status remain unverified; an independently green CI is not that evidence.

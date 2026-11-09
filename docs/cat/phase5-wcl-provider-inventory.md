---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.wcl-provider-inventory
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — WCL provider inventory authority (CAT Draft)

This is the proposed **SimpleCoins consumer contract** for the WCL `WonderfulStorageReader` service, not an implementation or a Confirmed Process/PI/TCE. The source-verified upstream API and provider lifecycle are recorded in `docs/verification/phase5-wcl-provider-contract.md`; this Draft only expresses the consumer decisions and testable acceptance boundaries. A provider's successful response does not prove that the caller's loaded/offline world catalog is complete.

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.owner.wcl-registration | Complete, authoritative WWC/WWW service registration inventory before owner and storage accounting |
| simplecoins.owner.wcl-result | Distinguish verified empty provider storage from missing, failed, stale or wrong-identity evidence |

## Existing decision provenance and unresolved integration design

The following is a **Draft provenance mapping**, not CAT normative promotion. Source locations are 1-based physical Markdown lines; SHA-256 identifies the actual raw Drive versions read for this mapping, not an approved CAT Artifact revision.

| Source | SHA-256 (raw UTF-8 bytes) | Existing design decisions used here |
| --- | --- | --- |
| [02A / Drive `1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g`](https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view) | `0c69fa2a94d3064484f805ee33840348b091d936a20034f9f03f61f1aaf8bccc` | L25–31: WWC/WWW commander → tame owner → FREE only when candidates are established; L37–40: current BAN/playerdata determines DEAD versus PERSONAL |
| [02B / Drive `1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8`](https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view) | `a435aa5f0a6eb1850d71e1482e26a5d38582a27d1fa1da3a82c48d5626234aef` | L54–58: full saved/loaded scan, failure remains incomplete and is never published as zero; L27: settlement replaces the prior cached estimate only after authoritative scan |

| WCL Draft rule | Existing decision authority | Additional proposal requiring separate CAT review/confirmation |
| --- | --- | --- |
| WCL-CONSUMER-01 / -03 / -04 | 02A L31 and 02B L54–58 require complete, authoritative WWC/WWW accounting | Enumerating **all** Bukkit `RegisteredServiceProvider` entries, checking duplicate provider IDs, registration owner and enabled state are implementation-specific safeguards inferred from the inspected WCL develop API; these mechanics are not explicitly user-confirmed in 02A/02B |
| WCL-CONSUMER-02 | 02B L57 and 02A L31 prohibit interpreting uninspected assets as FREE or zero | The exact required set and deployed plugin version/JAR identity must be established from the runtime, not assumed from `develop` source or this Draft |
| WCL-CONSUMER-05 | 02B L54–58 require a consistent source for the counted entity/world snapshot | Comparing the snapshot entity/world/provider identifiers with an independently observed request is a proposed technical guard, not an approved protocol specification |
| WCL-CONSUMER-06 | 02B L57–58 separate failed or absent loaded data from verified saved/offline data | `NotFound` versus `Failure` are observed API variants; their provenance is `docs/verification/phase5-wcl-provider-contract.md`, not a blanket user decision that either variant means empty |
| WCL-CONSUMER-07 | 02A L31 allows absence of ownership candidates **after successful verification**; 02B L57 requires completed read | Empty `Success` covers only the identified entity/provider storage. It cannot establish complete world inventory, verified missing commander/owner, or an overall zero settlement |

**Current source boundary:** `develop@b31b8cc0a5c1e88d2c0960349b964fd934d1bc39`, `SavedEntityOwner.java` Git blob `ec5d5a91e51cb7bb925bbb2152486100c0e07cbc`: vanilla saved tame-owner hints, not a WCL service enumeration or an authoritative WWC/WWW owner. This mapping does not supply formal Process/PI/TCE Confirmed history; that provenance and the implementation-specific rules above remain to be reviewed before any new production semantics or spec-implementation Gate.

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| WCL-CONSUMER-01 | Both WWC and WWW register the same WonderfulStorageReader service type | Enumerate all registrations, not only the single result of ServicesManager.load | WCL-REG-01 |
| WCL-CONSUMER-02 | A required provider is missing, disabled or its plugin owner is unverified | Mark the source UNKNOWN/incomplete; do not publish zero assets | WCL-REG-02 |
| WCL-CONSUMER-03 | Duplicate provider IDs or conflicting plugin owners appear | Reject the ambiguous provider inventory before accounting | WCL-REG-03 |
| WCL-CONSUMER-04 | A registration's providerId disagrees with its expected WWC/WWW plugin identity | Reject that registration as untrusted even if it returns Success | WCL-REG-04 |
| WCL-CONSUMER-05 | Success snapshot entity, world or provider identity differs from independently verified request | Reject the snapshot; do not count its items or infer absence | WCL-REG-05 |
| WCL-CONSUMER-06 | readLoaded returns NotFound or readStored returns NotFound/Failure | Do not equate the result with an authoritative empty offline inventory | WCL-REG-06 |
| WCL-CONSUMER-07 | Success has matching identity and an empty items list | Treat only that provider/entity storage as verified empty; preserve other mandatory source checks | WCL-REG-07 |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| WCL-REG-01 | Two valid service registrations with distinct WWC/WWW provider IDs | Both retained; no single-provider shortcut |
| WCL-REG-02 | WWC present and WWW absent/disabled | UNKNOWN/incomplete, not a successful empty WWW inventory |
| WCL-REG-03 | Two registrations claim providerId=wwc | Reject duplicate authority |
| WCL-REG-04 | Claimed providerId=www but registration owner is not the verified WWW plugin | Reject untrusted provider |
| WCL-REG-05 | Success carries wrong entity/world/provider UUID or ID | Reject without accounting |
| WCL-REG-06 | NotFound for loaded entity while saved/offline source is not yet verified | UNKNOWN until saved source and reconciliation are established |
| WCL-REG-07 | Success with verified identity and zero items | Empty for this one provider/entity only |

## Evidence and execution boundary

The referenced verification document records exact WCL, WWC and WWW source blob SHAs, service registration semantics and the `Success`/`NotFound`/`Failure` distinction. These are source observations, not observations of the user's installed Paper 26.2 plugins.

No SimpleCoins WCL adapter, service enumeration, offline PDC reconciliation, nested-item decoding or Paper integration test is claimed by this Draft. It requires legitimate CAT Process/PI/TCE confirmation and the normative pre-implementation Gates before production code. Draft syntax lint is not semantic conformance or authorization to merge an implementation.

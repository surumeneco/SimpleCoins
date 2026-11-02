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

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.paper-tameable-runtime-boundary
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/blob/feature/owner-evidence-20261101/src/main/java/co/surumene/simplecoins/PaperCompanionOwnerAdapter.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paper Tameable loaded-owner runtime boundary (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.owner.paper-tameable | Read-only owner evidence from a loaded Paper Tameable |
| simplecoins.owner.paper-unavailable | Failure to read runtime owner evidence is UNKNOWN, not verified absence |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| TAME-RUNTIME-01 | Loaded tamed entity and tamer UUID are available | Return KNOWN UUID | TAME-RUNTIME-01 |
| TAME-RUNTIME-02 | Loaded entity reports untamed | Return ABSENT without requesting owner | TAME-RUNTIME-02 |
| TAME-RUNTIME-03 | Loaded entity is tamed but getOwner returns null or throws | Return UNKNOWN; never infer verified absence | TAME-RUNTIME-03 |
| TAME-RUNTIME-04 | isTamed or AnimalTamer.getUniqueId throws during read | Return UNKNOWN; no exception escapes to settlement | TAME-RUNTIME-04 |
| TAME-RUNTIME-05 | Null Tameable reference | Return UNKNOWN | TAME-RUNTIME-05 |

## Scope and Gate boundary

These are direct Paper-interface proxy tests of the already implemented `PaperCompanionOwnerAdapter.readPaper` read-only boundary. They do not prove WWC/WWW/WCL commander absence, saved/unloaded evidence, Paper plugin runtime compatibility, or complete owner settlement. The tests use interface proxies rather than a live Paper server and do not modify any entity.

Status is Draft; no Process/PI/TCE Confirmed promotion or canonical semantic Gate is claimed. This test-only increment does not authorize production code changes or release.

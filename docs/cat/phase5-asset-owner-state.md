---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.asset-owner-state
status: draft
process: simplecoins.owner-classification
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: classified asset owner state (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.asset-owner | 検証済み所有者UUID・Everyone・BAN・playerdataからの分類 |
| simplecoins.companion | 指揮者が既知ならtame ownerより優先する |
| simplecoins.settlement | 未確定の外部所有者・BANをゼロ資産やFREEとして公開しない |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| owner.free | 事前検証済みの所有者不在、またはEveryone保護 | FREEでowner/reasonを持たない | OWNER-01 |
| owner.personal | 既知UUID、BANなし、playerdataあり | PERSONALと当該UUID | OWNER-02 |
| owner.banned | 既知UUID、現在BANあり | DEAD/CURRENT_BAN。playerdata欠損よりBAN優先 | OWNER-03 |
| owner.missing | 既知UUID、BANなし、playerdata欠損 | DEAD/MISSING_PLAYERDATA | OWNER-04 |
| owner.invariant | Resultを構築するとき | kind/UUID/reasonの不整合を拒否する | OWNER-05 |
| owner.commander | 既知commander UUIDとtame owner UUIDが競合する | commanderを優先する。commander不在時のみtame owner | OWNER-06 |
| owner.precondition | 外部所有者・BAN・snapshotが不明なとき | このboolean分類器に渡さず、上流のtyped evidence resolverでUNKNOWNのまま中断する | OWNER-07 |

## Process / PI
AssetOwner.resolve is a **post-validation** classification helper, not a proof source. Its null owner and boolean arguments have no UNKNOWN state; callers must establish authoritative ownership, current BAN completeness, registration/playerdata and snapshot consistency **before** invoking it. In particular, unknown owner must not be passed as null and converted to FREE. The same applies to mobOwner: it is a simple precedence helper and cannot distinguish missing from unresolved commander evidence. ExternalOwnerDecision and CompanionOwnerResolution retain the fail-closed boundary.

## Test model / TCE
- OWNER-01: null known-absent owner and Everyone return FREE without owner/reason.
- OWNER-02: verified owner with clear BAN and playerdata returns PERSONAL.
- OWNER-03: current BAN takes precedence over missing playerdata.
- OWNER-04: absent playerdata yields DEAD/MISSING_PLAYERDATA.
- OWNER-05: all invalid record combinations throw IllegalArgumentException.
- OWNER-06: commander UUID wins over tame UUID, tame is fallback.
- OWNER-07: contract boundary documented; UNKNOWN input is **not** a supported runtime case for this pure helper.

Status: **Draft, not Confirmed**. Source extraction from existing PR #8 is byte-identical. Formal CAT process-closure, TestModel, vectors, Queue, model-conformance and implementation-conformance not-run. Unit tests cannot establish live Paper/WWC/WWW/WCL identity, authoritative BAN, or settlement correctness.

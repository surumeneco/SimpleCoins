---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.startup-coordinator
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/223
  - https://github.com/surumeneco/SimpleCoins/pull/224
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase6 startup settlement coordinator — experimental Draft

## Targets

| Ref | Aspect |
| --- | --- |
| AdmissionBarrier | Player joining remains denied until all required conditions are published |
| SettlementStartupCoordinator | Separate bounded async disk staging and main-thread publication |
| SimpleCoinsPlugin | Production lifecycle integration is not implemented by this Draft |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| START-01 | Startup begins and scan is queued | Admission remains denied until a verified generation publishes; repeated startup cannot replace it | queuedWorkRemainsClosed |
| START-02 | Background disk staging succeeds | Main-thread publication must be scheduled; scanning alone cannot admit players | stageThenPublishOpens |
| START-03 | Scanner fails or returns null | Revoke admission and invoke the fail-safe callback | scannerFailureFailsClosed |
| START-04 | Players are online before publication | Do not publish; fail closed | onlinePlayerStopsPublication |
| START-05 | Atomic publisher throws | Never open admission and invoke failure response | failedPublisherNeverOpens |
| START-06 | Background worker rejects scheduling | Leave admission closed and invoke failure response | schedulerRejectionFailsClosed |
| START-07 | Scan and publish are complete | Opening admission must occur after publication and zero-online precondition checks | readyOnlyAfterPublish |
| START-08 | Terminal accounting failure occurs after publication | The current generation must revoke admission | lateFailureRevokes |

## Important boundaries

This is not an approved Process/PI/TCE. A user-verified authoritative complete snapshot, all loaded-world/disk sources, missing/duplicate detection, BAN/BlockLocker/WWC/WWW evidence, playerdata saving, atomic statistic generation switch and active journal monitoring are **not provided** by the coordinator. Its callbacks cannot prove the external scan is correct or that publication is atomic. Zero players is necessary but insufficient for a static world. Worker thread must never call Bukkit world APIs; publication callback must run on Paper main thread. Fail-safe callback in this prototype is not a verified Paper shutdown path.

This Draft is a source-first experimental implementation aid, never authorization to register a login listener in production or merge/release the startup path.

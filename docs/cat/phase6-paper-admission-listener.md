---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.paper-admission-listener
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/223
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6 Paper admission listener (Experimental Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| SettlementAdmissionListener | Adapter to Paper 26.2 connection validation event |
| AdmissionBarrier | Shared fail-closed startup settlement state machine |
| SimpleCoinsPlugin | Listener registration and scan lifecycle are deliberately not integrated in this experiment |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| PAPER-ADMIT-01 | A connection arrives while settlement is not READY | Set the connection kick message; do not allow the login | connectionDeniedUntilPublished |
| PAPER-ADMIT-02 | A connection arrives while settlement is READY | Do not override a prior rejection by another plugin; do not invoke event.allow() | readyDoesNotOverrideOtherPlugins |
| PAPER-ADMIT-03 | A post-publication fatal accounting condition occurs | The state machine revokes readiness and the adapter rejects subsequent connections | lateFailureReblocksConnections |

## Scope and limitations

This is an experimental, unregistered adapter against the actual Paper 26.2 API. The initial plugin branch does not yet have a complete authoritative world/player settlement or atomic publication, so automatic listener registration could lock out all players. No runtime registration or unconditional permission opening is proposed here. This Draft is not Confirmed and must not be interpreted as approval to merge, deploy or release.

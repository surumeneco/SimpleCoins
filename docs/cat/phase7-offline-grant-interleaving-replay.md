---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.offline-grant-interleaving-replay
status: draft
process: simplecoins.phase7
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Offline give: interleaved recipient receipt/restart invariants (CAT Draft)

Test-only characterization of the current `GrantLedger` journal. No new issuance behavior is approved by this Draft.

| Vector | Scenario | Expected behavior |
| --- | --- | --- |
| GIVE-REPLAY-01 | Grants for two recipients are interleaved by global journal sequence | Each recipient can acknowledge only its own receipts; another recipient's grant must not be interpreted as an owed grant |
| GIVE-REPLAY-02 | One recipient's grant is acknowledged and the process restarts while the other has two pending grants | Pending order and per-recipient skip prevention persist; no unrelated receipt permits a skipped grant |
| GIVE-REPLAY-03 | A receipt is saved before journal acknowledgement and the process restarts twice | Repeated acknowledgement is idempotent; no duplicate pending grant appears; next reservation sequence remains monotonic |

These are isolated journal tests, **not** live Paper `OfflineGrants.deliver`, inventory mutation, playerdata persistence, admin permission, Nexo/Geyser/JE/BE delivery, or full settlement evidence. CAT Draft is not Confirmed; formal CAT semantic Gates and actual Paper restart/inventory tests remain not-run.

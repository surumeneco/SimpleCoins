---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.ban-source-identity-boundary
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BAN source identity validation (CAT Draft)

Status: Draft; not Confirmed. This isolated test preparation does not change production behavior or claim a passed CAT semantic Gate.

## Boundary

A BAN decision is based on a caller-declared, authoritative set of required source IDs and observations for one owner UUID. Source IDs are compared after trimming outer whitespace and lowercasing with Locale.ROOT. Distinct spellings of the same normalized ID do not constitute independent evidence. Unexpected observations, malformed declarations, null evidence, or duplicate observations are invalid, even if another observation claims BANNED.

## Test vectors

| ID | Input | Expected |
| --- | --- | --- |
| BAN-IDENTITY-01 | Required ID and observation differ only by case/outer whitespace | One matching source; CLEAR when all required sources are CLEAR |
| BAN-IDENTITY-02 | Two declared required IDs normalize to the same identity | IOException; no fabricated source plurality |
| BAN-IDENTITY-03 | Missing/null/blank required ID or null required set | IOException |
| BAN-IDENTITY-04 | Duplicate observations with conflicting or identical statuses | IOException, even if one observation is BANNED |
| BAN-IDENTITY-05 | Unexpected observation ID, null source, null status or blank ID | IOException; not interpreted as a verified BAN |
| BAN-IDENTITY-06 | Missing required source with one definitive BANNED observation | BANNED; otherwise missing source remains UNKNOWN |

## Scope and evidence

The tests target the existing develop `BanEvidence.combine` pure-domain API. They do not establish an authoritative external plugin inventory, UUID/IP binding, Paper BAN adapter, loaded/unloaded companion ownership, or live settlement acceptance. Formal Process/PI/TCE confirmation and deterministic CAT Gates remain not-run; this Draft is not a production change.

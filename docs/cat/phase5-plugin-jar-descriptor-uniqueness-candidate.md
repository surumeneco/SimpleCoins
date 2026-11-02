---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.jar-inventory-descriptor-uniqueness-candidate
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/pull/110
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 plugin JAR inventory: ambiguous root descriptors (Draft)

This is a proposed contract for a **read-only diagnostic inventory** only. It does not prove that Paper loaded a plugin or determine BAN/owner authority. A ZIP archive may legally contain multiple members with the same name; a set intersection over `ZipFile.namelist()` discards their multiplicity. Different ZIP readers may choose different members, so the inventory must not report an unambiguous root descriptor based solely on a name set.

## Rules and vectors

| ID | Condition | Requirement/要求 | Evidence |
| --- | --- | --- | --- |
| JAR-DESCRIPTOR-01 | A JAR contains two root `plugin.yml` entries | Mark scan incomplete with an explicit ambiguous descriptor error; do not attest a unique descriptor | Duplicate legacy descriptor Red regression |
| JAR-DESCRIPTOR-02 | A JAR contains two root `paper-plugin.yml` entries | Mark scan incomplete with an explicit ambiguous descriptor error; do not attest a unique descriptor | Duplicate Paper descriptor Red regression |
| JAR-DESCRIPTOR-03 | A JAR contains one `plugin.yml` and one `paper-plugin.yml`, each unique | Preserve both names as evidence without treating this as a duplicate-path error | Dual-distinct positive control |

## Boundaries and status

The ZIP member multiplicity is an observation about bytes in a JAR, not a normative claim about which descriptor Paper selects. The scanner must remain read-only, report the SHA-256 of the same copied byte sequence used for descriptor inspection, and avoid silently treating an ambiguous archive as complete. Existing JAR-SNAPSHOT-01–05 constraints remain applicable.

**Status: Draft.** These tests are characterization/Red candidates. They do not authorize production owner-settlement changes or imply CAT Confirmed status, formal semantic Gate completion, Paper runtime acceptance, or integration into develop/main.

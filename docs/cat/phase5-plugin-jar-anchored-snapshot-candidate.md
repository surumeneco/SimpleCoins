---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.jar-inventory-anchored-snapshot-candidate
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/pull/105
  - https://github.com/surumeneco/SimpleCoins/pull/106
  - https://github.com/surumeneco/SimpleCoins/pull/108
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 plugin JAR inventory: anchored read and immutable observation (Draft)

This is a proposed **read-only diagnostic-tool** contract, not a Confirmed settlement rule. It does not establish loaded Paper plugin identities, BAN-provider completeness, or a consistent world snapshot.

## Rules and vectors

| ID | Condition | Proposed behavior | Evidence |
| --- | --- | --- | --- |
| JAR-SNAPSHOT-01 | The plugins directory is stable and contains a valid root JAR | Report deterministic name, SHA-256, size, and root descriptors from the **same captured byte sequence** | Existing seven baseline tests |
| JAR-SNAPSHOT-02 | The plugins directory pathname is replaced during enumeration or file opening | Never attest a JAR reached only through the replacement directory; either use the original directory descriptor or fail closed | PR #106 three tests |
| JAR-SNAPSHOT-03 | The JAR bytes change between digest calculation and ZIP descriptor inspection, even when size and mtime are restored | SHA and descriptors must come from one immutable copied byte sequence; changed source metadata may additionally mark the scan incomplete | PR #108 two tests |
| JAR-SNAPSHOT-04 | A final-component JAR symlink appears at opening | Reject it rather than follow it | Baseline swap-at-open regression |
| JAR-SNAPSHOT-05 | Any ancestor component is a symlink | Reject the directory, rather than relying on a final-component-only check | New ancestor-link regression |
| JAR-SNAPSHOT-06 | Root `plugin.yml` or `paper-plugin.yml` is listed in the ZIP central directory but its payload fails CRC verification | Do not attest a complete descriptor; mark the JAR unreadable/incomplete | CRC-tampered ZIP control and expected-Red regression |

## Boundaries

The scanner may copy an opened JAR to a bounded-memory temporary spool and inspect that spool. This guarantees digest/descriptor consistency **for the bytes actually read**, not atomicity of the source file while it is being copied. Source metadata changes should cause an incomplete result. Directory-relative operations must use an opened directory descriptor rather than re-resolving a pathname for each JAR.

A descriptor name in the ZIP central directory alone does not establish that the descriptor payload is readable. CRC validation requires consuming the descriptor bytes. This does not assert YAML schema validity, authenticity, or verification of unrelated ZIP members. The test-only Red is not an approved production fix.

This Draft is test-support only. It must not be promoted to Confirmed without normative approval; no Paper live acceptance, CAT semantic Gate, production ownership inference, develop/main merge or deployment is implied.

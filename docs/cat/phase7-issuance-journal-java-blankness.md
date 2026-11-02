---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.issuer-blankness
status: candidate
process: simplecoins.phase7
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Issuance journal issuer blankness compatibility

Existing Java GrantLedger validates issuer with String.isBlank(). The Python read-only journal verifier uses str.strip(), which is not equivalent. JDK 21 accepts a single U+00A0, U+0085, U+2007 or U+202F as nonblank; Python strip treats each as empty. The verifier must not reject a structurally valid Java-written journal solely for these issuer characters. Conversely U+2003 and ASCII whitespace are blank under Java and must remain invalid.

This is a code-to-spec Candidate. No normative promotion, production modification, Paper runtime acceptance, or formal CAT Gate is claimed.

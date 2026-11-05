---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.ban-unicode-source-alias
status: draft
process: simplecoins.owner-ban-evidence
source_refs: []
refs: []
semantic_contract: markdown-v2
language: ja
---

# BAN source ID: Unicode whitespace aliases (Draft)

BAN-DEC-03 requires whitespace aliases to be non-independent. Current Java String.trim does not remove EM SPACE or NO-BREAK SPACE. Whether these code points are accepted in canonical provider IDs requires normative confirmation.

| Vector | Candidate input | Expected observation | Current status |
| --- | --- | --- | --- |
| BAN-UNICODE-01 | Required paper and paper with trailing U+2003; both CLEAR | IOException: reject alias as non-independent source | **Expected Red**: currently CLEAR |
| BAN-UNICODE-02 | Required paper and U+00A0 followed by paper; both CLEAR | IOException: reject alias as non-independent source | **Expected Red**: currently CLEAR |
| BAN-UNICODE-03 | One required paper, two observations differing by trailing U+2003 | IOException: reject undeclared observation | **Green control**: currently rejected |
| BAN-UNICODE-04 | Required paper and ASCII-space-surrounded paper | IOException: reject duplicate after ASCII trim | **Green control**: currently rejected |
| BAN-UNICODE-05 | One required paper, one matching CLEAR observation | CLEAR | **Green control**: currently CLEAR |

These are **Draft candidates and controls**, not a Confirmed CAT specification. On 2026-11-03, a direct Java 21 harness compiled the exact `develop` `BanEvidence.java` Git blob `0e3f29c2a85fca1d711a928b9db7637193805299` and observed 2 expected Red and 3 Green controls. This is source-exact direct execution, not a Gradle/JUnit run, formal CAT Gate, or Paper acceptance. No production behavior change or settlement authorization. Provider-ID grammar confirmation remains pending.

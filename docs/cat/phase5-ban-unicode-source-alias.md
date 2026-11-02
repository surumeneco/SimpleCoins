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

| Vector | Candidate input | Expected Red assertion |
| --- | --- | --- |
| BAN-UNICODE-01 | Required paper and paper with trailing U+2003; both CLEAR | IOException: duplicate normalized source |
| BAN-UNICODE-02 | Required paper and U+00A0 followed by paper; both CLEAR | IOException: duplicate normalized source |
| BAN-UNICODE-03 | One required paper, two observations differing by trailing U+2003 | IOException: duplicate observations |

These are candidate Red tests, not a confirmed CAT specification. No production behavior change or settlement authorization. Formal CAT Gates and provider-ID grammar confirmation remain pending.

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.ban-canonical-empty-source-id
status: draft
process: simplecoins.owner-ban-evidence
source_refs: []
refs: []
semantic_contract: markdown-v2
language: ja
---

# BAN source ID: canonical empty after Java trim (Draft)

The declared and observed BAN source IDs must identify a nonempty authority. Current `BanEvidence.combine` checks `String.isBlank()` **before** `trim().toLowerCase(Locale.ROOT)`. Some C0 control-only strings are not blank under `isBlank()`, but `trim()` reduces them to the empty string. A matching empty normalized required/observed pair can therefore produce `CLEAR`. This is a fail-open evidence declaration boundary, not a real provider lookup.

| Vector | Candidate input | Expected observation | Source-exact direct result |
| --- | --- | --- | --- |
| BAN-CEMPTY-01 | Required and observed IDs each contain only U+0000, observed CLEAR | IOException: reject empty normalized authority ID | **Expected Red**: CLEAR |
| BAN-CEMPTY-02 | Required and observed IDs each contain only U+0001, observed CLEAR | IOException: reject empty normalized authority ID | **Expected Red**: CLEAR |
| BAN-CEMPTY-03 | Required and observed IDs each contain U+0000 U+0001 U+0000, observed CLEAR | IOException: reject empty normalized authority ID | **Expected Red**: CLEAR |
| BAN-CEMPTY-04 | Required/observed ID `paper-profile`, observed CLEAR | CLEAR | **Green control**: CLEAR |

This Draft does not decide a general Unicode provider-ID grammar or the alias-normalization questions in PR #115. It isolates the narrower invariant that an authority key **after existing normalization** cannot be empty. The exact `develop` `BanEvidence.java` blob `0e3f29c2a85fca1d711a928b9db7637193805299` was compiled with Java 21 (`javac --release 21`) and directly executed on 2026-11-03: 3 expected Red cases, 1 Green control. Formal CAT semantic Gates, real Gradle/JUnit, Paper runtime and production Green are not-run. No specification is promoted to Confirmed by this characterization.

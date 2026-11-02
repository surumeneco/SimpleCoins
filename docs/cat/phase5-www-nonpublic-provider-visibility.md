---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.www-nonpublic-provider-visibility
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/blob/feature/owner-evidence-20261101/src/main/java/co/surumene/simplecoins/PaperWonderfulWolfOwnerAdapter.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# WWW public API / non-public implementation visibility (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.owner.www-reflection | Public loadedIndividuals/find/commanderId/ownerId method contract exposed through implementation classes with narrower Java visibility |
| simplecoins.owner.www-unavailable | Preserve UNKNOWN when the optional WWW API or an individual is genuinely unavailable |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| WWW-VIS-01 | A public plugin method returns a package-private implementation of a public loaded-individuals contract, with a known commander | Consumer should obtain the known commander without requiring the implementation class to be public; proposed behavior, not Confirmed | WWW-VIS-01 |
| WWW-VIS-02 | A public loaded provider returns an individual whose implementation class is package-private but whose public methods provide a known commander | Consumer should obtain the known commander through a legitimate public contract; proposed behavior, not Confirmed | WWW-VIS-02 |
| WWW-VIS-03 | All runtime classes and public methods are accessible | Existing commander precedence remains PERSONAL | WWW-VIS-03 |
| WWW-VIS-04 | A genuinely missing individual or unavailable API returns no authoritative evidence | Return UNKNOWN, never FREE | WWW-VIS-04 |

## Evidence and Gate boundary

Current `PaperWonderfulWolfOwnerAdapter.readPlugin` invokes `getMethod(...).invoke(...)` on each runtime concrete class. Java 21 may reject invocation of a public method declared by a non-public implementation class across packages. The test fixture models that Java visibility boundary; it is not evidence that the installed WWW plugin actually uses those concrete class visibilities. Source contract confirmation against the installed WWW binary and Paper runtime remains necessary.

This Draft records a test-only compatibility hypothesis. It does not authorize reflective `setAccessible`, production implementation changes, owner settlement, or promotion to Confirmed. Normative Process/PI/TCE approval, machine CAT Gates, Gradle/JUnit and Paper integration must be reported separately.

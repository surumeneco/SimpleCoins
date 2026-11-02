---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.ban-null-declaration-observation
status: draft
process: simplecoins.owner-ban-evidence
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BAN source declaration null boundaries (CAT code-to-spec Draft)

## Scope and source of truth

Characterize the existing pure-Java `BanEvidence.combine` validation boundary on SimpleCoins `develop` commit `755b179b984a216c56722de3ad8d6ffaa7909115`, source Git blob `0e3f29c2a85fca1d711a928b9db7637193805299`. No new provider-ID grammar, BAN source discovery, external provider completeness, or owner settlement rule is proposed. This is a code-to-spec observation, **not** a Confirmed normative CAT artifact.

## Non-overlapping characterization vectors

| Vector | Input | Observed fail-closed expectation |
| --- | --- | --- |
| BAN-NULL-01 | null required-source set, otherwise valid evidence | `IOException` |
| BAN-NULL-02 | valid required-source set, null observation list | `IOException` |
| BAN-NULL-03 | required-source set containing a null member | `IOException` |
| BAN-NULL-04 | required-source set containing a blank member | `IOException` |
| BAN-NULL-05 | observation list containing a null `Source` after a positive BANNED observation | `IOException` rather than BANNED |
| BAN-NULL-06 | observation with null ID after a positive BANNED observation | `IOException` rather than BANNED |

Existing `BanEvidenceTest` already covers empty declarations, blank observation IDs, null statuses, normalized duplicates, and missing required evidence; `PR #107` covers other malformed observation permutations. Those cases are deliberately not reimplemented here.

## Verification boundary

The JUnit candidate uses only the existing public `combine` API and no Paper dependencies. The source-exact Java 21 direct harness passed 3 methods / 7 assertions on the recorded production and test blobs. Separately, GitHub Actions [Domain foundation #41518134613](https://github.com/surumeneco/SimpleCoins/actions/runs/41518134613) completed SUCCESS on test commit `fd3a5ae1c9f985f77ad6714954f900ccca1b375e`, using Java 25 and Gradle 9.2.1 `clean test jar`. This establishes real JUnit/build execution for the unchanged test blob, not a normative CAT Gate. This Draft does not satisfy Process/PI/TCE confirmation, model-conformance, implementation-conformance, provider completeness, or Paper live acceptance. Do not infer a deployable release from it.

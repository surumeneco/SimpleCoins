---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.ban-permutation-observation
status: draft
process: simplecoins.owner-ban-evidence
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BAN evidence permutation and missing-source observation (CAT Draft)

## Position in the existing contract

This code-to-spec verification extends the existing `phase5-ban-decision-evidence-matrix.md` without replacing its rules. The subject is the already implemented, pure-Java `BanEvidence.evaluate` on `develop`. It does not discover plugins, prove that the required source list is complete, or authorize an owner settlement.

## Targets

| Ref | Aspect |
| --- | --- |
| `BanEvidence.evaluate` | Declared four-source evidence precedence and order invariance |
| `BanEvidencePermutationAndMissingSourceTest` | Exhaustive permutations and malformed-source rejection |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| BAN-PERM-01 | Four declared sources with complete observations in any order | BANNED dominates; otherwise UNKNOWN dominates; only all CLEAR yields CLEAR | `fourSourceTruthTableIsOrderIndependent` (1,944 cases) |
| BAN-PERM-02 | One of four required sources absent; three observed sources in any order | Positive BANNED still dominates; otherwise UNKNOWN, never CLEAR | `missingFourthSourceNeverProvesClear` (162 cases) |
| BAN-PERM-03 | Undeclared, duplicate, or null-status source before or after a BANNED observation | Reject with IOException rather than returning BANNED | `invalidSourceEvidenceNeverOverridesValidation` (6 cases) |

## Evidence boundary

Pinned starting point: SimpleCoins `develop` commit `32eb8c1be98f98cf502227fa91028a6c66400dae`, `BanEvidence.java` Git blob `0e3f29c2a85fca1d711a928b9db7637193805299`. This is a characterization of existing behavior, not a new normative decision. The new tests use the same public API and no Paper dependencies.

The document remains Draft. Direct Java 21 execution with a minimal JUnit-API shim is separate from a real Gradle/JUnit run and from formal CAT Process/PI/TCE Gates. No production code, external plugin, BAN inventory, or Paper runtime behavior is changed.

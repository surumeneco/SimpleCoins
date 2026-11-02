---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.www-commander-owner-accessor-boundary
status: draft
process: simplecoins.owner
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/d71683ad7f9e29de24af2f7c6de4663909990b38/src/main/java/co/surumene/simplecoins/PaperWonderfulWolfOwnerAdapter.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: WWW commander and lower-priority owner lookup failure (CAT Draft)

The accepted owner precedence in 02A is commander, then owner, then free. This Draft records a candidate discrepancy between the existing reflection adapter and the pure CompanionOwnerResolution resolver. It does not authorize production changes or a CAT Confirmed promotion.

## Targets

| Ref | Aspect |
| --- | --- |
| `PaperWonderfulWolfOwnerAdapter.readPlugin` | Loaded WWW reflection and lower-priority owner lookup failure |
| `CompanionOwnerResolution.resolve` | Known commander precedence over unknown fallback evidence |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| WWW-ACCESS-01 | Known commander; owner accessor throws | Candidate: preserve the known commander despite failure of a lower-priority lookup | knownCommanderSurvivesThrowingOwnerAccessor |
| WWW-ACCESS-02 | Known commander; owner accessor missing | Candidate: preserve known commander; lower-priority accessor not needed for commander identity | knownCommanderSurvivesMissingOwnerAccessor |
| WWW-ACCESS-03 | Verified absent commander; owner accessor throws | UNKNOWN, not FREE | absentCommanderAndFailingOwnerRemainUnknown |
| WWW-ACCESS-04 | Both accessors return different UUIDs | Commander wins | knownCommanderStillWinsWithReadableOwner |

## Source observation and limits

Pinned PaperWonderfulWolfOwnerAdapter.java blob: 4a9045b0e89e99f694acaefdb67254675a90b91c. It invokes both accessors before calling CompanionOwnerResolution.resolve, so a reflective owner failure currently yields UNKNOWN even when commander is known. Two proposed positive-commander tests are expected Red; no implementation change is authorized by this Draft. A failed owner accessor may indicate an incomplete provider snapshot; legitimate normative CAT review must decide whether positive commander evidence is independently sufficient.

## Upstream applicability and decision boundary

The inspected WWW `main` source exposes **both** public record accessors (`WonderfulWolfIndividual.java` blob `ad2bd722d6e3ba7e5ddaec457f17e834b9b560a9`), with constructor validation requiring non-null `Optional<UUID>` values. The `main` plugin class (`09b1d2a4719258118a93711b23bc575a8e43606f`) and loaded registry (`e0b31006c841f14c19831fc51eb1efdc3c7d9eed`) expose the public `loadedIndividuals().find(UUID)` chain. Therefore WWW-ACCESS-01/02 are **synthetic partial-API-failure probes**, not evidence that the inspected WWW `main` implementation actually omits or throws from `ownerId()`; neither deployed JAR identity nor runtime exception behavior has been observed.

There is also a normative ambiguity: the existing broader Phase 5 Draft says an invalid reflection contract yields UNKNOWN, whereas this narrower Draft proposes preserving a verified commander when only the lower-priority accessor fails. A failed accessor may invalidate the provider snapshot as a whole. Do not implement a Green fix or promote either rule to Confirmed until the authoritative CAT decision specifies whether independent commander evidence survives such a partial provider failure.

This is a loaded-individual code-to-spec test boundary only. Provider identity, plugin enablement, saved/offline entities, world completeness, settlement publication and Paper live acceptance remain unverified. CAT process-closure, TestModel, model-conformance, vectors, Queue/current-item and implementation-conformance Gates are not-run.

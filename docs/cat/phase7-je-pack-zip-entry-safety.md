---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.pack.je-zip-entry-safety
status: draft
process: simplecoins.pack
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/verify-je-pack.sh
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7 — JE pack ZIP entry safety (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| hosted-pack-verifier | Decide whether a byte-identical, structurally valid ZIP containing unsafe entry names may pass the distribution verification gate |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| JE-ZIP-ENTRY-01 | ZIP contains a parent-traversal entry such as ../escape.txt | Reject as an unsafe hosted resource pack, even if byte-identical and unzip -tq succeeds | parentTraversalEntryCannotPassHostedPackVerifier |
| JE-ZIP-ENTRY-02 | ZIP contains an absolute path entry such as /absolute.txt | Reject as unsafe rather than advertise a successful verification | absolutePathEntryCannotPassHostedPackVerifier |
| JE-ZIP-ENTRY-03 | ZIP has a normal pack.mcmeta entry with no unsafe names | Preserve ordinary byte-identical success | ordinaryPackEntryStillVerifies |

## Test vectors

| ID | Generated and hosted archive | Required result |
| --- | --- | --- |
| JE-ZIP-ENTRY-01 | pack.mcmeta + ../escape.txt, same local/hosted bytes | Nonzero verifier exit |
| JE-ZIP-ENTRY-02 | pack.mcmeta + /absolute.txt, same local/hosted bytes | Nonzero verifier exit |
| JE-ZIP-ENTRY-03 | pack.mcmeta only, same local/hosted bytes | Exit 0 and identical-byte evidence |

## Scope and authority

**Draft, not Confirmed.** The current verifier's documented checks are byte identity, SHA-1 equality, and unzip -tq integrity; these do not validate entry-name safety. The proposed entry-name rejection is a separate deployment policy candidate and does not imply that a client will extract an entry to the host filesystem. This test is read-only and uses disposable localhost archives; it must not change a live hosted pack.

The real JE/BE/Geyser acceptance, normative CAT Process/PI/TCE Gates, policy approval and production verifier fix are **not-run**. Do not merge expected Red tests or report pack delivery as accepted.

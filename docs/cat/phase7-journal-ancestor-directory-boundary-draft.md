---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.journal-ancestor-directory-boundary
status: draft
process: simplecoins.phase7
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
  - https://github.com/surumeneco/SimpleCoins/pull/116
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: offline journal verifier ancestor directory boundary (Draft)

This proposed read-only verifier contract refines the existing ISS-DIAG-06 symlink rule. It is not a new settlement or offline-give policy. The verifier is intended for a stopped-server journal copy; a pathname containing an ancestor symlink must not silently redirect the observed journal to another directory.

## Rules and vectors

| ID | Condition | Requirement/要求 | Evidence |
| --- | --- | --- | --- |
| ISS-PATH-01 | The supplied journal directory itself is a symlink | Reject it as a non-real directory | Existing ISS-DIAG-06 regression |
| ISS-PATH-02 | A parent component of the supplied journal directory is a symlink | Reject rather than traverse the alias and report a clean journal | New ancestor-symlink Red regression |
| ISS-PATH-03 | A stable nested directory with no symlink component contains a valid empty journal | Accept it with records=0, pending=0, head=0 | New positive control |

## Boundary and status

The rule is for directory-path provenance of the offline verifier only. It does not assert a coherent filesystem snapshot under concurrent renames or writes, nor that Paper actually delivered grants. This Draft must not be promoted to Confirmed without normative approval; formal CAT semantic Gates, live Paper restart/idempotence and JE/BE/Nexo acceptance remain not-run.

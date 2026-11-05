---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.pack-hash-lexical-boundaries
status: draft
process: simplecoins.resource-pack.hash
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Resource-pack hash path boundary (existing-code Draft)

TASK-20261101-006. Characterization of existing `ResourcePackHash.java`
blob `50219d9f4b858ce8b58d67b9ae9944aba5ba94b4` on develop.

- A lexical `..` path component is rejected, even when it would normalize to an existing regular archive.
- A filename containing consecutive dots but not a standalone `..` path component is permitted and hashed accurately.
- An empty archive is a regular file and yields the standard 20-byte SHA-1 digest.
- Valid nested real directories are allowed; no implicit requirement for the output to live at the workspace root.

These are non-normative, code-to-spec Draft observations with no production changes. A test result cannot establish JE/BE client behavior, Nexo publication, Paper/Geyser, or CAT semantic and implementation Gates.

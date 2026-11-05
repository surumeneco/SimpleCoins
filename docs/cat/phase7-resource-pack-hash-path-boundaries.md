---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.pack-hash-regular-archives
status: draft
process: simplecoins.resource-pack.hash
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Resource-pack hash for regular archives (existing-code Draft)

TASK-20261101-006. Observation of `ResourcePackHash.java` blob
`50219d9f4b858ce8b58d67b9ae9944aba5ba94b4` on develop.

1. An empty regular archive produces the standard 20-byte SHA-1 digest.
2. An archive under existing nested real directories yields its exact content digest.
3. Rebuilding an archive at the same path with different bytes changes its hash; restoring the prior bytes restores the prior hash.

Scoped code-to-spec characterization only. The tests do not establish JE/BE client distribution, Geyser/Nexo integration, security against all filesystem races, or CAT normative Gates. No production source is modified.

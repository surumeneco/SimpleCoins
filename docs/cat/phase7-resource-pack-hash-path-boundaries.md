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

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.resource-pack.hash | `ResourcePackHash.java` の既存コードにおけるregular archiveのSHA-1導出のDraft観測 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| PACK-HASH-01 | 空のregular archive | 標準の20-byte SHA-1 digestを返す | 既存回帰テスト（IDは本書未特定） |
| PACK-HASH-02 | 実在する多段の実ディレクトリ下にあるarchive | archiveの内容に対する正確なdigestを返す | 既存回帰テスト（IDは本書未特定） |
| PACK-HASH-03 | 同一pathのarchiveを異なるbyte列で再生成し、その後元へ戻す | 内容変更に応じてhashが変化し、内容を戻すとhashも復帰する | 既存回帰テスト（IDは本書未特定） |

Scoped code-to-spec characterization only. The tests do not establish JE/BE client distribution, Geyser/Nexo integration, security against all filesystem races, or CAT normative Gates. No production source is modified.

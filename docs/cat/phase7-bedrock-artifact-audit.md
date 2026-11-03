---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.bedrock-artifact-audit
status: draft
process: simplecoins.bedrock-pack-publication
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/BedrockPack.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: read-only Bedrock pack and mapping structural audit (Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.bedrock-pack-publication | Existing generated .mcpack ZIP and Geyser mapping JSON, observed without modifying either artifact |
| simplecoins.phase7.je-be-acceptance | Offline structural evidence only; no Geyser installation or Bedrock client acceptance |

## Rules

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| BE-AUDIT-01 | ZIPにmanifest、atlas、9額面のPNGが存在し、mappingの9定義とID/texture参照が一致する | valid_structureを返すが、配布・表示成功とは称さない | valid |
| BE-AUDIT-02 | PNG不足、余剰/危険ZIP member、空PNG、重複member | invalidを返す | archive |
| BE-AUDIT-03 | atlasのtexture参照、mappingのmodel/identifier/icon/stack-sizeが一致しない | invalidを返す | mapping |
| BE-AUDIT-04 | manifestのUUID/モジュール不正、JSON/ZIP破損、展開容量超過 | invalidを返す | malformed |
| BE-AUDIT-05 | ZIPとmappingが別個に正しい構造であっても、生成・公開の原子性とJE/BEクライアント受入は未検証 | valid_structureをリリースGateとして使わない | scope |

## Process / PI

Input: two already-generated local files on a disposable verification host. The read-only auditor checks ZIP entries, integrity, manifest UUIDs, texture atlas, and Geyser v2 mapping IDs for the nine source-verified CoinDenomination values. It does not verify Geyser's installed file identities, external pack hosting, texture rendering, Java pack delivery, Nexo, or whether the pair was atomically published. No production files are written.

## TestModel / TCE

- Valid synthetic pair, missing/extra/empty PNG, wrong atlas reference.
- Missing/duplicate mapping IDs, wrong icon, duplicate manifest UUID.
- Corrupt ZIP and malformed JSON.

Status: **Draft**. Formal CAT semantic Gates and real Paper/Geyser/JE/BE runtime acceptance are not-run. The independent publication-alias Red candidate is PR #149; this auditor does not repair that defect.

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.bedrock-output-path-alias
status: draft
process: simplecoins.bedrock-pack-publication
source_refs: []
refs: []
semantic_contract: markdown-v2
language: ja
---

# Bedrock pack/mapping destination alias (Draft)

`BedrockPack.write(pack, mapping, ...)` publishes a ZIP archive and a JSON mapping in sequence. Its existing symlink checks protect against redirected paths, but do not prove that the two destination arguments identify **different files**. If the same destination is passed twice, the JSON publication replaces the just-written ZIP and the method can return success while the expected `.mcpack` is no longer a ZIP. This is distinct from the second-move failure characterized in PR #86.

| Vector | Candidate input | Expected observation | Current status |
| --- | --- | --- | --- |
| BE-ALIAS-01 | `pack` and `mapping` are the identical destination Path | Reject before publication; do not claim a valid pack/mapping pair | **Expected Red**: both publications succeed; destination contains JSON |
| BE-ALIAS-02 | `pack` and `mapping` normalize to the same destination (e.g. `./`) | Reject before publication; no partial artifact | **Expected Red**: same destination overwritten |
| BE-ALIAS-03 | Two distinct output paths with complete denomination data | Generate a readable ZIP and separate JSON mapping | **Green control** |

The proposed precondition is a **Draft candidate**, not Confirmed CAT. This test does not decide broader transactional pair-publication semantics, filesystem race resistance, or external Geyser delivery. Production fix, formal CAT semantic Gates, actual Gradle/JUnit, and Paper/BE runtime are not-run at Draft creation. No production code or deployment change is authorized by this document.

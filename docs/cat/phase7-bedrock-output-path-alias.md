---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.bedrock-output-path-alias
status: draft
process: simplecoins.bedrock-pack-publication
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: Bedrock pack/mapping output identity (Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.bedrock-pack-publication | `BedrockPack.write(pack,mapping,...)` のZIPとJSONの公開先が同一実パスを指す場合の破損境界 |

## 規則

以下は**規範未確定の候補**。現行`BedrockPack.write`は出力先のsymlinkを検査するが、ZIPとmappingが**同一宛先**かを検査せず、ZIP公開後にJSONで上書きして正常終了し得る。これはPR #86の「2回目のmoveが失敗する」境界とは異なる。

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| bedrock.alias.same | packとmappingに同一Pathを指定 | 公開前にIOExceptionで拒否し、成功扱いにしない | BE-ALIAS-01 |
| bedrock.alias.dot | packとmappingが`./`を含む正規化同一宛先 | 公開前にIOExceptionで拒否し、部分生成物を残さない | BE-ALIAS-02 |
| bedrock.alias.control | 9額面の完全データと別々の2出力先 | ZIPとJSONをそれぞれ生成し、ZIPのmanifestを読める | BE-ALIAS-03 |
| bedrock.alias.existing | 同一出力先に既存の有効なファイルがある | 衝突を公開前に拒否し、既存ファイルの内容を変更しない | BE-ALIAS-04 |

## 状態・境界

Java21の`Files.move`/ZipFile直接probeで、同一正規化先にZIP→JSONを公開するとZIPが不正になることを確認した。これはファイルシステム動作のprobeであり、`BedrockPack`本体の実行ではない。JDK25 Gradle 9.2.1 [Actions #41553800492](https://github.com/surumeneco/SimpleCoins/actions/runs/41553800492)では94件中2件の**期待Red**（`BedrockPackOutputAliasTest`の同一/正規化同一先）で終了し、別宛先controlは成功した。CI総合はFAILURE。

本Draftはpair publicationのトランザクション、symlink race、実際のGeyser/BE配布を証明・確定しない。正式CAT意味論Gate、production Green、Paper/Geyser/BE実機は未実施。Confirmed昇格やdevelop/main統合は行わない。

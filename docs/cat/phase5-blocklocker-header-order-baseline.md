---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.blocklocker-header-order-baseline
status: candidate
process: simplecoins.blocklocker
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/d5161a64b5e6768d9fa1adaa0fe92d24907b86f1/src/main/java/co/surumene/simplecoins/domain/BlockLockerHeaderMatcher.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# BlockLocker runtime header order baseline (code-to-spec Candidate)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker.header-order | Existing `BlockLockerHeaderMatcher.match` text-only implementation; Git blob `804f9987c616222ae81b10ce57ab31236f259337` |

## 観測された振る舞い

| ID | 入力条件 | 現行コードの出力 | 回帰テスト |
| --- | --- | --- | --- |
| BL-HO-01 | PRIVATE / MORE_USERS の有効aliasを逆順に列挙 | 同じ正確なヘッダーに同じType、非一致はempty | matchingDoesNotDependOnRuntimeAliasIterationOrder |
| BL-HO-02 | 異なるTypeのaliasが旧式カラーコード・大文字小文字・外側余白除去後に衝突 | alias順序や正の表示一致より先にIOException | crossTypeCollisionAlwaysRejectsBeforePositiveMatch |
| BL-HO-03 | alias集合のどこかにnullまたは書式コードのみの値が存在 | 正の表示一致があってもIOException | malformedAliasAlwaysRejectsBeforePositiveMatch |
| BL-HO-04 | aliasに正規表現のメタ文字が含まれる | 正規表現ではなく文字列全体をリテラル比較 | regexMetacharactersAreComparedAsLiteralHeaderText |

## 証拠と限界

現行ソースの振る舞いを固定する**code-to-spec Candidate**であり、BlockLocker 1.14.1の正式契約・保護対象・所有者を認定するものではない。実行時ローカライズ値の信頼性、看板の物理的取付け、所有者UUID、保存済み/ライブの整合、Paper実機は別に検証する。

正規CAT Process/PI/TCE Confirmed、process-closure、model-conformance、implementation-conformance、Paper実機は未実施。Candidateの観測結果を新たな規範仕様に昇格させない。

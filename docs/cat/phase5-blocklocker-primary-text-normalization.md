---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.primary-visible-name
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: PRIVATE看板の可視primary名正規化

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | 保存済みPRIVATE看板の可視primary名から検証済みUUIDへの解決 |
| simplecoins.settlement | identity UNKNOWNのときの未確定決算境界 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| primary.name.formatted | 可視primary名にlegacy Minecraft色・書式コードと周辺空白があるとき | header matcherと同じ書式コード除去・trimを行い、例えば色付き Alice を正確なlookup key Aliceにする | SC-BL-NAME-01 |
| primary.name.plain | 可視primary名が既に正規化されているとき | 文字列を不必要に変換せず同一lookup keyを使う | SC-BL-NAME-02 |
| primary.name.empty | 書式除去後に名前が空であるとき | identity UNKNOWNを維持し、Everyone表示があってもFREEへ昇格しない | SC-BL-NAME-03 |
| primary.name.hint | 可視名lookup結果と保存済みPDC UUID hintが食い違うとき | identity UNKNOWNを返し、どちらのUUIDも正当な所有者と推測しない | SC-BL-NAME-04 |
| primary.name.authority | 可視名の正規化が成功したとき | 正規化名はlookup keyに限り、独立した権威あるidentity resolverの確認を必須とする。lookup失敗はUNKNOWNでありFREEではない | SC-BL-NAME-01, SC-BL-NAME-03, SC-BL-NAME-04 |

## 境界と状態

既存SC-BL-NAME-01〜04を意味変更せず、正規化とUUIDの権威確認を分離したTASK-20261101-004のCAT Draft。BlockLocker 1.14.1の実機ローカライズ、保存済みPDC/可視テキストの同期、物理的な看板取付、Paperの整合snapshot、決算公開はこの規則だけでは検証されない。正式CAT machine Gates・Paper実機検証はnot-run。Confirmed・マージ・デプロイではない。

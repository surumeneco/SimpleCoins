---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.ban-canonical-empty-source-id
status: draft
process: simplecoins.owner-ban-evidence
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: BAN source canonical-empty authority (Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.owner-ban-evidence | `BanEvidence.combine/evaluate` のrequired/observed source IDを正規化した後の空文字列境界 |

## 規則

以下は**規範未確定の候補**であり、現行実装が期待どおりに拒否するかを検証する。Java `String.isBlank()`で非空と見えるC0制御文字列が`trim()`後に空となるため、空のauthority keyで`CLEAR`を返す経路を隔離する。

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| ban.cempty.nul | requiredとobservedのIDがそれぞれU+0000のみ、観測CLEAR | 正規化後の空authority IDをIOExceptionで拒否する | BAN-CEMPTY-01 |
| ban.cempty.soh | requiredとobservedのIDがそれぞれU+0001のみ、観測CLEAR | 正規化後の空authority IDをIOExceptionで拒否する | BAN-CEMPTY-02 |
| ban.cempty.mixed | requiredとobservedのIDがU+0000 U+0001 U+0000、観測CLEAR | 正規化後の空authority IDをIOExceptionで拒否する | BAN-CEMPTY-03 |
| ban.cempty.control | requiredとobservedのIDが通常の`paper-profile`、観測CLEAR | 有効な単一authorityからCLEARを返す | BAN-CEMPTY-04 |

## 状態・境界

2026-11-03に、`develop`の実ソース`BanEvidence.java` Git blob `0e3f29c2a85fca1d711a928b9db7637193805299`とテストblob `5f0d18e73cadf3e9571c65e909b7d14799ea09ab`を照合し、Java21直接実行で3 expected Red・1 Green controlを観測した。JDK25 Gradle 9.2.1 [Actions #41553574566](https://github.com/surumeneco/SimpleCoins/actions/runs/41553574566)も95件中3件の**期待Red**（いずれも本テスト）で終了した。総合CIはFAILUREであり、Green成功ではない。

これは一般的なUnicode source-ID文法やPR #115のalias方針を決定しない。正式CAT意味論Gate、production Green、Paper実機・決算公開は未実施。DraftをConfirmedへ昇格しない。

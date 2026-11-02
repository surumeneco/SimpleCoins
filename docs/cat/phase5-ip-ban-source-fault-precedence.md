---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.ban.ip-source-fault-precedence
status: draft
process: simplecoins.ban
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 検証済みIP BAN証拠の故障優先順位（Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.ban | UUIDに対応付け済みの複数IPアドレスに対するBAN照会の集約 |
| simplecoins.owner | 未確認・欠損・故障をCLEARとして通常資産に公開しない |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| ip.banned-dominates | 検証済みアドレスの少なくとも1つがBAN中で、他が未確認または照会失敗 | BANの陽性証拠を保持し、UNKNOWNで上書きしない | IP-FAULT-01 |
| ip.clear-requires-complete | すべてのアドレス照会がCLEARかつUUID→IP対応が完全 | CLEARを返す。不完全な対応やnull・例外があればUNKNOWNを返す | IP-FAULT-02 |
| ip.malformed-address | 検証済みアドレス集合にnullが含まれる | 欠損をUNKNOWNとして扱うが、別の確実なBAN陽性を消さない | IP-FAULT-03 |

## 受入ベクトル（独立テスト準備）
- IP-FAULT-01: 2つの検証済みIPアドレスの各結果をBANNED/CLEAR/null/RuntimeExceptionの4状態で組み合わせ、対応完全/不完全の2状態、計32組を検査する。
- IP-FAULT-02: BAN陽性がなければ、全照会CLEARかつ対応完全の場合のみCLEAR。その他はUNKNOWN。
- IP-FAULT-03: nullアドレスとBAN陽性が同時ならBANNED、nullアドレスとCLEARならUNKNOWN。

## Gateと証拠の境界
本書は02A/02Bの現在BAN状態と不完全な外部情報を安全に扱う要件に基づくDraftであり、Confirmedではない。テスト用の照会関数は実際のPaper IP BANリストやUUID→IP履歴の完全性を証明しない。正式CAT Process/PI/TCE GateとPaper実機試験は未実行。

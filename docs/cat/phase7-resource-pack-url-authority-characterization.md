---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.resource-pack-url-authority-characterization
status: candidate
process: simplecoins.java-resource-pack-url-validation
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b9b1c59203b230d6812f4f46318ea248e6a3b508/src/main/java/co/surumene/simplecoins/domain/ResourcePackUrl.java
  - https://github.com/surumeneco/SimpleCoins/blob/b9b1c59203b230d6812f4f46318ea248e6a3b508/src/test/java/co/surumene/simplecoins/domain/ResourcePackUrlTest.java
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: Java リソースパック URL authority の code-to-spec Candidate

## Observation（既存コードから確認できる振る舞い）

対象は `ResourcePackUrl.validate(String)` の既存実装（Git blob `10ca43e289a0dd1a8bb39038949a9336ea81706a`）。`new URI(value).parseServerAuthority()` を使用し、scheme は大文字小文字を区別せず http/https に制限する。host 必須、userinfo・fragment 不可、明示 port は 1〜65535 の範囲でなければ拒否する。クエリ文字列は検証中に改変しない。呼出し自体はネットワークアクセスを行わない。

既存の `ResourcePackUrlTest` は基本的な誤構文、userinfo、fragment、範囲外port、署名付きHTTPS URLおよび未設定値を試験している。IPv6 authority／範囲両端の明示port／空fragment／percent-encoded userinfo／schemeの大小文字・クエリ保持を独立して網羅したテストは、参照した `develop` HEAD には存在しない。

## Candidate（既存挙動の文字起こし、規範判断ではない）

| ID | 入力境界 | 既存コードから導かれる観測候補 | 分類 |
| --- | --- | --- | --- |
| RPU-AUTH-01 | IPv6 literal authority と正規の署名クエリ | 元URLを変えず受理 | positive |
| RPU-AUTH-02 | 大文字schemeとPercent-encoded query | 元URLを変えず受理 | positive |
| RPU-AUTH-03 | 明示port境界 1,65535 と 0,65536 | 範囲内受理、範囲外 `IOException` | boundary |
| RPU-AUTH-04 | IPv6 authority内のuser情報／percent-encoded user情報 | `IOException` | negative |
| RPU-AUTH-05 | 空fragment `#` と fragment本文 | `IOException` | negative |
| RPU-AUTH-06 | ユーザー指定値の前後ASCII空白 | trimしたURLを返す | observation |

## Question（規範未確定）

- 既存実装で受理されるURLの全てが、配布先の運用・セキュリティポリシー上も許可されるかは未決定。外部公開URLへの接続や証明書検証は本件で扱わない。
- ここでのCandidateは既存コードの振る舞いの回帰固定だけであり、規範仕様をConfirmedに昇格するものではない。

## 検証境界

ローカルJDK 21で元Git blobとbyte-identicalなソースを直接 `javac` した事実と、別途行う呼出しテストの結果を区別する。正式CAT semantic Gate・Paper/Geyser/JE/BE実機検証・配布パック検証は、本Candidateの作成では通過しない。テスト追加があっても製品の完成認定には使わない。

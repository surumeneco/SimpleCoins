---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.je-pack-url-userinfo
status: draft
process: simplecoins.phase7.pack-verification
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: hosted Java pack URL userinfo boundary (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.phase7.pack-verification | `docs/verification/verify-je-pack.sh` に入力される外部HTTP(S) URLのuserinfo（認証情報）境界 |
| simplecoins.phase7.pack-handoff | 公開URLの検証失敗を、配布済み・受入済みの証拠としない |

既存の `docs/verification/phase7-je-be-pack-handoff.md` は埋込み認証情報を不正URLとして扱う。本Draftはその期待と現行verifierの観測を分離する。認証情報を含むURLの拒否は候補であり、規範CAT Gateや実クライアント受入を通過したと主張しない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| PACK-URL-USERINFO-01 | URLに `user:password@` が含まれる | 認証情報入りURLを受け入れず、ZIP一致を成功として報告しない | PACK-URL-USERINFO-01 |
| PACK-URL-USERINFO-02 | 認証情報のない同一ローカルHTTP URL | 既存の正常ZIP一致検証を維持する | PACK-URL-USERINFO-02 |
| PACK-URL-USERINFO-03 | 認証情報入りURLを拒否する際 | 入力したユーザー名・パスワードを標準出力・エラーへ漏らさない | PACK-URL-USERINFO-03 |

## Test model / TCE

- PACK-URL-USERINFO-01: localhostで配信する同一ZIPを `http://user:password@127.0.0.1:<port>/generated.zip` から要求し、成功終了を拒否する。テスト値は架空のものだけを使う。
- PACK-URL-USERINFO-02: 同じZIPを `http://127.0.0.1:<port>/generated.zip` で検証し、従来どおり一致成功を観測する。
- PACK-URL-USERINFO-03: 失敗結果のstdout/stderrにテスト用userinfoが出現しないことを確認する。

## 観測と検証境界

`develop` のverifier blob `4ae34b19572e8dcb3d678c292a4ec706f0a5b021` を同一git blob SHAで隔離再現し、Python localhost HTTP serverから同一ZIPを配信した。userinfoなしとuserinfoありの両方がexit 0を返した（後者は期待Red）。本Draftは現行挙動の観測と候補テストであり、本番ソース修正・規範決定ではない。

**Draft / not Confirmed.** 正式CAT Process/PI/TCE、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformance、JDK25 Gradle、JE/BE/Paper実機はnot-run。userinfo拒否のGreen実装やdevelop統合をこのDraftのみで許可しない。

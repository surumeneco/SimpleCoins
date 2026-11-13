---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.guard
status: draft
process: simplecoins.paper.failclosed-guard
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1AZxqlD11YUxDgpe10DYtG18ht5IoMZgc/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.guard
semantic_contract: markdown-v2
language: ja
---

# TCE: startup guard admission truth table (Draft)

網羅要求: closed

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| g00 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g01 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g02 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g03 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g04 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g05 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g06 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g07 | 接続検証 | `入力.mainEnabled == 偽 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g08 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g09 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g10 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g11 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 偽 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g12 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 偽` |
| g13 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 偽 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |
| g14 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 偽` | 1 | `出力.allowLogin = 真` |
| g15 | 接続検証 | `入力.mainEnabled == 真 かつ 入力.serviceRegistered == 真 かつ 入力.verifiedAdmission == 真 かつ 入力.stopping == 真` | 1 | `出力.allowLogin = 偽` |

このDraftは既存のConfirmed入室可否を置換しない。サービス検証の実装方法と起動依存の前提は別途実機検証する。

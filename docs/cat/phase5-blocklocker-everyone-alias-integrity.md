---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.everyone-alias-integrity
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BlockLocker Everyone runtime alias integrity (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Runtime Everyone localization aliases and visible text normalization |
| simplecoins.settlement | Prevent unsupported public ownership from malformed alias evidence |

既存の `BlockLockerEveryoneText.matches` が受け取る、実行時に検証された Everyone ローカライズ名の集合。BlockLocker の看板の実在・付属関係、PDC、primary owner、保存済み/loaded の一貫性は対象外。元の `phase5-blocklocker-everyone-text.md` (PR #8) にある表示テキスト照合規則を補足する独立した入力整合性ベクトルであり、確定済み仕様への昇格ではない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| everyone.alias.normalized | runtime alias に旧式色コードが含まれる | 表示行と alias の双方を同一の旧式書式正規化で比較する。ブラケットは runtime alias に含めない | BL-EVERYONE-ALIAS-01 |
| everyone.alias.blank-after-format | alias が書式コードのみで、正規化後に可視名がない | 他の有効 alias が同時に存在しても、集合全体を不正として IOException で拒否する | BL-EVERYONE-ALIAS-02 |
| everyone.alias.exact | 正規化後に未知の可視名や余分な文字がある | Everyone と推定しない。実行時 alias 以外の英語名を補完しない | BL-EVERYONE-ALIAS-03 |

## Test model / TCE

- BL-EVERYONE-ALIAS-01 (Red): `[Everyone]` と runtime alias `§aEveryone` は一致する。大小文字と表示側書式も同一基準で扱う。
- BL-EVERYONE-ALIAS-02 (Red): `Set.of("Everyone", "§r")` は、先に有効 alias が一致しても IOException を返す。
- BL-EVERYONE-ALIAS-03: `[Everyone] other` は書式を除去しても一致しない。

## 実測・Gate

2026-11-02、`develop` の `BlockLockerEveryoneText.java` blob `8e8900c3c26126de082fbbffa3e30e9712123cc2` と同じロジックを Java 21 で直接実行し、ALIAS-01 と ALIAS-02 が期待通り失敗することを確認。これは規範 Gate の代替ではない。

**Draft / not Confirmed.** Process/PI/TCE の正規確認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformance および Paper/BlockLocker 実機受入は not-run。production の修正や develop/main への反映は本 Draft のみを根拠に行わない。

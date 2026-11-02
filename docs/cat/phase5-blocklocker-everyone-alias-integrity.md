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

既存の `BlockLockerEveryoneText.matches` が受け取る、実行時に検証された Everyone ローカライズ名の集合。BlockLocker の看板の実在・付属関係、PDC、primary owner、保存済み/loaded の一貫性は対象外。元の `phase5-blocklocker-everyone-text.md` (PR #8) にある表示テキスト照合規則を補足する独立した入力整合性ベクトルであり、確定済み仕様への昇格ではない。BlockLocker v1.14.1 の `ProfileFactoryImpl` は `Translator.getAllWithoutColor(TAG_EVERYONE)` を使って角括弧付き alias を構築し、表示側を `ChatColor.stripColor` して比較する。`ConfigTranslator` は設定値の色コードを除去した名前を返す。従って色付き alias の正規化を要求する旧案は上流 API 契約と一致せず撤回する（上流 blob `19ad9bdc4f6af6746df83dc3cd571768e2ad97b5` / `6667932d76e6dad1aa4a54f5dfee56877266377d`）。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| everyone.alias.uncolored | runtime alias が実行時の無色名であり、表示行だけが旧式色コードを含む | 表示行の色コードを除去して角括弧付き alias と比較する | BL-EVERYONE-ALIAS-01 |
| everyone.alias.formatted-input | runtime alias に旧式色コードが含まれる（書式のみを含む場合も同様） | BlockLocker の無色 alias 契約に反するため、他の有効 alias が一致しても IOException で拒否する | BL-EVERYONE-ALIAS-02 |
| everyone.alias.exact | 正規化後に未知の可視名や余分な文字がある | Everyone と推定しない。実行時 alias 以外の英語名を補完しない | BL-EVERYONE-ALIAS-03 |

## Test model / TCE

- BL-EVERYONE-ALIAS-01 (Green baseline): `§b[EVERYONE]§r` は無色 runtime alias `Everyone` に一致する。表示側のみ旧式色コードを除去する。
- BL-EVERYONE-ALIAS-02 (Red): `Set.of("Everyone", "§r")` と `Set.of("Everyone", "§aOther")` は、先に有効 alias が一致しても IOException を返す。
- BL-EVERYONE-ALIAS-03: `[Everyone] other` は書式を除去しても一致しない。

## 実測・Gate

2026-11-02、`develop` の `BlockLockerEveryoneText.java` blob `8e8900c3c26126de082fbbffa3e30e9712123cc2` と同じロジックを Java 21 で直接実行し、書式付き runtime alias の受理/不一致に関する探索的な2件の差分を確認した。上流契約確認後、ALIAS-01 は既存の Green として再定義し、ALIAS-02 の不正入力拒否を未実装の Draft 要求として残す。これは規範 Gate の代替ではない。

**Draft / not Confirmed.** Process/PI/TCE の正規確認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformance および Paper/BlockLocker 実機受入は not-run。production の修正や develop/main への反映は本 Draft のみを根拠に行わない。

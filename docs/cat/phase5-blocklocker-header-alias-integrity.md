---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.header-alias-integrity
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BlockLocker localized header alias integrity (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Runtime localized PRIVATE / MORE_USERS header aliases supplied to `BlockLockerHeaderMatcher.match` |
| simplecoins.settlement | Malformed runtime aliases must not establish an authoritative protection header |

既存の `BlockLockerHeaderMatcher` は、看板の表示行と実行時alias集合を区別する。BlockLockerの `ChestSettingsImpl.getSimpleLocalizedHeaders` は `Translator.getAllWithoutColor` を呼び、`ConfigTranslator` は無色aliasを返す。したがって、表示行には旧式色コードがあってもよいが、runtime alias入力に色コードが含まれる場合は上流契約に合致しない。実際のBlockLocker signの物理的付属、PDC、owner、saved/live一貫性は別途検証する。

上流根拠：BlockLocker commit `25b6224d52f4368d0fe73700d41851650d526d8d`、`ChestSettingsImpl.java` blob `88264cd547e03ca6366992cfb60fb8791df032fb`、`ConfigTranslator.java` blob `13d157d11077043f29e2dc1f06522b4938df91ec`。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| blocklocker.header.alias-uncolored | runtime PRIVATEまたはMORE_USERS aliasに旧式色コードが混入する | 他の有効aliasが一致しても不正なalias集合全体をIOExceptionで拒否する | BL-HEADER-ALIAS-01, BL-HEADER-ALIAS-02 |
| blocklocker.header.display-colored | runtime aliasが無色で、表示行に旧式色コードと余白がある | 表示行のみを正規化し、対応する正しいヘッダー種別を返す | BL-HEADER-ALIAS-03 |
| blocklocker.header.unknown | 表示行が未知の文字列・余分な文字を含む | 近似一致や部分一致から保護看板を推定しない | BL-HEADER-ALIAS-04 |

## Test model / TCE

- BL-HEADER-ALIAS-01 (Red): `[Private]` と `§a[Privat]` の混合集合は、表示が `[Private]` に一致しても拒否する。
- BL-HEADER-ALIAS-02 (Red): MORE_USERS側の `§b[More Users]` も、表示側が正しくても拒否する。
- BL-HEADER-ALIAS-03 (Green baseline): 無色alias `[Private]` に対し ` §b[PRIVATE]§r ` は PRIVATE と一致する。
- BL-HEADER-ALIAS-04: `[Private] extra` は一致しない。

## 検証・Gate

対象の `develop` `BlockLockerHeaderMatcher.java` Git blob `804f9987c616222ae81b10ce57ab31236f259337` に対する、既存の `phase5-blocklocker-localized-header.md` (PR #8) を補足する入力契約のテスト準備である。productionの修正は行わない。

**Draft / not Confirmed.** 正規CAT Process/PI/TCEの承認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformanceとPaper/BlockLocker実機受入は not-run。Redの再現は仕様確定や本番修正許可を意味しない。

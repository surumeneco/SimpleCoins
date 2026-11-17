---
cat_version: '0.2'
kind: work
id: simplecoins.a04.blocklocker-saved-profile-slots
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/4
  - https://github.com/surumeneco/SimpleCoins/pull/290
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04 — BlockLocker保存PDCの6プロフィールキー境界回帰

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/290 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/BlockLockerSavedHints.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/Lifecycle/Works/simplecoins.a04.blocklocker-saved-sign-paper-source.md |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.a04.blocklocker-saved-profile-slots | simplecoins.a04.blocklocker-saved-sign-paper-source;simplecoins.settlement.default-deny |

## Technologies

| Technology |
| --- |
| Java25; JUnit5; read-only PDC Map; actual BlockLockerSavedHints; pinned CAT-TDD issue-work validator |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.blocklocker-saved-profile-slots.md;src/test/java/co/surumene/simplecoins/domain/BlockLockerSavedHintsProfileSlotCandidateTest.java;.github/workflows/a04-blocklocker-profile-slot-regression.yml | — | src/test/java/co/surumene/simplecoins/domain/BlockLockerSavedHintsProfileSlotCandidateTest.java;.github/workflows/a04-blocklocker-profile-slot-regression.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a04.profile.slots | tests | junit | @repo:SimpleCoins | gradle :test --tests co.surumene.simplecoins.domain.BlockLockerSavedHintsProfileSlotCandidateTest | slots1to6;malformedSuffix;unrelatedExtension;orphanProfile | src/test/java/co/surumene/simplecoins/domain/BlockLockerSavedHintsProfileSlotCandidateTest.java | build/test-results/test/TEST-co.surumene.simplecoins.domain.BlockLockerSavedHintsProfileSlotCandidateTest.xml | 300 |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## 現行仕様・既存技術証拠と検証範囲

02A/02BではBlockLocker保護と所有者の整合がP/F/D全件決算の前提である。ただし、BlockLocker保存看板の単独PDCヒントは正本所有者とは限らず、表示テキスト、実地形の看板・チェスト位置、両面保護、UUIDと現在のBAN/playerdataを別途照合しなければならない。既存の実Paper BlockLocker 1.15検証では、正規保護看板の保存PDCヒントが存在しない実例もある。**このWorkはPDCヒントを所有権の権威に昇格させない。**

旧[#290](https://github.com/surumeneco/SimpleCoins/pull/290)の独立したJunit4件は、現developではパスが存在せず未収録だが、今の`BlockLockerSavedHints.read`に対するテストであり製品コードを変更せず実行できる。旧ブランチを直接mergeせず、既存製品実装に対して4件をそのまま移植する。

1. `blocklocker:profile_1`から`_6`の全6つで、`blocklocker:e`が値ではなく**存在**でEveryoneを示すこと、未知UUIDを勝手に所有者にしないことを確認する。
2. `_0`／`_7`／先頭ゼロ／余分な末尾／制御改行／全角数字・アラビア数字などの非正規プロフィール接尾辞は例外でfail-closed。
3. `unrelated:profile_7`と`blocklocker:profile`のような別キーは、正規プロフィールを捏造しない。
4. ヘッダーの無い単独`blocklocker:profile_6`はorphan evidenceとして拒否する。

独立して4件のJUnit XML、既存Domain/Phase6のPR HEAD CI、CAT `issue-work` Work構造検証、実テストのGit Blobが旧PR #290と一致することを確認する。合格なら検証限定単位を`develop`へ統合し旧PRを重複クローズする。これは実Paperプラグインを起動しない、型Mapベースの**静的な意味境界検証**に限られ、保存ソース全件閉包、BlockLocker 1.15正本、G0、入室開放、正式CAT Gateは未達のまま。

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.companion-identity-matrix
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: companion commander / owner state matrix (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.owner | 取得元が検証済みの commander と owner の三値証拠を統合する純粋な判定境界 |
| simplecoins.settlement | 未取得・未ロード・API不在を検証済み不在へ昇格させない |

本Draftは既存の `CompanionOwnerResolution.resolve`（PR #8）の三値状態を網羅するテスト準備である。WWC/WWW/WCL API の取得、保存済み個体の網羅性、外部BAN、実機の決算公開を検証したものではない。既存の `phase5-external-owner-ban-evidence.md` の COMPANION-OWNER-01〜04 を具体化し、確定済み仕様を追加しない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| companion.matrix.commander | commander=KNOWN、owner=KNOWN/ABSENT/UNKNOWN | owner の状態に関係なく検証済み commander UUID を PERSONAL として返す | COMPANION-MATRIX-01 |
| companion.matrix.fallback | commander=ABSENT、owner=KNOWN/ABSENT/UNKNOWN | それぞれ owner PERSONAL / FREE / UNKNOWN とし、verified absence のときだけfallbackする | COMPANION-MATRIX-02 |
| companion.matrix.unknown | commander=UNKNOWN、owner=KNOWN/ABSENT/UNKNOWN | すべて UNKNOWN とし、owner から補完しない | COMPANION-MATRIX-03 |
| companion.matrix.invalid | null / 不整合な identity evidence | 不正な証拠を拒否する。UNKNOWN を ABSENT に変換しない | COMPANION-MATRIX-04 |

## Test model / TCE

- COMPANION-MATRIX-01: commander=KNOWN で owner の三状態を変えても commander UUID の PERSONAL を返す。
- COMPANION-MATRIX-02: commander=ABSENT の owner 三状態が PERSONAL / FREE / UNKNOWN に対応する。
- COMPANION-MATRIX-03: commander=UNKNOWN の owner 三状態がすべて UNKNOWN に対応する。
- COMPANION-MATRIX-04: null / 矛盾する state-UUID の組を拒否する。

## 検証・Gate

対象の現行元ソースは `feature/owner-evidence-20261101` の `CompanionOwnerResolution.java` Git blob `5ec98bb7968d64b442fad7b0f7cc6e57eab3699d`。このブランチは元PR #8の本番実装を変更せず、テストのみを追加する。テスト実行結果と対応HEADは別途記録する。

**Draft / not Confirmed.** 規範Process/PI/TCEの確認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformanceおよびPaper/WWC/WWW/WCL実機受入は not-run。テストGreenをCAT Gate通過やsettlement受入とみなさない。

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.saved-source-drift
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# 保存済み資産の走査中のファイル変化（Draft）

## Targets

| Ref | Aspect |
| --- | --- |
| WorldSnapshotTraversal | 読取済み保存元の状態検証 |
| AnvilRegionReader | 単一保存リージョンの読取 |
| PlayerDataReader | プレイヤーデータ読取 |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| DRIFT-01 | region/entity .mcaが訪問中に拡張・置換された | 完了レポートを返さずIOException | changedRegionAborts |
| DRIFT-02 | playerdataが読取コールバック中に変化 | 完了レポートを返さずIOException | changedPlayerDataAborts |
| DRIFT-03 | 当初存在しなかった必須playerdataが走査途中に現れた | 当初のauthorityと異なるため完了扱いしない | latePlayerDataAborts |
| DRIFT-04 | 保存元のメタデータが前後で安定 | 成功レポートを維持する | stableSourcesPass |

## Scope

これはmTime・サイズ・物理ファイル識別子の変化の検出であり、完全な時点一致の証明ではない。同じ属性を維持した内容変更や全検査後の更新には別の静止条件と正本検証が必要。メモリ内ワールドの読み取り、Paperスレッド制約、所有者APIおよび全件決算の公開許可は本Draftの対象外。ConfirmedやCAT Gate通過を主張しない。

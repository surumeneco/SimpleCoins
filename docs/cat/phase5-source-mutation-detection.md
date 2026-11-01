---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.source-mutation
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 保存済み資産sourceの走査中変更検知

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot | authoritative region/entity/playerdataのread-only走査 |
| simplecoins.anvil | 外部.mcc sidecarとchunk callback後の再検証 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| source.stability.region | region/entity fileがchunk visitor中に変更されたとき | IOExceptionで中断し成功Reportを返さない | SC-SOURCE-STABILITY-01 |
| source.stability.player | playerdataがplayer visitor中に変更されたとき | IOExceptionで中断し成功Reportを返さない | SC-SOURCE-STABILITY-02 |
| source.stability.unchanged | regionとplayerdataが走査中不変のとき | 正しい件数のReportを返す | SC-SOURCE-STABILITY-03 |
| source.stability.replaced | 必須sourceが消失・置換されたとき | 空資産とせずfail-closedで拒否する | SC-SOURCE-STABILITY-04 |
| source.stability.sidecar | 参照外部.mcc sidecarがchunk callback中に変更されたとき | Anvil readerがIOExceptionで中断する | SC-SOURCE-STABILITY-05 |
| source.stability.sidecar-unchanged | 正しい外部.mcc sidecarが不変のとき | 正常に読み取る | SC-SOURCE-STABILITY-06 |
| source.stability.sidecar-late | 既読sidecarが同じregion内の後続chunk callbackで変更されたとき | 成功前に既読sidecarを再検証し拒否する | SC-SOURCE-STABILITY-07 |
| source.stability.sidecar-after-region | region走査後のplayerdata callbackが以前のsidecarを変更したとき | 全体Report返却前の再検証で拒否する | SC-SOURCE-STABILITY-08 |

## 境界と状態

既存SC-SOURCE-STABILITY-01〜08を意味変更せず構造化したTASK-20261101-003のCAT Draft。各ファイルをNOFOLLOWで読み取る前後にfile type・physical file key・size・creation/modified timeを比較するbest-effort検出であり、同じsize/timeへの復元やloaded Paper memoryの変更を完全には検知しない。完全なsnapshot・loaded/disk照合・公開ゲートは別途必要。正式CAT Process/PI/TCE・機械Gateはnot-run。Confirmed・マージ・決算公開を意味しない。

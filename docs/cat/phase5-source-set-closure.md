---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.source-set-closure
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 保存済みsource集合の前後closure

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot | 権威source集合の走査前後一致 |
| simplecoins.world | caller指定world rootとscope |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| closure.pre-added | manifest生成後・走査前にplayerdata sourceが増えたとき | visitor前に拒否 | SC-CLOSURE-01 |
| closure.post-added | 最終callback中にplayerdata sourceが増えたとき | 走査後に拒否し成功Reportを返さない | SC-CLOSURE-02 |
| closure.unchanged | 権威source集合が前後で一致するとき | 通常Reportを返す | SC-CLOSURE-03 |
| closure.scope | excluded worldのregionとworld非依存playerdataがあるとき | region除外を維持しplayerdataは対象に含める | SC-CLOSURE-04 |
| closure.invalid | source path不正・非通常ファイル・発見予算枯渇のとき | fail-closedで拒否 | SC-CLOSURE-05 |
| closure.identity | 固定world root/scopeで前後比較するとき | world名・source kind・path・player UUIDの一致を要求 | SC-CLOSURE-01, SC-CLOSURE-02 |
| closure.post-removed | 最後のplayer callbackが先に読み終えたplayerdataを削除するとき | 最終source stamp再検証で拒否し成功Reportを返さない | SC-CLOSURE-06 |
| closure.post-region-added | 最後のplayer callbackが同じworldに新たなregion sourceを作るとき | 再発見したsource集合の差異を拒否し成功Reportを返さない | SC-CLOSURE-07 |

## 境界と状態

既存SC-CLOSURE-01〜05の意味を変更せず、削除とregion追加の回帰をSC-CLOSURE-06/07で補うTASK-20261101-003のCAT Draft。bounded再発見比較はbest-effortであり、最終確認後の変更防止、loaded memory整合、Paper world freeze、決算公開を保証しない。正式CAT Process/PI/TCE・機械Gateはnot-run。Confirmed・マージ・リリースを意味しない。

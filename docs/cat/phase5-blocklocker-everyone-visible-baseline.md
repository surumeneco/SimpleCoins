---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.blocklocker-everyone-visible-baseline
status: candidate
process: simplecoins.blocklocker
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/50054b543acb7b73e312426d3181062a239e4919/src/main/java/co/surumene/simplecoins/domain/BlockLockerEveryoneText.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# BlockLocker Everyone visible-tag baseline (code-to-spec Candidate)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker.everyone-visible | Existing text-only `BlockLockerEveryoneText.matches` implementation, Git blob `8e8900c3c26126de082fbbffa3e30e9712123cc2` |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| BL-EV-01 | 表示側に旧式カラーコード・余白・大文字小文字差、実行時aliasは無色 | 対応する角括弧付きタグをtrueとする | coloredVisibleTagMatchesUncoloredRuntimeLocalization |
| BL-EV-02 | 全体が角括弧付きaliasに一致しない表示 | 部分一致・余分な角括弧・前後の別文字列をfalseとする | onlyTheEntireBracketedVisibleTagCanMatch |
| BL-EV-03 | 表示テキストnull、alias集合nullまたは空 | IOException | missingRuntimeEvidenceFailsClosed |
| BL-EV-04 | alias集合にnull、空白のみ、角括弧混入が一つでもある | 有効aliasが一致してもIOException | anyMalformedAliasRejectsTheEntireRuntimeSet |

## 根拠と未決定

これは既存実装からの**Candidate**であり、正しいBlockLocker 1.14.1の全契約として承認された仕様ではない。表示行の一致は、物理的な保護看板の存在、支持先、主所有者、他の有効看板、PDC、ワールド/保存状態の整合を証明しない。runtime aliasの旧式カラーコード拒否など既存のDraft Red問題は別Workで扱い、本Candidateで合格扱いにしない。

正規CAT Process/PI/TCEのConfirmed、process-closure、model-conformance、implementation-conformance、Paper実機検証は未実施。今回の直接Java21検証は既存ソースの観測範囲だけを示す。

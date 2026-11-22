---
cat_version: '0.3'
kind: tce
id: simplecoins.settlement.writer-epoch-publication.tce.authorize
status: confirmed
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs:
  - simplecoins.settlement.writer-epoch-publication.pi.authorize
semantic_contract: markdown-v2
language: ja
---

# TCE: G0公開試行・未公開保留・既公開失効

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| p0 | 公開判定 | `入力.必要ソース完全確認済 == 真 かつ 入力.新規writer入場拒否実効 == 真 かつ 入力.既入場writer排出完了 == 真 かつ 入力.全候補が同一保護世代 == 真 かつ 入力.帰属重複仕訳照合済 == 真 かつ 入力.通常仕訳監視への引継可能 == 真 かつ 入力.オンラインプレイヤー不在 == 真 かつ 入力.世代失効あり == 偽` | 1 | `出力.原子公開試行許可 = 真; 出力.未公開維持 = 偽; 出力.公開済を失効 = 偽` |
| p1 | 公開判定 | `入力.世代失効あり == 偽 かつ (入力.必要ソース完全確認済 == 偽 または 入力.新規writer入場拒否実効 == 偽 または 入力.既入場writer排出完了 == 偽 または 入力.全候補が同一保護世代 == 偽 または 入力.帰属重複仕訳照合済 == 偽 または 入力.通常仕訳監視への引継可能 == 偽 または 入力.オンラインプレイヤー不在 == 偽)` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 真; 出力.公開済を失効 = 偽` |
| p2 | 公開判定 | `入力.世代失効あり == 真` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 真; 出力.公開済を失効 = 真` |
| r0 | 失効通知 | `入力.世代失効あり == 真` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 真; 出力.公開済を失効 = 真` |
| r1 | 失効通知 | `入力.世代失効あり == 偽` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 偽; 出力.公開済を失効 = 偽` |

## 閉包の限界

全ての必須Booleanを観測に紐付けた場合の公開試行・保留・失効の分岐であり、実環境の全writerが閉じたことや原子的publishが成立したことは立証しない。r1は既公開のまま維持してよいという許可でもなく、単に失効通知時点で新たな操作を指示しない。旧候補の再公開、監視を失った後の再Permit、未知のwriterを「無い」とする挙動は禁止。

実Paper受入は、非協調Paper native保存、既入場InvSee++未来Future、LIVE貨幣増減、額不変のowner移動、WorldLoad/Unload ABA、通常仕訳引継、監視喪失、失敗後永続拒否の各区別を要する。既存 #470/#471、#505/#510、#559 の負例を再利用し、同じテストの量産は行わない。各CAT Gateの実行結果はWork/CIの証拠で別管理し、Process/Modelの分岐検証を全writerの実運用閉包、原子的公開またはImplementation Conformanceの成立と混同しない。

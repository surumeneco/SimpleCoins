---
cat_version: '0.3'
kind: tce
id: simplecoins.currency.tce.station-result-local
status: confirmed
process: simplecoins.currency.station-result-local-decision
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/529
  - https://github.com/surumeneco/SimpleCoins/pull/532#issuecomment-6976353886
decision_ref: user-chat:2026-11-18:A12-station-result-local-decision-approved
refs: 
  - simplecoins.currency.pi.station-result-local
semantic_contract: markdown-v2
language: ja
---

# TCE: ステーション結果取得の局所判断（局所確定仕様）

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| s0 | 判定 | `入力.対象結果操作 == 偽` | 1 | `出力.許可 = 真; 出力.取消 = 偽` |
| s1 | 判定 | `入力.対象結果操作 == 真 かつ 入力.入力読取可能 == 真 かつ 入力.貨幣マーカーあり == 偽` | 1 | `出力.許可 = 真; 出力.取消 = 偽` |
| s2 | 判定 | `入力.対象結果操作 == 真 かつ 入力.入力読取可能 == 真 かつ 入力.貨幣マーカーあり == 真` | 1 | `出力.許可 = 偽; 出力.取消 = 真` |
| s3 | 判定 | `入力.対象結果操作 == 真 かつ 入力.入力読取可能 == 偽` | 1 | `出力.許可 = 偽; 出力.取消 = 真` |

## 分岐の意味

真偽3項目の8組合せを排他的に分類する。s0は非対象操作を拒否しない。s1は通常PAPER等の結果受領を妨げない。s2は正規・不正PDCマーカーを持つ入力での結果消費を禁止する。s3は入力枠を検証できない場合fail-closedする。

本TCEは**局所判定の入出力のみ**の網羅仕様であり、ステーションの全操作、後続Plugin、直接NMS操作、JE/BEクライアント、保存・G0/G3を閉包しない。既存Confirmed A12の未証明範囲を縮小したと見なさない。

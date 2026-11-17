---
cat_version: '0.3'
kind: tce
id: simplecoins.currency.tce.station-result
status: draft
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/489
decision_ref: ''
refs:
  - simplecoins.currency.pi.station-result
semantic_contract: markdown-v2
language: ja
---

# TCE: 通常PAPERステーションでの貨幣消費阻止（Draft）

網羅要求: partial

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| s0 | 結果操作 | `入力.結果操作 == 偽` | 1 | `出力.許可 = 真; 出力.取消 = 偽` |
| s1 | 結果操作 | `入力.結果操作 == 真 かつ 入力.入力読取可能 == 真 かつ 入力.貨幣マーカーあり == 偽` | 1 | `出力.許可 = 真; 出力.取消 = 偽` |
| s2 | 結果操作 | `入力.結果操作 == 真 かつ 入力.入力読取可能 == 真 かつ 入力.貨幣マーカーあり == 真` | 0 | `出力.許可 = 偽; 出力.取消 = 真` |
| s3 | 結果操作 | `入力.結果操作 == 真 かつ 入力.入力読取可能 == 偽` | 0 | `出力.許可 = 偽; 出力.取消 = 真` |

## 代表的な検査ベクトル候補
- Merchantの入力slot0／slot1／両方、Cartographyの入力slot0／slot1／両方それぞれに正規貨幣、改造PDC貨幣、正常PAPERを投入し、結果slot2の各操作で期待拒否・許可を照合。
- 素材の通常PAPERだけ、結果枠以外の入れ替え、Shift・数字キー・ダブルクリック、出力生成後の他Plugin上書き、クリック中の入力変更を分ける。
- 実Paperクライアントでの最終消費（貨幣残存と結果未発生）と、イベントキャンセルだけを別証拠にする。

本TCEは**手動イベント上の部分Draft**であり、実際のMerchantレシピ成立条件・CartographyItemEvent・Paperのすべての最終消費分岐に対する正式な閉包ではない。判定関数JUnit Greenは、イベント配線やネイティブ交易の受入を意味しない。

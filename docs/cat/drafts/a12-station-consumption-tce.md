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
  - https://github.com/surumeneco/SimpleCoins/pull/491
  - https://github.com/surumeneco/SimpleCoins/pull/492
  - https://github.com/surumeneco/SimpleCoins/pull/493
  - https://github.com/surumeneco/SimpleCoins/pull/494
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

## 実測に基づく代表的な検査ベクトル候補

- **Merchant native matcher**：実`MerchantOffer.satisfiedBy/take`は通常PAPERを受理／消費しSTONEを拒否、正規PDC/model 5000C貨幣も**受理・1→0へ減算**（PR #491）。これは実取引のネットワークイベントや報酬取得ではない。
- **Cartography native result**：実MapView付FILLED_MAP（slot0）＋通常PAPER（slot1）→FILLED_MAP、同じ地図＋正規5000C貨幣（slot1）→FILLED_MAP。貨幣が**slot0**の場合や地図なしは結果なし（PR #492）。slot0に貨幣があれば常に実消費可能だとする旧対称ベクトルは誤りであり、入力監視を安全側に拡張する仮説と混同しない。
- **Cartography native take**：結果`Slot.onTake`により普通紙・正規5000Cのコピーが両方1→0（PR #493）。さらに`CartographyTableMenu.clicked`直接呼出しでも両方1→0、結果FILLED_MAP取得、Bukkit InventoryClickEvent/CartographyItemEventは**0件**（PR #494）。直接NMS呼出しはネットワークイベント経路をテストしない。
- **未検査で必須**：Merchant結果slot2、Cartography結果slot2の実ネットワーククリック／Shift・数字キー・ダブルクリック、通常PAPERでの許可、正規貨幣・不正マーカー・読取失敗での取消を各イベントから最終現物まで照合。結果枠以外の移動を誤ブロックせず、後段の他Plugin上書き・NMS直接変更・保存時の資産/仕訳矛盾を別々に検証。

## 適用境界

本TCEは四つの入力分岐の**部分Draft**で、事後の別Pluginによる変更、Paperネットワーク→イベント→NMS処理の順序、直接NMS呼出しへの完全排他、全クラッシュ・G0/G3を閉包しない。`s2/s3`での拒否・取消は**受入仕様候補**であって、実プロダクトに実装済み、実イベントでキャンセル成功、正式CAT Confirmed/Gate成立という意味ではない。

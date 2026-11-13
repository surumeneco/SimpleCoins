---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-coin-codec-integrity
status: candidate
process: simplecoins.currency.paper-codec
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/315
  - https://github.com/surumeneco/SimpleCoins/pull/355
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paper 26.2実体貨幣Codecの独立統合 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| coin-identity | PAPER素材・固定額面PDC・額面ごと固定item_model |
| coin-presentation | 表示名と内部識別子の分離 |
| verification | Paperの型・静的スキーマ検証および後続実機試験 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| codec-01 | 全9額面の内部識別 | 名前変更に依存しない固定`simplecoins:coin_denomination`整数PDCと額面固有`simplecoins:coin_<n>`モデルを用いる | 9額面と固定IDのJUnit |
| codec-02 | 名前設定を変更 | 9額面が揃うか検証し空欄を拒否し、識別子は変更しない | 名前の完全性とモデル不変性テスト |
| codec-03 | 貨幣生成数が範囲外 | 1〜64枚を超える生成を拒否する | Paper側ItemStack実機テスト |
| codec-04 | 通常PAPERにマーカーがない | 貨幣と誤認しない | Paper実機にて確認 |
| codec-05 | マーカーがあり素材・額面・モデルが不正 | 通常紙扱いして価値を0へ捨てず不正を拒否する | 不正PDC/ItemModel実機検証 |
| codec-06 | 保存NBTの既存額面と照合 | 保存走査`StoredItemValue`と同じ正規のキー・額面・モデルを維持 | #355の保存NBT回帰 |
| codec-07 | 単体Codecのみ実装 | 両替・クラフター保護・行商人・Nexo/JE/BE表示・参加許可の完成を主張しない | 未統合の機能範囲 |

## 境界

この文書は既存ソースと確定済み仕様から抽出したCAT Candidateであり、正規のProcess Closure/Model Conformance/Confirmed昇格を行ったことを示さない。JDK25のJUnitスキーマ検証は実Paperサーバー上のItemStack/PDC動作の代替にならない。

---
cat_version: '0.3'
kind: process
id: simplecoins.paper.coin-exchange
status: draft
process: simplecoins.paper.coin-exchange
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.exchange
semantic_contract: markdown-v2
language: ja
---

# Process: 20 Paper shapeless coin exchanges (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.exchange |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 等価両替 | simplecoins.paper.pi.exchange/craft |

## 解釈境界
n=1,10,100,1000ごとの5形式・計20種。投入額と出力額は一致する。PaperでのRecipeChoice.exactChoice利用は現物一致候補であり、表示名の変更前後に全貨幣を受け付けるか、Shift/クラフター/他プラグイン/バニラ紙レシピを完全に防ぐかは独立の実機検証が必要。両替以外のPAPER用途流出を許さないことは依然必須。この技術候補はその完成を宣言しない。

## 非クラフト消費の対象（Draft検証候補）
通常のPAPERとして消費される追加経路のうち、商人取引と製図台の結果スロット（raw slot 2）を実イベントで保護する。入力のどちらかに貨幣PDCマーカーがある場合は結果取得をキャンセルし、通常PAPERは従来どおり許可する。商人取引はPaper固有の取引確定イベントでも再判定する。Shiftクリックと通常クリックを同じ結果取得のガードに含める。これは他プラグインによる直接Inventory操作やすべてのPAPER用途の閉世界保証ではなく、正式受入は後続の実機操作試験を要する。

## 結果アイテムの改変・なりすまし対策（Draft検証候補）
同一NamespacedKeyを持つrecipeでも、その出力が規定のPDC額面・固定item_model・数量から逸脱した場合は許可しない。CraftItemEventでは結果スロット、CrafterCraftEventではgetResultをそれぞれ照合する。入力のみでは出力価値保存を証明できないため、正しい入力と不正な出力の組合せを拒否する。Paperでの登録所有権・他プラグインによる後続MONITOR変更などは別の安全境界として残す。

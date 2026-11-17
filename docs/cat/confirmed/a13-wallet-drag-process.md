---
cat_version: '0.3'
kind: process
id: simplecoins.wallet.drag-isolation
status: confirmed
process: simplecoins.wallet.drag-isolation
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A13-lower-only-drag-correction-approved
refs: 
  - simplecoins.wallet.drag-isolation.pi.assess
semantic_contract: markdown-v2
language: ja
---

# Process: 財布GUIのドラッグによる変更範囲の隔離

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.wallet.drag-isolation.pi.assess |

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
| ドラッグ判定 | simplecoins.wallet.drag-isolation.pi.assess/assess |

## 対象と限界

既存の財布GUIで、通常のプレイヤーインベントリ下段だけに作用するアイテム分配は、財布本体と上段9個の疑似通貨表示を変更しない場合に限り通常動作を許す。上段に触れる操作、財布の固定保持スロットへの操作、財布をドラッグ元として使う操作、元の財布とプレイヤーの一致が確認できない操作は拒否する。前段プラグインのキャンセルを解除してはならない。要求の根拠はDrive 01「入出金操作」の通常インベントリ準拠と、2026-11-18の利用者による限定修正の実行指示である。

ここでの入力はPaper APIから検証して得た判定値であり、領域は財布GUIが開かれたイベント1件の受理可否に限る。BukkitのInventoryDragEvent raw slot/ローカルslot変換、実JE/BEクライアントでの入力再現、同時変更する外部Plugin、上段ドラッグ入金の新規実装、G0/G3の保存・決算保証は別の技術検証責務であり、この純粋な判定に包含しない。

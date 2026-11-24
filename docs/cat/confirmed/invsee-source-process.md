---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.invsee-source
status: confirmed
process: simplecoins.accounting.invsee-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/Jannyboy11/InvSee-plus-plus/blob/v0.31.22/InvSee%2B%2B_Common/src/main/java/com/janboerman/invsee/spigot/api/InvseeAPI.java
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs:
  - simplecoins.accounting.invsee-source.pi.capture
semantic_contract: markdown-v2
language: ja
---

# Process: InvSee++通常仕訳の正本選択

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.invsee-source.pi.capture |

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
| 収納読取 | simplecoins.accounting.invsee-source.pi.capture/capture |

既決02Bの通常仕訳の局所観測境界。オンライン対象は実Playerを一度だけ読み、InvSee鏡像を加算しない。オフライン対象は有効な実InvSee編集画面の当該収納区画のみ優先し、その他の区画は保存playerdata。画面終了後は実保存結果を読む。前後差分は閲覧者と同じtick仕訳に反映する。未確定・読取不能な正本を0としない。新規通貨や帰属規則の追加ではなく、既決の通常仕訳への提供者接続。

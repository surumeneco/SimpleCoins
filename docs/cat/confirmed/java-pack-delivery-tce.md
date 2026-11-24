---
cat_version: '0.3'
kind: tce
id: simplecoins.display.java-pack-delivery.tce.request
status: confirmed
process: simplecoins.display.java-pack-delivery
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#外部連携と非機能要件
refs: 
  - simplecoins.display.java-pack-delivery.pi.request
semantic_contract: markdown-v2
language: ja
---

# TCE: Java版配布判定

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| send | 配布判定 | `入力.Nexo委任 == 偽 かつ 入力.Java接続 == 真 かつ 入力.配布情報準備済 == 真` | 1 | `出力.要求許可 = 真` |
| skip | 配布判定 | `入力.Nexo委任 == 真 または 入力.Java接続 == 偽 または 入力.配布情報準備済 == 偽` | 1 | `出力.要求許可 = 偽` |

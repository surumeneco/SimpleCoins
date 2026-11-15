---
cat_version: '0.3'
kind: pi
id: simplecoins.world-source-discovery.pi.manifest
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/429
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 保存Manifestの正本範囲と再発見検証 (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.settlement.saved-source-coverage | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.実ルート有効 | 受信 | 真偽 | 必須 |
| 入力.プレイヤー正本一意 | 受信 | 真偽 | 必須 |
| 入力.孤立バックアップあり | 受信 | 真偽 | 必須 |
| 入力.記号リンクあり | 受信 | 真偽 | 必須 |
| 入力.探索予算以内 | 受信 | 真偽 | 必須 |
| 入力.ソース集合不変 | 受信 | 真偽 | 必須 |
| 出力.Manifest受理 | 送信 | 真偽 | 必須 |
| 出力.失敗通知 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| discover | 受付 | 発見 |
| recheck | 受付 | 集合再検証 |

## 制約
| 式 |
| --- |

## ソース範囲
- `実ルート有効`: Paperから捕捉したディメンションの絶対パスと、それと同じ実レベルの共有プレイヤールートが分離・限定され、存在する通常ディレクトリとして検査済みであること。World名から兄弟フォルダを推測して採用してはならない。
- `プレイヤー正本一意`: `UUID.dat`を全ワールド横断で一度だけ数え、`UUID.dat_old`を現行正本に追加しないこと。主ファイルとバックアップが並存するときは二重計上しない。
- `孤立バックアップあり`: 対応主ファイルが欠損したバックアップが存在すれば、復旧承認のないままゼロ円とみなさない。
- `記号リンクあり`: 読取対象のシンボリックリンク・親エイリアス・別パスからの同一ファイル重複を曖昧な複数正本として許容しない。
- `ソース集合不変`: 最初の発見集合と走査完了後の同じルート・WorldScope・訪問予算での発見集合が一致すること。途中ABAやメモリ同世代性をそれ単独では証明しない。

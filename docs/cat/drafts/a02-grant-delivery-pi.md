---
cat_version: '0.3'
kind: pi
id: simplecoins.grants.pi.delivery
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/pull/434
  - https://github.com/surumeneco/SimpleCoins/pull/437
  - https://github.com/surumeneco/SimpleCoins/pull/467
  - https://github.com/surumeneco/SimpleCoins/pull/468
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 永続予約・受領PDC・現物の配布確定 (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.playerdata-persistence | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.予約あり | 受信 | 真偽 | 必須 |
| 入力.保存済み現物受領対 | 受信 | 真偽 | 必須 |
| 入力.予約全額受領済 | 受信 | 真偽 | 必須 |
| 入力.保存世代証拠有効 | 受信 | 真偽 | 必須 |
| 入力.他収納保全 | 受信 | 真偽 | 必須 |
| 入力.未ack | 受信 | 真偽 | 必須 |
| 入力.矛盾あり | 受信 | 真偽 | 必須 |
| 出力.ack許可 | 送信 | 真偽 | 必須 |
| 出力.予約維持 | 送信 | 真偽 | 必須 |
| 出力.異常遮断 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| prepare | 受付 | 配布検査 |
| verify | 受付 | 保存後照合 |
| recover | 受付 | クラッシュ復旧 |

## 制約
| 式 |
| --- |

## 意味境界
- `保存済み現物受領対`: 対象UUIDの実`players/data/UUID.dat`を再読取し、**同じファイル世代の**指定貨幣・数・額面・PDCモデルと予約ID/単調列のreceiptが矛盾なく存在すること。受領PDCだけ、通貨だけ、合致しない連番は不成立。
- `予約全額受領済`: 正規発行台帳で対象の未ACK予約を確定し、その予約の総額が**実保存済みプレイヤーデータ**内の現物貨幣証明と完了受領PDCで一致していること。完了連番は対象予約連番と一致し、active受領はなく、予約の途中受領値だけでは真にならない。**部分受領なら**保存済み現物受領対が真であっても予約全額受領済は偽としてACKを禁止し、未引渡分だけを予約に維持する。完了連番や数値を呼出し側が作っただけでは真にならない。
- `保存世代証拠有効`: 実際の読戻し・変更監視・必要な永続化確認による証拠。単なる`saveData()`の戻り、メモリ上のreceipt、呼出し側が渡したsequence、過去ファイルの内容は証拠ではない。
- `他収納保全`: 変更対象以外の既存アイテム・収納・装備・エンダーチェストが失われず、遺失/複製の不明状態がないこと。
- `未ack`: 予約が永続台帳で未配布扱いであること。既確定を再配布しない。
- `矛盾あり`: 保存不能・PDCと現物不一致・同一性不明・改変を含む。未確定予約を黙って消さず、回復不能な異常は参加・集計を閉じる。

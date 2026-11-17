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
  - https://github.com/surumeneco/SimpleCoins/pull/473
  - https://github.com/surumeneco/SimpleCoins/pull/480
  - https://github.com/surumeneco/SimpleCoins/pull/499
  - https://github.com/surumeneco/SimpleCoins/pull/500
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
- `保存世代証拠有効`: 実際の読戻し・変更監視・必要な永続化確認に加えて、**その検査後から永続ACK確定までの同じ正本世代を保証できること**を要する入力。単なる`saveData()`の戻り、メモリ上のreceipt、呼出し側が渡したsequence、過去ファイルの内容、検査直後までのSHA/ctime一致は、検査後に非協調writerが介在できる環境では十分な証拠ではない。`真`とできる環境前提・writer境界の選定は未完了であり、AIが本Draftで採用済みと宣言しない。
- `他収納保全`: 変更対象以外の既存アイテム・収納・装備・エンダーチェストが失われず、遺失/複製の不明状態がないこと。
- **実Paperの空装備Compound**：[PR #480](https://github.com/surumeneco/SimpleCoins/pull/480)の保存前`equipment`欠損／保存後`equipment={}`は、両世代で装備アイテム集合が空のままであるという限定観測。これだけを独立装備の消失/増殖と判定しない正規化候補とする。`SelectedItemSlot`や実`equipment.mainhand`の同値性を、この実測から勝手に仮定しない。非空`head`/`offhand`/防具/その他保存位置の実改変、構文異常、財布の重複、Ender Chest変更は引き続き`他収納保全=偽`／矛盾なら異常遮断。現行実装は空/欠損を拒否するため、この意味は**Draftで未実装**。
- `予約あり`: 対象UUID・予約ID・連番・額面と枚数を照合できた**正規永続台帳の発行履歴**であり、未ACK・既ACKのいずれも含む。過去に存在した履歴と今回新規に配布する予約を混同しない。
- `未ack`: 対象の正規永続予約が現時点で未配布扱いであること。`偽`は**台帳の永続ACKを再読取して照合した状態**に限り、メモリ上の処理完了フラグから推定しない。既確定を再ACK/再発行せず、ゲーム内で正常に移動・消費した貨幣を発行直後の元スロットに固定する要求も設けない。
- `矛盾あり`: 保存不能・PDCと現物不一致・永続済みACKよりも古いreceiptへの巻戻り・台帳に存在しないID/連番の受領・同一性不明・改変を含む。**無予約だから異常なしとは推定しない**。証拠源が未取得・評価不能なら`偽`と断定しない。未確定予約を黙って消さず、回復不能な異常は参加・集計を閉じる。
- `既ACKの再起動`: 保存済み物理貨幣の現在の所在は、後続の合法なプレイヤー取引・移動により発行当初と異なり得る。既ACKを根拠に発行直後の貨幣を再配置しない。履歴・receiptのロールバックと世界資産の未計上は別途G0/通常仕訳で検証する。
- `保存前クラッシュ`: 永続予約の後でメモリだけに適用した貨幣・PDC更新は未配布として扱う。再起動時に同じplayerdata世代を読取って現物が未保存であることを確かめ、予約を保持して一度だけ配布できることは[#473](https://github.com/surumeneco/SimpleCoins/pull/473)の隔離Paper試験で確認済み。実クライアントが操作中だった状態やOS電源断の正確な保証ではない。

## 協調writerと非協調writerの意味境界（技術的補足）

- [#499](https://github.com/surumeneco/SimpleCoins/pull/499)のテスト専用`closeAndDrain()`は、**既入場permitがゼロになってから**保存を始め、ACKまで新規permitを拒否する必要条件を示した。これを実Paper製品のwriter fenceとして採用した証拠ではない。
- [#500](https://github.com/surumeneco/SimpleCoins/pull/500)の実InvSee++は、保存イベントを同期通知してから実`saveData`を後続処理として実行する。初回イベントを通した後に新規受付を閉じても、初回writerは実保存を完了する。2回目だけをキャンセルできても`入力.保存世代証拠有効`は真にならない。
- `入力.保存世代証拠有効`は、少なくとも対象playerdataの全writerが判明し、**(i)** gate開始後の新規入場拒否、**(ii)** 既入場writer全員の排出、**(iii)** その後の真正保存・fsync・fresh同世代現物+receipt読戻し、**(iv)** 永続ACKまでの排他保持を実施できる場合のみに限る技術候補。非協調ファイルwriterの存在・列挙漏れ・排出待ち失敗は、不明を偽へ倒すのではなくACK不可/異常遮断条件にする。
- これらのプロトコルはまだ**製品未実装**。完了した`GrantLedger.acknowledge`の冪等性だけではwriterの排他を提供しない。全writer対象の凍結方法・証拠取得方法は引続き別途技術選択/正式CAT Gateを要する。

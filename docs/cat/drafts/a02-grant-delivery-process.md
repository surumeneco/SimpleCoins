---
cat_version: '0.3'
kind: process
id: simplecoins.grants.paper-save-commit
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/437
  - https://github.com/surumeneco/SimpleCoins/pull/467
  - https://github.com/surumeneco/SimpleCoins/pull/468
  - https://github.com/surumeneco/SimpleCoins/pull/473
decision_ref: ''
refs:
  - simplecoins.grants.pi.delivery
semantic_contract: markdown-v2
language: ja
---

# Process: Paperプレイヤーデータ保存を伴う配布確定 (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.grants.pi.delivery |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- |
| 観測.永続予約台帳 | GrantLedger独立ジャーナル | 真偽 |
| 観測.プレイヤーデータ正本 | Paper 26.2の対象UUID.datの保存後読戻し | 真偽 |
| 観測.物理貨幣と受領証明 | 同一UUID.dat世代内の収納とPDC receipt | 真偽 |
| 観測.予約全額受領 | 対象の本物の未ACK予約に対して、保存済みPDCの完了連番・非active状態と現物の累計全額の証拠がそろうか | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.台帳ack | GrantLedgerへの永続的配布確定 | 真偽 |
| 作用.配布候補遮断 | メモリ上の未確定物理配置・未公開仕訳 | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 配布検査 | simplecoins.grants.pi.delivery/prepare |
| 保存後照合 | simplecoins.grants.pi.delivery/verify |
| クラッシュ復旧 | simplecoins.grants.pi.delivery/recover |

## 要求と保証の分離
03/02Bの承認済み要求では、予約受付は現物発行ではなく、保存後にのみ発行仕訳と台帳確定が可能。金額・現物貨幣・受領PDCは整合したプレイヤーデータ世代に存在しなければならず、切断・満杯・保存失敗・中断・再起動によって二重配布または未配布を生じさせない。

保存済み現物と受領PDCの対が正しくても、予約全額の保存完了を意味するとは限らない。途中受領は予約IDと累計配布額を記録して未ACKを維持し、空き枠発生後に残額のみを保存する。最終分の実ファイル照合と非activeの完了受領を確認できたときに限りACK候補となる。#467は途中受領保存後SIGKILL、#468は全額保存後ACK前SIGKILLから、両者の違いを実Paper上で確認した。

予約が存在しない通常の無操作は、**全証拠が整合している場合**だけ異常なしと判定する。履歴にないreceiptやACK済み履歴に対するreceiptロールバックは、予約の有無だけで正常扱いせず異常遮断する。既ACK履歴は再発行・再ACKの対象ではなく、後続の合法的な貨幣移動を理由に発行時と同じ収納スロットの保持まで要求しない。ACK後の資産所在変化は通常仕訳/G0の責務と分離する。

[#473](https://github.com/surumeneco/SimpleCoins/pull/473)では、実Paperの隔離された第8 JVMで予約4の永続記録後・メモリ上の現物/receipt変更後に**playerdata保存前SIGKILL**し、第9 JVMで未保存の第4貨幣不在とpending維持を確認して、実保存・読戻し・ACKを一度だけ実行した。第10 JVMでは実ファイルと台帳の再読取で貨幣合計665,000 C、配布済み履歴4件、未配布0件、ACK冪等性を確認した。これは合成オフラインプレイヤーのJVM停止境界の実測であり、実クライアント操作・OS電源断・全外部writerには適用しない。

**技術上の重要な不足**：保存済playerdataの一度の読戻し・内容SHA/ctime証明が成功しても、その直後から永続ACKまで無通知の外部writerが変更できれば、古い受領/貨幣を根拠にACKしてしまう競合を排除できない。従ってTCEの`入力.保存世代証拠有効`を、検査瞬間だけの整合として真とみなさない。保護範囲と永続化の保証を別途選定・実証する必要がある。現行の読取専用ブリッジだけではG3の製品配布サービスを安全に有効化できない。

実Paperで`CraftPlayer.saveData()`が成功値なしで戻り、保存無効・ファイル書込み失敗を黙殺し得ることを既存CIは確認した。`GrantLedger.acknowledge(grant,seq)`はディスク上の現物証拠を引数に要求しない。このProcessは配布サービスに必要な保存証拠を規範候補として記述するが、`fsync`/OS電源断の保証境界、同時接続・外部Pluginの変更とジャーナル整合は未決であり、正式なConfirmedではない。

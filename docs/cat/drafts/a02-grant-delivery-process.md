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
  - https://github.com/surumeneco/SimpleCoins/pull/480
  - https://github.com/surumeneco/SimpleCoins/pull/499
  - https://github.com/surumeneco/SimpleCoins/pull/500
  - https://github.com/surumeneco/SimpleCoins/pull/502
  - https://github.com/surumeneco/SimpleCoins/pull/505
  - https://github.com/surumeneco/SimpleCoins/pull/510
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

**Paper保存表現の等価性（未承認の正規化候補）**：実Paper 26.2 [#480](https://github.com/surumeneco/SimpleCoins/pull/480) の対象プレイヤー保存で、変更前は`equipment`フィールド欠損（null）、変更後は空のCompound（`{}`）だった。装備アイテムの増減は確認されず、正規5000C貨幣・受領PDCが同一playerdataに保存され単一root証明は成功したが、現行`verifyTransition`のNBT構造一致判定はこの空/欠損差を「他収納の改変」と誤判定し、G3正常配布を妨げた。**mainhandアイテム自体の鏡像変化は、このログからは立証されない。** `他収納保全`の実装候補として、*空の`equipment` Compoundと未出現だけ*を同等と扱い、非空の装備変化、誤型、異常なnested貨幣、EnderItems/Inventory/receiptの変更は従来どおり拒否する。これはDraftの意味検討であり、正式CAT Confirmed・修正済み実装ではない。

**技術上の重要な不足**：保存済playerdataの一度の読戻し・内容SHA/ctime証明が成功しても、その直後から永続ACKまで無通知の外部writerが変更できれば、古い受領/貨幣を根拠にACKしてしまう競合を排除できない。従ってTCEの`入力.保存世代証拠有効`を、検査瞬間だけの整合として真とみなさない。保護範囲と永続化の保証を別途選定・実証する必要がある。現行の読取専用ブリッジだけではG3の製品配布サービスを安全に有効化できない。

実Paperで`CraftPlayer.saveData()`が成功値なしで戻り、保存無効・ファイル書込み失敗を黙殺し得ることを既存CIは確認した。`GrantLedger.acknowledge(grant,seq)`はディスク上の現物証拠を引数に要求しない。このProcessは配布サービスに必要な保存証拠を規範候補として記述するが、`fsync`/OS電源断の保証境界、同時接続・外部Pluginの変更とジャーナル整合は未決であり、正式なConfirmedではない。

## 既入場writerの排出が必要な保存世代証拠（技術観測・Draft）

[#499](https://github.com/surumeneco/SimpleCoins/pull/499)は、協調writerの入場許可→新規writer受付拒否→**既入場writer全員の排出完了**→本物の圧縮playerdata保存・file/parent fsync→本物の保存済貨幣+receipt読戻し→`GrantLedger`永続ACKまで排他を保持し、ACK後にだけ明示再開する**テスト専用**プロトコルをJUnitで検証した。これは実Paper保存/外部Plugin writerに配線していない。

[#500](https://github.com/surumeneco/SimpleCoins/pull/500)では、**実配布InvSee++ 0.31.22＋公式Paper 26.2**で、初回`SpectatorInventorySaveEvent`を許可したMONITOR時点で新規拒否を開始しても、通過済みの初回保存が後から完了し、正規5000C現物だけが`UUID.dat`から消え、完了receiptと未ACK台帳が残ることを実証。2回目の保存は拒否される。後から登録した拒否は、既入場writerの書込みを遡及的に取り消せない。**この反例で「新規拒否開始＝writer排出済」とする方式を棄却**する。

[#502](https://github.com/surumeneco/SimpleCoins/pull/502)では実配布InvSee++の**既に許可済みの1件の保存Future**を最後まで待ち、その1件が正規貨幣を保全したことを実UUID.dat/receiptで確かめた後に、ファイル/親ディレクトリfsync→新規読戻し→元予約ACK→異常終了→別Paper JVMの現物/台帳復元を実証した。これは**既知の1件を排出できた場合**の技術正例であり、他入口のwriter全件が止まった証明ではない。

[#505](https://github.com/surumeneco/SimpleCoins/pull/505)では同じ公式Paper 26.2・実InvSee++で、InvSee保存イベントの事前拒否が確実に効いた後でも、同じオフラインCraftPlayerの**Paper標準`Player.saveData()`を直接呼ぶと、InvSeeイベントを一度も追加発火させずに同一UUID.datを上書き可能**と実証した。テストでslot1から正規5000Cのみを除去すると、署名済み完了receiptとSTONEを残したまま現物が0となり、実ファイル/親ディレクトリの同期と異常終了後の別Paper JVMでも同じ内容が復元された。元台帳は未ACKでfresh readbackは拒否したので、誤ACKの発生を主張しない。この反例によって**InvSee++固有の`SpectatorInventorySaveEvent`拒否をPaper標準保存まで包含する共通writer fenceにする案を棄却**する。実Paper APIの全保存イベント有無や、別pluginの無通知ファイル操作を網羅したという意味ではない。

[#510](https://github.com/surumeneco/SimpleCoins/pull/510)では、既知InvSee++保存Futureが正常に終了し、新規InvSee保存も拒否され、file/dir fsync後の**新しい正しい現物+receipt読戻し**を得た後、ACK直前に**別入口のPaper標準`CraftPlayer.saveData()`**を実行した。その保存はInvSee拒否イベントを通らず正規5000C現物のみを消し、完了receiptは保持した。直後のfresh読戻しは欠損を正しく拒否するが、実験のため意図的に**古い証拠の連番を低レベル`GrantLedger.acknowledge`へ渡す**と誤ACKが永続化し、別Paper JVMで現物0・ACK済みを再現した。これは**不正な低レベル呼出しの反例**であり、未接続の製品coordinatorが実際に誤ACKしたことを意味しない。#502の正例と矛盾せず、正例で制御できたwriter集合がPaper native保存を包含していなかったことを示す。保存検査とACKを別々の時点で通過しただけでは同一世代保証にならない。

したがって、`観測.プレイヤーデータ正本`や`観測.物理貨幣と受領証明`は、一度のファイル検査だけでなく「全当該writerが入場前から追跡され、拒否開始後には既入場処理がすべて終了し、その状態を**最終ACK永続化まで**維持」の実装可能な環境前提が必要。イベントに通知せず直接書換えるPlugin・Paper保存・外部ファイル操作の存在下でその前提が不明なら、`保存世代証拠有効`を真とみなさず未ACK/入室拒否を保つ。後日正常に取得した貨幣が移動することは許容し、発行済スロットを永久固定しない。

**技術的な未決**：全writerの列挙/排他の実装方法、旧writer待ちの上限・タイムアウト時の停止、main-thread/asyncのdeadlock回避、Crash後の安全な再開、OS電源断での保存/ACK同時耐久性。この節は承認済みの正規貨幣配布要件に基づく**設計候補と実測**であり、AIが正規CAT Confirmedを付与したものではない。

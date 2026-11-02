---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.root-lifecycle
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 発行ジャーナルのdurable root lifecycle

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.journal | durable root・head・物理directory identity |
| simplecoins.grant | pending・receipt・発行予約の安全な継続条件 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal.root.deleted | 構築後にjournal rootとheadを削除したとき | pendingをfail-closedで拒否する | SC-GRANT-ROOT-01 |
| journal.root.replaced-file | 構築後にrootを通常ファイルへ置換したとき | validateReceiptをfail-closedで拒否する | SC-GRANT-ROOT-02 |
| journal.head.missing | rootは存在するがdurable headが欠損・非通常ファイルのとき | 未配達予約を公開せず拒否する | SC-GRANT-ROOT-03 |
| journal.root.healthy | 正常な新規ledgerのroot/headが検証できるとき | 空pendingとreceipt=0を許可する | SC-GRANT-ROOT-04 |
| journal.root.latch | 異常を検出したrootを同じパスに再作成したとき | 既存ledgerをunhealthyのまま維持し、明示のrestart/replayを要求する | SC-GRANT-ROOT-05 |
| journal.head.rollback | durable headが形式上有効だがin-memory最新sequenceより古いとき | pending・receipt照会・追加発行を拒否する | SC-GRANT-ROOT-06 |
| journal.root.identity | 構築後にrootの物理ディレクトリが同じパスの別実体へ置換されたとき | 構築時の物理identityと照合し、継続使用を拒否する | SC-GRANT-ROOT-01, SC-GRANT-ROOT-05 |
| journal.root.ancestor | journal pathの祖先がsymbolic linkへ変化したとき | root健全性を失効させ、再起動・復旧まで後続操作を停止する | SC-GRANT-ROOT-05 |

## 境界と状態

既存SC-GRANT-ROOT-01〜06を意味変更せず構造化したTASK-20261101-005のCAT Draft。rootの同一ホストlifecycle guardはcross-process transaction、全履歴replay、filesystem identity再利用に対する完全保証を意味しない。正式CAT Process/PI/TCE・機械Gate、Paper実機の配達・クラッシュ復旧はnot-run。Confirmed・マージ・デプロイを意味しない。

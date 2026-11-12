---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.external-mcc-content-drift
status: candidate
process: simplecoins.settlement.saved-sources
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/335
refs: []
semantic_contract: markdown-v2
language: ja
---

# Anvil外部圧縮チャンクの保存世代ドリフト検出 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| anvil-sidecar | Anvilの外部圧縮チャンク `c.<x>.<z>.mcc` |
| saved-source-generation | 全件走査で整合を照合する保存ファイル集合 |

## 観測

Anvil Regionの外部チャンクは同じディレクトリの `c.<x>.<z>.mcc` に圧縮NBTを保持する。現行 `AnvilRegionReader` はこの外部ファイルを読んで訪問するが、`WorldSnapshotTraversal.SourceStamp` が照合するのは `.mca` とplayerdataだけである。`.mca` を変えずに外部NBTを書き換えると、全件決算の入力が変わっても正本の変更を検出しない。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| sidecar-01 | Anvilヘッダが外部 `.mcc` を参照する | 訪問前に対象ファイルの物理同一性・長さ・mtime・SHA-256を採取し、全訪問後に再確認する | 02B・保存ソースの全件正本照合 |
| sidecar-02 | 訪問中に `.mcc` の内容だけが書換えられた | 同一サイズ・同一mtimeでもIOExceptionで全走査を無効とする | 既存 `.mca` ドリフト防止との整合 |
| sidecar-03 | 外部 `.mcc` が事前欠落、途中削除、物理ファイル差替、ハードリンクで別ソースと重複 | 未確認の値を確定せず走査を拒否する | Fail-closed |
| sidecar-04 | 外部参照しない標準Anvilチャンク | 追加の存在必須ファイルを生まない | バニラ互換 |
| sidecar-05 | ファイル内容が走査前後で一致する | 技術的な保存ファイル照合のみ成功可能 | 完全決算は別責務 |

## Boundary

- 完全な世界の静止・原子的スナップショットを証明するものではなく、保存ファイル入出力上の既知の監視漏れを塞ぐ。
- 未ロード/ロード済み選択、外部プラグイン、BAN、WWC/WWW帰属、通常仕訳、正式CAT Gateおよび入室解禁とは別課題。現状の受付禁止を維持する。
- CAT Candidateであり、Confirmed Process・PI・TCEへの昇格や規範的なGate合格を意味しない。

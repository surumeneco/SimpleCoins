---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.normal-journal-lockfile-identity
status: candidate
process: simplecoins.normal-mutation-journal
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/465068c7a2b26bd7e9eaafc5bfe94dfaba1feb9c/src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# NormalMutationJournal: 書込者ロックの実ファイル同一性 (CAT Candidate)

## 観測

現在の `NormalMutationJournal` は `.writer.lock` への `FileChannel.tryLock()` 成功と `writerLock.isValid()` を検査する。しかし、ロック取得後に同ファイル名が別の実ファイルに置換されても、開いた旧ファイルのロックが有効なままになり得る。別プロセスが新しいファイル名を対象に別ロックを取得すると、従来のロックだけでは同一ディレクトリのwriter排他を保証できない。

## 技術候補・受入ベクトル

| ID | 前提・刺激 | 要求される観測 |
| --- | --- | --- |
| NL-LOCK-01 | ロック取得後、`.writer.lock` が別inodeの通常ファイルに置換される | 次の操作前に永続的失敗として扱い、追加仕訳レコードを生成しない |
| NL-LOCK-02 | ロック取得後、`.writer.lock` が消失またはsymlink化する | 台帳の継続利用を拒否し、異常なパスを黙って再作成しない |
| NL-LOCK-03 | 同じ実ファイルのロックが存続する正常系 | 通常仕訳・照会・再起動時の検証を維持する |

## 制約と未決定

- Java NIOの `BasicFileAttributes.fileKey()` を用いた実ファイル同一性チェックはチェック時点の異常検出であり、ロックとパスの検査を不可分にするものではない。TOCTOUやディレクトリ権限が不正な状況で完全な相互排他を証明しない。
- fileKeyを提供できないファイルシステムでのフェイルクローズと可用性のトレードオフは実運用前の技術検証を要する。
- 現物通貨への仕訳適用・決算・入室許可への接続を行わない。本Candidateは承認済み仕様ではなく、正式CAT Gateは未実行。

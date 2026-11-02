---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.journal.bounded-stream-failure
status: draft
process: simplecoins.journal
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: bounded journal stream short reads and late I/O failure (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.journal | `DurableFile.readBounded` が入力streamを上限まで読み切る際の境界 |
| simplecoins.recovery | 読取途中のIOExceptionや予算超過を、成功した短いrecordとして返さない |

既存の `DurableFile` はchecksummed recordの低レベル読み書き境界であり、世代切替・仕訳再生・クラッシュ後の正しさそのものは検証しない。PR #44のrecord境界、PR #50の物理buffer長/破損record検証とは別に、`InputStream` の部分読取と途中故障の扱いを検証する。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| durable.stream.short-read | 1〜3byteずつ返すstreamを最大8193byteの予算で読み込む | EOFまで連結し、完全なbyte列を返す。1回のreadの長さをEOFと誤認しない | DURABLE-STREAM-01 |
| durable.stream.late-failure | 有効なprefixを返した後にIOExceptionが発生する | 例外を伝播し、prefixを成功したjournalとして返さない | DURABLE-STREAM-02 |
| durable.stream.short-overflow | 小分けreadの合計が最大byte数を超える | 予算超過をIOExceptionとして拒否する | DURABLE-STREAM-03 |
| durable.stream.zero-length | 予算0の空streamと非空stream | 空streamのみ空byte列を返し、1byte以上の入力は拒否する | DURABLE-STREAM-04 |

## Test model / TCE

- DURABLE-STREAM-01: 8193byteを1〜3byteの部分readで返すstreamを完全に復元する。
- DURABLE-STREAM-02: prefix読取後にIOExceptionを投げるstreamをfail-closedで扱う。
- DURABLE-STREAM-03: 3byteずつのreadで上限を超えるとIOExceptionを返す。
- DURABLE-STREAM-04: 上限0で空/非空streamを区別する。

## 検証・Gate

対象は `develop` の `DurableFile.java` blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8`。本Draftは既存実装に対する独立したテスト準備であり、production sourceは変更しない。

**Draft / not Confirmed.** CAT Process/PI/TCEの規範確認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformanceおよびPaper再起動/故障注入受入は not-run。純粋なstreamテストのGreenはクラッシュ整合性の証明ではない。

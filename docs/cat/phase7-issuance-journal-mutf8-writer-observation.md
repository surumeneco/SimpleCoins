---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.issuance-journal-mutf8-writer-observation
status: candidate
process: simplecoins.phase7
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/d9b90a78df491af266651f187ea03e48934cae94/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/d9b90a78df491af266651f187ea03e48934cae94/docs/verification/verify-issuance-journal.py
refs: []
semantic_contract: markdown-v2
language: ja
---

# 発行台帳のJava実writerとPython readerのModified UTF-8境界（code-to-spec Candidate）

## Observation（固定ソース）

- `GrantLedger.encode` は `DataOutputStream.writeUTF(issuer)` でJava Modified UTF-8を記録する。NULは通常のUTF-8の単一ゼロバイトではなく2バイト、補助平面文字はUTF-16サロゲート各3バイトで記録される。
- `GrantLedger.Grant` は `issuer.isBlank()` とJavaのUTF-16 code unit数 `issuer.length() <= 1024` を検査する。外部Python readerの `Reader.utf` はUTF-16 code unit列を復元し、`_grant` は長さと非空白を検査する。
- 既存の実writer fixtureは日本語発行者を検証しているが、NULと補助平面文字の長さ境界を独立した実writer fixtureで拘束していない。

## Candidate（規範承認前の検証対象）

外部診断器は、実Java writerが受理して保存したSCG2をModified UTF-8とUTF-16長さの違いだけで不正扱いしない。Java writerが拒否した長さ超過を、診断器側で勝手に受理可能な記録として生成しない。

| Vector | 実Java writerの発行者 | 検証観点 |
| --- | --- | --- |
| ISS-DIAG-09A | NUL単独 | `writeUTF`のNUL符号化を外部readerが受理 |
| ISS-DIAG-09B | ASCII + U+1F600 | 2個のUTF-16 code unitとして保存・読取 |
| ISS-DIAG-09C | ASCII 1022個 + U+1F600（Java length 1024） | Java境界値で保存されたSCG2の読取 |
| ISS-DIAG-09D | ASCII 1023個 + U+1F600（Java length 1025） | 実Java writerの拒否と台帳無変更を確認 |

## Question / 限界

これは現行実装の観測から導くcode-to-spec Candidateであり、正式な規範仕様やConfirmedではない。実Java writer/readerの交差試験は、Paper実機のオフラインgive一回性、JE/BE、Nexo、playerdata/PDCの同時永続化を証明しない。CAT semantic Gatesは別工程として未実行のまま保持する。

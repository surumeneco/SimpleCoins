---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.journal.symlink-target-write
status: draft
process: simplecoins.journal
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: reject existing symlink at journal record write target (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.journal | `DurableFile.write` の既存record targetがsymbolic linkの場合の安全境界 |
| simplecoins.recovery | 不審なrecord path状態を黙って修復・置換し、証拠消失や誤った正常化を起こさない |

現行 `DurableFile.read` は最終recordのsymbolic linkを拒否するが、`write` は一時ファイルを `ATOMIC_MOVE + REPLACE_EXISTING` するため、既存のsymlinkを黙って置換する。参照先のファイルは書き換わらなくても、既存record pathが不審な状態だったという証拠を消す可能性がある。PR #35の**祖先ディレクトリsymlink**とは別の、最終targetに関する規範判断が必要。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| durable.write.existing-link | targetが既存の有効symbolic link | 書込みをIOExceptionで拒否し、リンクと参照先を保持する | DURABLE-TARGET-01 |
| durable.write.dangling-link | targetが既存のdangling symbolic link | 欠損した通常recordと同一視せずIOExceptionで拒否し、リンクを保持する | DURABLE-TARGET-02 |
| durable.write.regular-replace | targetが通常の既存record | 既存の原子的置換とchecksummed readを維持する | DURABLE-TARGET-03 |

## Test model / TCE

- DURABLE-TARGET-01 (Red): symlink targetへの `write` を拒否し、symlinkと外部sentinelの双方を保持する。
- DURABLE-TARGET-02 (Red): dangling symlink targetへの `write` を拒否し、リンクを保持する。
- DURABLE-TARGET-03 (Green): 通常recordの2回書込みは最新payloadを読める。

## 検証・Gate

対象は `develop` `DurableFile.java` Git blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8`。Linux/JDK21で既存target symlinkが置換されることを再現。これは対象OSの観測であり、Paper実機、Windows filesystem、クラッシュ復旧の保証ではない。既存の上流callerによるpath validationとの責務境界を確認するまでproductionは変更しない。

**Draft / not Confirmed.** CAT Process/PI/TCEの規範確認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformanceおよびPaper再起動/障害注入受入は not-run。意図的Redを仕様確定とみなさない。

---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.ban-source-coverage
status: candidate
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/4
refs: []
semantic_contract: markdown-v2
language: ja
---

# BAN情報源の完結性と決算所有者判定 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| required-sources | この世代で照合を必須と宣言した標準BAN・IP BAN・外部BAN等の情報源ID |
| per-uuid-evidence | UUID単位で取得した情報源ごとのBANNED/CLEAR/UNKNOWNと取得の有無 |
| saved-owners | 保存済みplayerdataおよびBlockLocker・Entityアダプターが特定したUUID |
| source-generation | 発見済みplayerdata集合と同じ決算候補へ伝播するBAN判定 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| ban-coverage-01 | 必須情報源がない・空・不正・重複正規化IDを含む | 確定済みBANスナップショットを作らず拒否 | 空宣言・大文字/空白別名衝突 |
| ban-coverage-02 | UUIDに関し必須情報源が欠ける・UNKNOWNがある・同じ情報源を二度報告する | BANNEDが一件あっても、そのUUIDを全情報源確認済みとは扱わずIOExceptionで拒否 | IP/外部BAN欠損、部分BANNED、UNKNOWN、重複 |
| ban-coverage-03 | 必須情報源を全て報告しUNKNOWNなし | いずれかがBANNEDならBANNED、全てCLEARならCLEARとして記録 | 通常BAN・IP BAN・外部BANの任意一件BAN |
| ban-coverage-04 | 決算時に参照されるプレイヤー/保護者UUIDの報告がない | 非BANやフリーを推測せず拒否する | 未登録UUID照会 |
| ban-coverage-05 | 個人保護対象の主所有者UUIDに有効なplayerdataがある/ない | BANを確認済みの世代値とし、存在する場合は個人またはD、存在しない場合はDとする。EveryoneはF優先 | playerdata有無・BAN・Everyone |
| ban-coverage-06 | 出所が未検証の任意Set<UUID>のみを外部から渡す | このCandidateによる情報源完結性の証明と称さない | 従来stage APIとの証拠境界 |
| ban-coverage-07 | PaperのLIVE Chunk/オンラインPlayerが保存NBTと同じ候補世代に含まれる | 保存側とLIVE側で同じBAN証拠を再利用し、オンラインPlayerが無額でもBAN情報の欠落を検出。LIVE所有者分類の未検証フラグは採用しない | IP BANのみのオンラインPlayer、保存側clear、LIVE missing evidence、従来API互換 |
| ban-coverage-08 | Paperサーバーの標準プロフィールBAN情報をUUIDから取得する | メインスレッドで`OfflinePlayer.isBanned()`を読んだ実観測だけを`paper-profile`のBANNED/CLEARとし、API例外・UUID不一致・証拠欠落はIOExceptionで拒否。IP BAN・外部BANの代替とはしない | 真/偽・失敗・UUID不一致・空UUIDのリーダ単体試験、Paper APIコンパイル |

## Boundary

この候補は外部アダプターから受け取った情報源ID・UUIDごとの状態を厳密に評価するデータ境界である。標準BAN/IP BAN/外部プラグインの現物読取や、IPとUUIDの対応、時点の凍結を証明しない。実際の所有者が不明な場合やBlockLocker・WWC/WWWの連携が未完成なら決算公開は引き続き禁止する。本ArtifactはCandidateでありConfirmed仕様ではない。

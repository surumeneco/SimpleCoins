---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.world-root-ancestor-symlink
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: world root の祖先シンボリックリンク（CAT Draft）

## 対象と解釈境界

WorldStorageDiscovery.discover の「非シンボリックリンク走査」「物理保存sourceの重複防止」「不確実なsourceは部分的な集計に使わない」という既存のDraft契約を、root自身ではなくrootの祖先がsymlinkである場合に拡張して検証する候補。これは規範確定済みProcess/PI/TCEではない。

Java NIOの Files.isDirectory(root, NOFOLLOW_LINKS) は、root 自身が通常ディレクトリでも、途中の親要素がsymlinkであれば通常trueとなる。したがって最終要素だけの非追従検査では、callerが渡したsource rootの実経路を検証できない。

## Draftの振る舞い候補

- SC-SOURCE-ANCESTOR-01: callerが指定したworld rootのいずれかの祖先要素がsymlinkであれば、manifestを返さず IOException でfail-closedする。
- SC-SOURCE-ANCESTOR-02: root自体がsymlinkの場合も従来どおりfail-closedする。
- SC-SOURCE-ANCESTOR-03: symlinkを含まない通常のrootと正規Anvil sourceは従来どおり発見できる。
- world scopeが対象worldを除外していても、playerdataはworld非依存なので、root経路の信頼性を省略しない。
- これらの候補はworld rootの列挙完全性、Paper/Multiverse実機source契約、loaded/diskの整合性、集計結果の公開許可を意味しない。

## テスト準備とGate

WorldStorageAncestorSymlinkTest は上記候補の最小回帰。まず既存実装に対するRed/Greenを確認し、規範CAT Process/PI/TCEの正当な確認・Gate前にproduction guardを変更しない。symlink生成可能なLinux実行環境で試験し、未対応環境では失敗を実装欠陥と混同しない。

状態: Draft（未Confirmed）。formal CAT machine Gates: not-run。Paper/Multiverse live: not-run。

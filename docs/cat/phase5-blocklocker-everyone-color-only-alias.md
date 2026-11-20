---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.blocklocker-everyone-empty-color-alias
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/BlockLockerEveryoneText.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — BlockLocker Everyone color-only aliases (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| everyone-runtime-alias | Validate the complete runtime Everyone localization set before interpreting any visible profile as public ownership |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-EVERYONE-ALIAS-01 | Runtime alias consists solely of legacy Minecraft color/format codes | Reject as malformed (IOException), even when a valid Everyone alias is also present | colorOnlyAliasCannotBeIgnoredAfterPositiveMatch |
| SC-EVERYONE-ALIAS-02 | Runtime alias contains only formatting codes and whitespace | Reject as malformed even if the displayed profile is unrelated | whitespaceAndFormattingOnlyAliasFailsBeforeNegativeMatch |
| SC-EVERYONE-ALIAS-03 | All runtime aliases are valid, with a colored visible Everyone profile | Preserve positive detection | validAliasAndColoredVisibleProfileStillMatch |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-EVERYONE-ALIAS-01 | displayed [Everyone], aliases Everyone + §a | IOException |
| SC-EVERYONE-ALIAS-02 | displayed [Other], aliases Everyone + space-§r-space | IOException |
| SC-EVERYONE-ALIAS-03 | displayed §a[Everyone]§r, aliases Everyone | true |

## Authority and scope

**Draft only.** Existing BlockLockerEveryoneText validates blank and bracketed aliases, but a formatting-only alias is nonblank before visible-text normalization. A positive match from another alias must not hide a malformed runtime configuration. The test class is isolated from the header alias Draft PR #53 and saved-sign palette Draft PR #57. It does not infer sign attachment, chest direction, or ownership from text alone.

CAT Process/PI/TCE confirmation and normative machine Gates, production fix, Paper/BlockLocker live acceptance: **not-run**. Do not merge the expected Red Draft.

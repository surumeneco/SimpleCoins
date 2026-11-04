---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.resource-pack-uri-shape-characterization
status: candidate
process: simplecoins.java-resource-pack-url-validation
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/ResourcePackUrl.java
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Resource-pack URL shape — code-to-spec Candidate

Scope: Existing Java `ResourcePackUrl.validate` only. This characterizes current behavior; it does not approve a new policy, prove remote reachability, or establish JE/BE/Paper delivery.

| Rule | Input boundary | Existing observed behavior |
| --- | --- | --- |
| RPU-SHAPE-01 | Percent-encoded `#` in query and encoded path traversal segment | Preserve the exact URL spelling; do not interpret encoded data as a fragment |
| RPU-SHAPE-02 | HTTP(S) URL with query but no slash path, and explicit standard HTTPS port | Accept and preserve unchanged |
| RPU-SHAPE-03 | Scheme-relative `//host/path` and opaque `https:host/path` | Reject with `IOException` (no valid server authority) |
| RPU-SHAPE-04 | Unicode hostname in raw URI | Reject with `IOException` under current Java `URI.parseServerAuthority` behavior |

Verification: local JDK 21 compiled a source-equivalent transcription of Git blob `10ca43e289a0dd1a8bb39038949a9336ea81706a` and exercised these inputs; not a byte-identical repository checkout. New JUnit characterization is pending direct project execution.

Normative decisions still open: whether encoded traversal segments, localhost/private hosts, Unicode/IDN hosts, and non-HTTPS URLs are permitted by operational policy. No production change, CAT Gate, or delivery acceptance is implied.

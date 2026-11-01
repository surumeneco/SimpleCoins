# Phase 7: Java resource-pack URL validation (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-006. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/03_運営設定・コマンド.md.

## Process / PI

When Nexo is absent, SimpleCoins generates a Java resource-pack ZIP and advertises the configured public URL on player join. An empty URL means the operator has not configured hosting, so the plugin sends nothing. A non-empty URL must be a syntactically valid absolute HTTP(S) URI with a nonempty host, no userinfo and no fragment; a valid path/query (including signed URLs) is retained unchanged. Invalid configuration must not be passed to Paper's resource-pack API. Validation does not prove that the URL is reachable or serves the generated ZIP matching its SHA-1; that remains an external deployment/JE-client test.

## Test model / TCE

- PACK-URL-01 Red: a bare `https://`, malformed URL, unsupported scheme, userinfo, or fragment is rejected.
- PACK-URL-02: valid HTTP(S) URLs including query strings are preserved verbatim; a valid host is mandatory.
- PACK-URL-03: an empty or whitespace-only URL is absent, not a pack delivery attempt.
- PACK-URL-04: `PackDelivery.onJoin` calls the Paper resource-pack method only after successful validation; malformed configuration is logged and skipped.

Formal CAT machine gates, Paper/JE/Geyser/BE client delivery, hosted content/hash verification and Nexo delegation remain not-run.
# BAN evidence locale normalization — code-to-spec Draft

Status: Draft (observed implementation; not Confirmed normative specification)
Flow: code-to-spec
Source: `src/main/java/co/surumene/simplecoins/domain/BanEvidence.java` on `develop`, blob `0e3f29c2a85fca1d711a928b9db7637193805299`.

## Observation

The existing `combine` implementation canonicalizes source IDs using `trim().toLowerCase(Locale.ROOT)`. It rejects duplicate canonical IDs, including case variants, and treats a missing required source as UNKNOWN rather than CLEAR.

## Probe

A deterministic Java regression test should run with a Turkish default locale, where default-locale lowercasing of `IP` differs from `Locale.ROOT`, and verify that required `IP` matches observed `ip`. It should also verify canonical duplicate rejection with mixed case and surrounding ASCII whitespace. This is characterization only, not approval of the policy or a complete BAN provider inventory.

## Gate

No normative confirmation or semantic CAT-TDD Gate is asserted by this Draft. Production behavior must not be changed on the basis of this Draft alone.

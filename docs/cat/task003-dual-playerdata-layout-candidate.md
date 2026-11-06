# TASK-20261101-003 — Dual playerdata layout authority conflict (Candidate)

Status: Candidate (not Confirmed)
Flow: code-to-spec characterization; no production implementation authorized
Source: WorldStorageDiscovery.java on Draft PR #34, branch feature/phase5-world-storage-discovery-20261102-queue-b
Observed behavior: the scanner recognizes both root/playerdata/<UUID>.dat and root/players/data/<UUID>.dat as playerdata candidates, and uses a single UUID-keyed map. Duplicate UUIDs must fail closed even if the two files reside under the same world root.
Proposed regression: create two distinct regular files for the same UUID in these two supported layouts, then assert discover throws IOException rather than selecting one. Also verify two different UUIDs in those layouts are both preserved.
Decision: not a Confirmed normative requirement; production source is unchanged. Formal CAT Gate: not-run. JUnit execution: not-run until independently verified.

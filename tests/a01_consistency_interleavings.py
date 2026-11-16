#!/usr/bin/env python3
"""A01/G0: bounded interleaving model of LIVE vs saved settlement publication.

This is NOT a Paper runtime probe and NOT a CAT Gate. It explicitly models
uncooperative writers; real-Paper witnesses are PR #470 and #471. The model
only answers whether proposed *observation schedules* can prove that a staged
LIVE asset map still equals reality at the point of publication.
"""
from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
import unittest


class Strategy(Enum):
    SAVED_WITNESS_ONLY = "saved-witness-only"
    FINAL_LIVE_RESCAN = "final-live-rescan"
    FULL_COOPERATIVE_GUARD = "full-cooperative-guard"


class Mutation(Enum):
    INCREASE = "increase"
    OWNER_TRANSFER = "owner-transfer"


@dataclass(frozen=True)
class Outcome:
    strategy: Strategy
    mutation: Mutation
    insertion: int
    events: tuple[str, ...]
    published: bool
    stale: bool
    saved_witness_unchanged: bool
    total_preserved: bool


def value_map(mutation: Mutation) -> dict[str, int]:
    # Physical positions/owner-attribution are represented as unique source
    # identities; a pure sum comparison cannot prove correct P/F/D buckets.
    if mutation is Mutation.INCREASE:
        return {"world/chestA:ownerA": 15000, "world/chestB:ownerB": 0}
    return {"world/chestA:ownerA": 0, "world/chestB:ownerB": 10000}


def run(strategy: Strategy, mutation: Mutation, insertion: int,
        *, cooperative_writer: bool) -> Outcome:
    events = ["capture", "saved_read", "saved_witness", "publish"]
    if strategy is Strategy.FINAL_LIVE_RESCAN:
        events.insert(-1, "recapture")
    if strategy is Strategy.FULL_COOPERATIVE_GUARD:
        events.insert(0, "guard_enter")
        events.append("guard_release")
    if not 0 <= insertion <= len(events):
        raise ValueError("Writer insertion outside schedule")
    events.insert(insertion, "writer_mutate")

    actual = {"world/chestA:ownerA": 10000, "world/chestB:ownerB": 0}
    disk = actual.copy()  # persisted MCA stays unmodified throughout
    witness = tuple(sorted(disk.items()))
    observed: dict[str, int] | None = None
    active_guard = False
    published = False
    stale = False
    total_preserved = False
    for event in events:
        if event == "guard_enter":
            active_guard = True
        elif event == "guard_release":
            active_guard = False
        elif event in ("capture", "recapture"):
            observed = actual.copy()
        elif event == "saved_read":
            assert tuple(sorted(disk.items())) == witness
        elif event == "saved_witness":
            assert tuple(sorted(disk.items())) == witness
        elif event == "writer_mutate":
            if active_guard and cooperative_writer:
                continue  # A complete writer barrier must actually block it.
            original_total = sum(actual.values())
            actual = value_map(mutation)
            total_preserved = sum(actual.values()) == original_total
        elif event == "publish":
            if observed is None:
                raise AssertionError("Model published before LIVE source capture")
            published = True
            stale = observed != actual
        else:
            raise AssertionError(f"Unrecognized model operation {event}")
    return Outcome(strategy, mutation, insertion, tuple(events), published,
                   stale, tuple(sorted(disk.items())) == witness,
                   total_preserved)


def enumerate_outcomes(strategy: Strategy, mutation: Mutation,
                       *, cooperative_writer: bool) -> list[Outcome]:
    base_length = 4 + (1 if strategy is Strategy.FINAL_LIVE_RESCAN else 0) \
        + (2 if strategy is Strategy.FULL_COOPERATIVE_GUARD else 0)
    return [run(strategy, mutation, i, cooperative_writer=cooperative_writer)
            for i in range(base_length + 1)]


class A01ConsistencyModelTests(unittest.TestCase):
    def test_saved_file_witness_cannot_detect_live_only_change(self) -> None:
        outcomes = enumerate_outcomes(Strategy.SAVED_WITNESS_ONLY,
                                      Mutation.INCREASE, cooperative_writer=False)
        self.assertTrue(all(o.saved_witness_unchanged for o in outcomes))
        self.assertTrue(any(o.published and o.stale for o in outcomes))

    def test_equal_total_cannot_prove_owner_or_slot_consistency(self) -> None:
        outcomes = enumerate_outcomes(Strategy.SAVED_WITNESS_ONLY,
                                      Mutation.OWNER_TRANSFER, cooperative_writer=False)
        self.assertTrue(any(o.stale and o.total_preserved for o in outcomes))

    def test_a_final_live_rescan_still_has_a_gap_before_publication(self) -> None:
        outcomes = enumerate_outcomes(Strategy.FINAL_LIVE_RESCAN,
                                      Mutation.INCREASE, cooperative_writer=False)
        gap = [o for o in outcomes if
               o.events.index("recapture") < o.events.index("writer_mutate")
               < o.events.index("publish")]
        self.assertEqual(1, len(gap))
        self.assertTrue(gap[0].stale)

    def test_a_complete_writer_barrier_prevents_modelled_stale_publication(self) -> None:
        for mutation in Mutation:
            outcomes = enumerate_outcomes(Strategy.FULL_COOPERATIVE_GUARD,
                                          mutation, cooperative_writer=True)
            self.assertFalse(any(o.stale for o in outcomes))

    def test_uncooperative_writer_bypasses_partial_guard(self) -> None:
        outcomes = enumerate_outcomes(Strategy.FULL_COOPERATIVE_GUARD,
                                      Mutation.INCREASE, cooperative_writer=False)
        self.assertTrue(any(o.stale and
                            o.events.index("guard_enter") < o.events.index("writer_mutate")
                            < o.events.index("guard_release") for o in outcomes))


if __name__ == "__main__":
    unittest.main(verbosity=2)

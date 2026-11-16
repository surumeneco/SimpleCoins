#!/usr/bin/env python3
"""Independently enumerate A02 Draft's seven Boolean inputs.

This is issue-work verification of *Draft rule partitioning*, NOT CAT Model
Conformance and NOT proof that the real Paper/ledger inputs are authoritative.
The truth of the seven inputs is a separate physical/epoch proof obligation.
"""
from __future__ import annotations

import re
from pathlib import Path
import unittest

TCE = (Path(__file__).resolve().parents[1]
       / "docs/cat/drafts/a02-grant-delivery-tce.md")
INPUTS = (
    "入力.予約あり",
    "入力.保存済み現物受領対",
    "入力.予約全額受領済",
    "入力.保存世代証拠有効",
    "入力.他収納保全",
    "入力.未ack",
    "入力.矛盾あり",
)
OUTPUTS = ("出力.ack許可", "出力.予約維持", "出力.異常遮断")
ORIGINS = ("配布検査", "保存後照合", "クラッシュ復旧")
TOKENS = re.compile(r"入力\.[^\s()=]+|==|かつ|または|[()]|真|偽")
ASSIGNMENT = re.compile(r"(出力\.[^\s=;]+)\s*=\s*(真|偽)")


class BooleanExpression:
    def __init__(self, source: str, values: dict[str, bool]) -> None:
        self.tokens = TOKENS.findall(source)
        self.index = 0
        self.values = values

    def pop(self, expected: str | None = None) -> str:
        if self.index >= len(self.tokens):
            raise ValueError(f"Expression ended early; wanted {expected!r}")
        token = self.tokens[self.index]
        self.index += 1
        if expected is not None and token != expected:
            raise ValueError(f"Expected {expected!r}, found {token!r}")
        return token

    def atom(self) -> bool:
        if self.index < len(self.tokens) and self.tokens[self.index] == "(":
            self.pop("(")
            result = self.disjunction()
            self.pop(")")
            return result
        key = self.pop()
        if key not in INPUTS:
            raise ValueError(f"Unrecognized input {key!r}")
        self.pop("==")
        value = self.pop()
        if value not in ("真", "偽"):
            raise ValueError(f"Unrecognized Boolean {value!r}")
        return self.values[key] == (value == "真")

    def conjunction(self) -> bool:
        value = self.atom()
        while self.index < len(self.tokens) and self.tokens[self.index] == "かつ":
            self.pop("かつ")
            another = self.atom()
            value = value and another
        return value

    def disjunction(self) -> bool:
        value = self.conjunction()
        while self.index < len(self.tokens) and self.tokens[self.index] == "または":
            self.pop("または")
            another = self.conjunction()
            value = value or another
        return value

    def evaluate(self) -> bool:
        value = self.disjunction()
        if self.index != len(self.tokens):
            raise ValueError(f"Unparsed condition: {self.tokens[self.index:]}")
        return value


def rows() -> list[tuple[str, str, str, dict[str, bool]]]:
    result = []
    for line in TCE.read_text(encoding="utf-8").splitlines():
        if not line.startswith("| "):
            continue
        parts = [part.strip() for part in line.split("|")[1:-1]]
        if len(parts) != 5 or not re.fullmatch(r"[pvr][0-9]+[a-z]?", parts[0]):
            continue
        case, origin, condition, weight, effect = parts
        if origin not in ORIGINS or weight != "1":
            raise ValueError(f"Unexpected Draft case shape: {case}")
        assigns = {k: v == "真" for k, v in ASSIGNMENT.findall(effect)}
        if set(assigns) != set(OUTPUTS):
            raise ValueError(f"Incomplete Draft outputs: {case}: {assigns}")
        result.append((case, origin, condition, assigns))
    if len(result) != 24:
        raise ValueError("Missing expected Draft branches")
    if len({case for case, *_ in result}) != len(result):
        raise ValueError("Repeated Draft case identity")
    return result


def assignments() -> list[dict[str, bool]]:
    return [{key: bool(mask & (1 << index))
             for index, key in enumerate(INPUTS)}
            for mask in range(1 << len(INPUTS))]


class DraftBooleanPartitionTests(unittest.TestCase):
    def test_each_of_128_input_assignments_matches_one_branch_per_origin(self) -> None:
        cases = rows()
        self.assertEqual(7, len(INPUTS))
        for inputs in assignments():
            for origin in ORIGINS:
                matched = [case for case in cases
                           if case[1] == origin
                           and BooleanExpression(case[2], inputs).evaluate()]
                self.assertEqual(1, len(matched),
                                 f"{origin}: {inputs}: {[c[0] for c in matched]}")

    def test_ack_is_only_eligible_for_full_verified_pending_reservation(self) -> None:
        cases = rows()
        for inputs in assignments():
            for case in cases:
                if not BooleanExpression(case[2], inputs).evaluate():
                    continue
                out = case[3]
                if out["出力.ack許可"]:
                    self.assertIn(case[1], ("保存後照合", "クラッシュ復旧"))
                    self.assertTrue(inputs["入力.予約あり"])
                    self.assertTrue(inputs["入力.未ack"])
                    self.assertTrue(inputs["入力.保存済み現物受領対"])
                    self.assertTrue(inputs["入力.予約全額受領済"])
                    self.assertTrue(inputs["入力.保存世代証拠有効"])
                    self.assertTrue(inputs["入力.他収納保全"])
                    self.assertFalse(inputs["入力.矛盾あり"])
                    self.assertFalse(out["出力.異常遮断"])
                    self.assertFalse(out["出力.予約維持"])
                if inputs["入力.矛盾あり"]:
                    self.assertFalse(out["出力.ack許可"])
                    self.assertTrue(out["出力.異常遮断"])

    def test_replayed_ack_and_contradictory_full_receipt_are_never_reissued(self) -> None:
        cases = rows()
        for inputs in assignments():
            for case in cases:
                if not BooleanExpression(case[2], inputs).evaluate():
                    continue
                out = case[3]
                if inputs["入力.予約あり"] and not inputs["入力.未ack"]:
                    self.assertFalse(out["出力.ack許可"],
                                     f"Already ACKed: {case[0]}")
                    self.assertFalse(out["出力.予約維持"])
                if (case[1] in ("保存後照合", "クラッシュ復旧")
                        and inputs["入力.予約あり"] and inputs["入力.未ack"]
                        and inputs["入力.予約全額受領済"]
                        and not inputs["入力.保存済み現物受領対"]):
                    self.assertFalse(out["出力.ack許可"])
                    self.assertTrue(out["出力.異常遮断"],
                                    f"Contradictory full receipt: {case[0]}")
                if not inputs["入力.予約あり"] and not inputs["入力.矛盾あり"]:
                    self.assertFalse(out["出力.ack許可"])
                    self.assertFalse(out["出力.予約維持"])
                    self.assertFalse(out["出力.異常遮断"])


if __name__ == "__main__":
    unittest.main(verbosity=2)

#!/usr/bin/env python3
"""Check AG-09 evidence arithmetic; never infer Paper acceptance from this tool.

Input CSV columns: case_id,stop_point,promised,delivered_before,
delivered_after,reserved_after,receipt_before,receipt_after,
disk_inventory_verified,ledger_verified,stop_point_verified.
Amounts are integer minor units. The three *_verified fields are
literal true/false; false produces inconclusive, never pass.
"""
import argparse
import csv
import json
import sys

REQUIRED = (
    "case_id", "stop_point", "promised", "delivered_before",
    "delivered_after", "reserved_after", "receipt_before",
    "receipt_after", "disk_inventory_verified", "ledger_verified",
    "stop_point_verified",
)
NUMERIC = ("promised", "delivered_before", "delivered_after",
           "reserved_after", "receipt_before", "receipt_after")


def evaluate(row):
    missing = [key for key in REQUIRED if not str(row.get(key, "")).strip()]
    if missing:
        return "inconclusive", "missing: " + ", ".join(missing)
    try:
        values = {key: int(row[key]) for key in NUMERIC}
    except ValueError:
        return "inconclusive", "noninteger evidence"
    if any(value < 0 for value in values.values()):
        return "fail", "negative amount or receipt"
    if values["delivered_after"] < values["delivered_before"]:
        return "fail", "delivered amount decreased"
    if values["receipt_after"] < values["receipt_before"]:
        return "fail", "receipt rolled back"
    if values["delivered_after"] + values["reserved_after"] != values["promised"]:
        return "fail", "saved delivered + reserved != promised"
    verified = [row[key].strip().lower() for key in REQUIRED[-3:]]
    if any(flag not in ("true", "false") for flag in verified):
        return "inconclusive", "invalid verification flag"
    if not all(flag == "true" for flag in verified):
        return "inconclusive", "disk/ledger/stop-point evidence incomplete"
    return "consistent", "arithmetic only; Paper exactly-once not proven"


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_file")
    args = parser.parse_args(argv)
    with open(args.csv_file, newline="", encoding="utf-8-sig") as stream:
        reader = csv.DictReader(stream)
        if not reader.fieldnames or any(k not in reader.fieldnames for k in REQUIRED):
            parser.error("missing required CSV columns")
        results = [
            {"case_id": row.get("case_id"), "result": status, "reason": reason}
            for row in reader
            for status, reason in [evaluate(row)]
        ]
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 1 if not results or any(r["result"] != "consistent" for r in results) else 0


if __name__ == "__main__":
    sys.exit(main())

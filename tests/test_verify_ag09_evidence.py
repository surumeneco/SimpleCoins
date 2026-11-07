import unittest
from tools.verify_ag09_evidence import evaluate


class AG09EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.row = dict(
            case_id="AG-09-A", stop_point="after-playerdata-save",
            promised="5200", delivered_before="0", delivered_after="5000",
            reserved_after="200", receipt_before="0", receipt_after="1",
            disk_inventory_verified="true", ledger_verified="true",
            stop_point_verified="true",
        )

    def test_consistent_is_not_accepted_as_paper_pass(self):
        self.assertEqual(evaluate(self.row)[0], "consistent")
        self.assertIn("not proven", evaluate(self.row)[1])

    def test_duplicate_delivery_fails(self):
        self.row["delivered_after"] = "5200"
        self.assertEqual(evaluate(self.row)[0], "fail")

    def test_rollback_fails(self):
        self.row["receipt_before"] = "2"
        self.assertEqual(evaluate(self.row)[0], "fail")

    def test_missing_disk_evidence_inconclusive(self):
        self.row["disk_inventory_verified"] = "false"
        self.assertEqual(evaluate(self.row)[0], "inconclusive")

    def test_missing_stop_point_inconclusive(self):
        self.row["stop_point"] = ""
        self.assertEqual(evaluate(self.row)[0], "inconclusive")

    def test_negative_amount_fails(self):
        self.row["reserved_after"] = "-1"
        self.assertEqual(evaluate(self.row)[0], "fail")

    def test_invalid_flag_inconclusive(self):
        self.row["ledger_verified"] = "yes"
        self.assertEqual(evaluate(self.row)[0], "inconclusive")


    def test_missing_numeric_csv_cell_does_not_raise_typeerror(self):
        # csv.DictReader uses None when the row ends before this field.
        self.row["promised"] = None
        self.assertEqual("inconclusive", evaluate(self.row)[0])

    def test_missing_verification_flag_does_not_raise_attributeerror(self):
        self.row["ledger_verified"] = None
        self.assertEqual("inconclusive", evaluate(self.row)[0])

    def test_null_stop_point_never_reports_consistent(self):
        self.row["stop_point"] = None
        self.assertEqual("inconclusive", evaluate(self.row)[0])



if __name__ == "__main__":
    unittest.main()

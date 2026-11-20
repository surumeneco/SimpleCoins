# WCL provider record boundaries — CAT Draft

Status: Candidate / unconfirmed. Flow: code-to-spec characterization. Scope: read-only `tools/audit_wcl_service_capture.py` provider records.

## Target
The WCL capture audit must distinguish malformed provider records from absent or unexpected providers without treating incomplete evidence as zero assets or authenticated runtime state.

## Candidate rules
- A non-object registration is invalid (`INVALID_REGISTRATION`) while the missing required provider remains incomplete (`REQUIRED_PROVIDER_MISSING`).
- A required provider with blank provider_class is invalid (`INVALID_PROVIDER_CLASS`).
- A required provider with non-boolean owner_enabled is invalid (`INVALID_PROVIDER_ENABLED`).
- An unexpected provider identifier is incomplete (`UNEXPECTED_PROVIDER`); its presence does not satisfy a required provider.
- Duplicate required provider IDs are invalid (`DUPLICATE_PROVIDER`) even if the duplicate has a different owner.
- Mixed invalid/incomplete evidence retains both codes, and overall status is invalid.

## Vectors
| ID | Mutation from valid() | Expected status | Required codes |
| --- | --- | --- | --- |
| PBR-01 | replace wwc registration with null | invalid | INVALID_REGISTRATION, REQUIRED_PROVIDER_MISSING |
| PBR-02 | blank wwc provider_class | invalid | INVALID_PROVIDER_CLASS |
| PBR-03 | wwc owner_enabled = 1 | invalid | INVALID_PROVIDER_ENABLED |
| PBR-04 | replace wwc provider_id with unknown id | incomplete | UNEXPECTED_PROVIDER, REQUIRED_PROVIDER_MISSING |
| PBR-05 | append wwc duplicate with owner_plugin = Unknown | invalid | DUPLICATE_PROVIDER, PROVIDER_OWNER_MISMATCH |

These are characterization candidates of existing behavior, not Confirmed requirements. No production change is authorized by this draft. Formal CAT-TDD closure/model/vector/queue/current-item gates remain not-run.

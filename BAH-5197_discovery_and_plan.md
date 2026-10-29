# Discovery & Plan: BAH-5197

**JIRA**: BAH-5197
**Summary**: [Backend Logic]: Resolve Disposition ServiceRequest to a valid Order type
**Story Points**: 2
**Created**: 2026-10-07

---

## JIRA Details

### Description
Follow-up from BAH-5066 (Disposition button & form in Consultation Pad). The frontend posts a FHIR `ServiceRequest` when a disposition (Admit/Discharge/Transfer/Undo Discharge) is saved. Posting fails today with `Unable to determine order type for ServiceRequest`, thrown from `ServiceRequestValidatorImpl.java:33` because `OrderService.getOrderTypeByConcept(concept)` returns `null` for the disposition concepts — they have conceptClass `"Misc"`, and no `OrderType` in `order_type_class_map` is associated with that class. This is missing configuration, not a regression.

### Acceptance Criteria
1. Posting a disposition `ServiceRequest` (Admit/Discharge/Transfer/Undo Discharge) via the FHIR encounter-bundle endpoint resolves to a valid Order and succeeds.
2. The resulting Order/ServiceRequest round-trips correctly on read (status reflects fulfillment per the design discussion on BAH-5066).
3. Decision recorded on which approach (new order_type_class_map row vs. conceptClass change) was taken, and why.

**AC Status**: Clear.

### Attachments
No attachments.

### Comments & Context
No comments on BAH-5197 itself. Full design context lives on BAH-5066:
- Disposition concepts are seeded by `bahmni-core`'s `V1_62__AddDispositionConcepts.sql` (conceptClass `"misc"` for Admit/Discharge/Transfer Patient; `Undo Discharge` already exists separately, concept_id 127, also class `Misc`).
- Per BAH-5066's latest comment, the frontend saves disposition as a `ServiceRequest` left **open**; bed management later fulfills it via the existing `admitConfirmation` encounter flow and stamps `fulfillerStatus = COMPLETED`. `ServiceRequestStatusTranslatorImpl` already maps `Order.FulfillerStatus → ServiceRequest.status` generically (order-type-agnostic) — so AC2 needs no new code, only needs AC1's fix to land so an Order can be created at all.

### Linked Issues
| Type | Issue | Status | Impact |
|------|-------|--------|--------|
| Blocks | BAH-5066 | In Development | BAH-5066's save logic (`createBundleEntries`) is a placeholder pending this card |

---

## Codebase Assessment

### Affected Files
| File | Change Type |
|------|-------------|
| `bahmni-core/bahmnicore-omod/src/main/resources/liquibase.xml` (separate repo) | Modify — add changeset(s) |
| This repo (`bahmni-module-fhir2-addl-extension`) | **None required** — see finding below |

### Existing Patterns
- `ServiceRequestValidatorImpl.validate()` and `BahmniServiceRequestTranslatorImpl.toOpenmrsType()` (line 189: `order.setOrderType(orderService.getOrderTypeByConcept(conceptBeingOrdered))`) both already call the same generic, order-type-agnostic resolution path. Once a concept resolves to an `OrderType`, both AC1 and AC2 work with **zero code changes** in this repo.
- Existing `order_type_class_map` precedent in `bahmni-core`'s `liquibase.xml` (e.g. Radiology Order ↔ `Radiology/Imaging Procedure`, line ~4545) uses an idempotent `sqlCheck` precondition + raw `<sql>` insert — the pattern to follow.
- All existing Bahmni `OrderType` rows (`Lab Order`, `Radiology Order`, `Procedure Order`) use `java_class_name = org.openmrs.Order` (plain `Order`, no custom subclass) — confirmed via a live DB query against the running `bahmni-standard` stack. A new disposition `OrderType` can follow the same convention.

### Live DB findings (verified against the running `bahmni-standard-openmrsdb-1` container)
- `order_type` table has 5 rows today: Drug Order, Test Order, Lab Order, Radiology Order, Procedure Order. **None map to conceptClass `Misc`.**
- **`Misc` conceptClass has 2,401 concepts** — it's a generic dumping-ground class (booleans, frequency codes like `qD`/`BID`, specimen-source answers, etc.), not something scoped to disposition. Mapping `Misc` itself to any `OrderType` (as the ticket's Option 1 literally suggests) would make **all 2,401** of those unrelated concepts newly resolvable/orderable via FHIR — a real, unintended side effect, not just a theoretical one.
- The disposition answer concepts (`Admit Patient`, `Discharge Patient`, `Transfer Patient`, `Undo Discharge`) are exactly 4 concepts, all class `Misc`, confirmed via query.
- `Procedure` conceptClass (the closest existing OrderType-mapped class, semantically) already holds real clinical procedure concepts (vaccinations, etc.) — reusing it for a conceptClass-reassignment approach would mix 4 ADT/administrative concepts into a class otherwise used for actual clinical procedures.
- All 4 disposition concepts carry stable, unambiguous `concept_reference_map` entries for source `org.openmrs.module.emrapi` — codes `ADMIT` (29), `DISCHARGE` (30), `TRANSFER` (31), `UNDO_DISCHARGE` (127) — confirmed via query. These are the same codes BAH-5066's analysis says the legacy `dispositionController` already keys off of.
- No precedent anywhere in this repo or `bahmni-core` for lazily creating metadata (OrderType/ConceptClass/ProviderAttributeType) at request time — every example found (Lab/Radiology/Procedure OrderTypes, BAH-5116's "License Number" attribute type) is pre-seeded via Liquibase/Admin UI, never created implicitly by request-handling code. The one `getOrCreate` precedent found (`EpisodeEncounterCreateCommand`) is for per-patient transactional data (Episode), not shared reference metadata — not analogous.

---

## Implementation Approach

### Approach C: Code-level fallback resolution by emrapi reference code, fail-fast (Recommended, Final)
**Technical Approach**: In `ServiceRequestValidatorImpl` and `BahmniServiceRequestTranslatorImpl` (this repo), when `orderService.getOrderTypeByConcept(concept)` returns `null`:
1. Check the concept's reference mappings for source `org.openmrs.module.emrapi` with code in `{ADMIT, DISCHARGE, TRANSFER, UNDO_DISCHARGE}`.
2. If matched, resolve via `orderService.getOrderTypeByName("ADT Order")`.
3. If that `OrderType` doesn't exist, fail fast with a clear `InvalidRequestException` (same philosophy as the existing code's current behavior for any unmapped concept) — no runtime creation. The `"ADT Order"` row itself must be pre-seeded.
4. If no emrapi code matches, keep the existing behavior (fail with "Unable to determine order type for ServiceRequest") — unaffected for all other concepts.

**Files to Modify**:
- `api/src/main/java/org/bahmni/module/fhir2addlextension/api/validators/impl/ServiceRequestValidatorImpl.java`
- `api/src/main/java/org/bahmni/module/fhir2addlextension/api/translator/impl/BahmniServiceRequestTranslatorImpl.java`
- New shared resolver (e.g. `DispositionOrderTypeResolver` or a small helper) used by both, to avoid duplicating the emrapi-code lookup.
- `bahmni-core/bahmnicore-omod/src/main/resources/liquibase.xml` (separate repo) — single new `order_type` row: `"ADT Order"`, `java_class_name = org.openmrs.Order`. No `concept_class`/`order_type_class_map`/concept reassignment needed at all with this approach.

**Pros**: No DB migration touching concepts/classes at all — avoids both the `Misc` blast radius (2,401 concepts) and the `Procedure`-class pollution risk entirely. Matches existing fail-fast convention (`ServiceRequestValidatorImpl` already fails this way today). Identification via emrapi reference code is precise (only these 4 concepts match) rather than relying on a shared conceptClass. Keeps the behavioral fix self-contained in this repo; `bahmni-core` only needs one trivial new metadata row, with zero conceptual coupling to anything else.

**Cons**: Requires the `"ADT Order"` OrderType to be pre-seeded in `bahmni-core` (or created once via Admin UI) before disposition works on a given environment — same one-time-setup trade-off as BAH-5116's attribute type. Two call sites (validator + translator) need the same fallback logic, so it must be factored into one shared resolver to avoid duplication/drift.

**Complexity/Risk**: Low.

### Rejected: DB-only approaches (new ConceptClass, or reassigning to `Procedure`)
Both were considered and rejected in favor of Approach C, per discussion with the engineer: the fix should be handled in backend code in this repo (since it's this module that receives and translates the ServiceRequest), not via a `bahmni-core` migration that reshapes concept metadata. Approach C achieves the same outcome with a much smaller, safer `bahmni-core` footprint (one new OrderType row vs. new ConceptClass + concept reassignment + class-map row).

### Rejected: lazy get-or-create of the OrderType
Considered, but rejected — would be a new pattern for this codebase. No existing precedent creates shared reference metadata (OrderTypes, ConceptClasses, attribute types) implicitly from request-handling code; it's always pre-seeded. Fail-fast keeps the new code consistent with how `ServiceRequestValidatorImpl` already handles missing order types today.

---

## Work Breakdown

| Sub-task | Effort | Files | Tests |
|----------|--------|-------|-------|
| Confirm approach against live DB (done above) | ~0.25 pt | — | — |
| Add shared resolver: emrapi reference-code lookup → `OrderType` by name, fail-fast if missing | ~0.5 pt | New resolver class | Unit tests: matched code resolves, unmatched code/no mapping falls through to existing error, missing OrderType fails clearly |
| Wire resolver into `ServiceRequestValidatorImpl` and `BahmniServiceRequestTranslatorImpl` | ~0.5 pt | Both files | Unit tests updated/added for both |
| `bahmni-core` Liquibase changeset: seed `"ADT Order"` OrderType row (idempotent via `sqlCheck`) | ~0.25 pt | `bahmni-core/bahmnicore-omod/src/main/resources/liquibase.xml` | Manual: run on fresh + existing DB, confirm idempotent re-run |
| Verify live: POST a disposition `ServiceRequest` via `/ws/fhir2/R4/Encounter` bundle, confirm Order is created and read-back status is correct | ~0.25 pt | — | Manual live verification (as done for BAH-4880) |
| Record decision (AC3) as a JIRA comment on BAH-5197, linking the `bahmni-core` tracking card/PR | ~0.25 pt | — | — |

**Total estimated**: ~2 points.

---

## Scope Verdict

- [x] ✅ Confirmed 2-pointer - moderate complexity, focused scope

The investigation (DB queries, blast-radius analysis, approach comparison, confirming code-level vs. migration-level placement) is the bulk of the effort; the actual code change is small and localized to two existing classes plus one new resolver, with a one-row `bahmni-core` migration tracked separately.

---

## Clarifying Questions

No open clarifying questions — approach, naming, and scope confirmed with the engineer.

**Resolved decisions**:
1. Fix lives in this repo's code (`ServiceRequestValidatorImpl` + `BahmniServiceRequestTranslatorImpl`), not as a `bahmni-core` data migration reshaping concepts/classes.
2. OrderType name: `"ADT Order"`.
3. Missing-OrderType behavior: fail-fast (consistent with existing convention), not lazy get-or-create.
4. `bahmni-core` still needs a one-row Liquibase changeset to seed `"ADT Order"` — tracked via a separate card, same pattern as BAH-5116.

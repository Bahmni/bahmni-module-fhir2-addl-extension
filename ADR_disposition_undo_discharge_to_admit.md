

# ADR: Disposition Dropdown State & Undo Discharge → "To Admit" Behavior

**Status**: Proposed
**Related**: BAH-5066, BAH-5197
**Date**: 2026-10-25

## Context

The consultation pad's Disposition control offers Admit / Discharge / Transfer /
Undo Discharge, filtered by the current visit's state. Separately, bed
management has a "To Admit" queue that ward staff work from to physically
assign a bed. This ADR records how those two surfaces are wired together
across visits, encounters, and the DB — and the one open decision needed for
Undo Discharge to behave correctly.

## How visit state actually drives the dropdown (confirmed, already correct)

`dispositionService.ts` filters the dropdown from a single visit attribute,
not from any observation or order:

```ts
fetchAdmissionStatus(visitUuid)
  → GET /openmrs/ws/rest/v1/visit/{uuid}?v=custom:(attributes:(attributeType:(name),value,voided))
  → reads the non-voided "Admission Status" attribute (Admitted | Discharged | absent)

filterDispositionTypesByAdmissionStatus(types, status):
  ADMITTED   → [Discharge, Transfer]
  DISCHARGED → [Undo Discharge]
  null/fresh → [Admit]
```

This is a direct re-implementation of the legacy `dispositionController.js`
filtering rule and is already correct — verified end-to-end this session by
watching `Admission Status` flip `NULL → Admitted → Discharged` in the DB as
we drove admit/discharge through the bed-management UI.

## The encounter/visit-attribute mechanics underneath (confirmed via live testing)

| Action | Encounter created/voided | `Admission Status` | `Visit Status` |
|---|---|---|---|
| Admit | new `ADMISSION` encounter | → `Admitted` | → `IPD` |
| Discharge | new `DISCHARGE` encounter | → `Discharged` | unchanged |
| Undo Discharge (legacy, dead code) | **voids** the `DISCHARGE` encounter | → rolls back to `Admitted` | unchanged |

This table is driven entirely by `BahmniVisitAttributeService`, triggered off
encounter *type* on save/void — not by anything disposition- or
ServiceRequest-specific. One visit, many encounters; the attributes are a
rolling summary of the latest ADT-relevant encounter.

The legacy `$scope.undoDischarge()` (`ui/app/adt/controllers/adtController.js:316`)
implements the bottom row directly: `DELETE bahmniencounter/{dischargeEncounterUuid}`
→ encounter voided → `Admitted` restored **immediately, in place** — no bed
reassignment, no "To Admit" step. We confirmed this works correctly at the API
level, but it has **no UI entry point anywhere in the codebase** (dead code).

## The decision this ADR is actually about

Your requirement — selecting "Undo Discharge" from the clinical pad should
land the patient back on bed management's **"To Admit"** tab, not silently
flip them back to Admitted in place — is a different behavior than the dead
legacy function above. It means: **Undo Discharge is an admission request,
not an instant data correction.** The patient must go through bed assignment
again (possibly a different bed), the same as a brand-new Admit.

This matters because "To Admit" (`emrapi.sqlSearch.patientsToAdmit`) is driven
by a **hardcoded** SQL match on an Observation:

```sql
... join concept_name cn on c.concept_id = cn.concept_id ...
where cn.name = 'Admit Patient'
  and v.visit_id not in (select visit_id from encounter ... where encounter_type = 'ADMISSION')
```

It matches the literal concept name `'Admit Patient'` and excludes visits that
already have *any* ADMISSION encounter — both of which make it structurally
blind to "Undo Discharge" as a distinct disposition, even though the dual-write
Observation for Undo Discharge would carry a different `value_coded`
(`Undo Discharge`, not `Admit Patient`), and the visit already has a prior
ADMISSION encounter from the first admit.

## Options for making Undo Discharge resolve to "To Admit"

1. **Dual-write Undo Discharge's Observation value as the `Admit Patient` concept**
   (not the `Undo Discharge` concept). Zero backend/SQL changes; reuses the
   existing GP as-is. Downside: semantically lossy — any reader of the raw
   Observation sees "Admit Patient" for what was actually an undo-discharge
   action; loses the distinction if something else ever needs to tell them apart.
2. **Extend the `emrapi.sqlSearch.patientsToAdmit` GP SQL** to also match
   `cn.name = 'Undo Discharge'`, and drop its `not in (ADMISSION encounters)`
   exclusion for that case (since an undo-discharge visit *will* already have
   an old ADMISSION encounter). Correct semantics, but it's a shared,
   per-deployment GP — changing it is a real migration with broader blast
   radius, and the legacy SQL wasn't written to be safely extended (recall the
   unrelated `NOT IN` + `NULL` bug we hit and fixed by hand this session).
3. **Don't route through the legacy SQL at all** — build the new
   ServiceRequest-driven "pending admissions queue" (already scoped as
   required, separate, net-new UI work per the BAH-5066/BAH-5197 design) to
   filter on `category=ADT Order` and `code in (ADMIT, UNDO_DISCHARGE)`. This
   is the architecturally "right" answer per the existing dual-write design,
   but it depends on work that doesn't exist yet (confirmed empty in
   `openmrs-module-ipd-frontend` this session).

**Recommendation**: (3) is correct long-term, but needs the queue UI built.
(1) is the pragmatic bridge if you need Undo Discharge → To Admit working
against the *current* legacy bed-management screen today — it's a one-line
change in the frontend's disposition bundle-entry builder (same file,
`createDispositionBundleEntries`), not a backend/SQL change.

## Open question for you

Do you want (1) as an interim fix now, or hold Undo Discharge's queue
behavior until the real ServiceRequest-based "To Admit" queue (option 3) is
built? This determines whether there's a frontend code change to make today.

# Tech Analysis: Multiple Contact Points in Patient.telecom (BAH-4740)

Audience: a developer with no prior context on this ticket who needs to
understand the shipped design, how to configure it, and why it looks the
way it does. Verified working end-to-end against a local docker instance.

## 1. The problem (BAH-4740)

OpenMRS person attributes (`phoneNumber`, `email`, and optionally
`alternatePhoneNumber`) must appear as standard FHIR `Patient.telecom`
`ContactPoint` entries — not just as the custom extensions this module
used to emit — and **more than one must be visible at once** (e.g. phone
*and* email together), for both read and write.

## 2. AC traceability

| # | AC (from ticket) | Satisfied by |
|---|---|---|
| 1 | Phone in `telecom` on read | `BahmniPatientTranslatorImpl.addAdditionalContactPoints` (§5) |
| 2 | Email in `telecom` on read | same |
| 3 | Multiple contacts in `telecom` | same — loops over every configured attribute type, not just one |
| 4 | No `telecom` when no contacts | naturally satisfied — nothing added when no attributes match the configured mapping |
| 5 | Telecom write creates phone attribute | `BahmniPatientTranslatorImpl.processContactPoints` (§5) |
| 6 | Telecom write creates email attribute | same |
| 7 | Configurable attribute-type mapping | `fhir2Extension.telecomAttributeTypeMap` global property (§4) — admin-configurable, no SQL required |
| 8 | Backward compatibility, no data migration | telecom is derived live from existing person attributes on every read — nothing to migrate |

## 3. Design decision: implement here, not in `openmrs-module-fhir2`

- The base `fhir2` module (version `2.5.0`, pinned via
  `openmrs.fhir2.version` in this repo's `pom.xml`, `provided` scope)
  already has partial telecom support: `PatientTranslatorImpl` builds
  `Patient.telecom` from person attributes, but only for a **single**
  attribute type, selected via its own global property
  `fhir2.personContactPointAttributeTypeUuid`.
- Its write path has a real bug: it writes *every* incoming `ContactPoint`
  to that one configured attribute type regardless of `system` — sending
  both a phone and an email `ContactPoint` would corrupt data (e.g. the
  email value landing in the `phoneNumber` attribute).
- We do not patch `openmrs-module-fhir2` locally. This repo compiles
  against the real upstream `fhir2-api:2.5.0` jar (`provided`
  dependency); a locally patched/rebuilt base module that doesn't exactly
  match that jar's API surface fails Spring context initialization on
  deploy and takes down the entire FHIR REST layer. All work lives in
  `bahmni-module-fhir2-addl-extension`, which stays compatible with
  whatever `fhir2-api` version is actually deployed.

## 4. Design evolution — two approaches tried and abandoned first

Worth recording so nobody re-attempts these:

1. **Seed the base module's `fhir_contact_point_map` table from a GP, at
   module startup.** The base module has a generic mapping table
   (`fhir_contact_point_map`, backed by `FhirContactPointMapService`)
   that supports multiple attribute types, so the first design synced a
   new global property into that table via a `@PostConstruct`-triggered
   seeder. This hit three real bugs in sequence, all confirmed live
   against a running instance:
   - Calling `Context.getAdministrationService()` (even indirectly) from
     a bean's `@PostConstruct` **deadlocks module startup** — the
     startup thread ends up waiting on `ServiceContext` to finish
     registering services, on the very thread that would otherwise
     finish registering them. Fixed by moving the trigger to
     `ModuleActivator.started()`, which only runs after the context
     fully refreshes.
   - The base module's own `FhirContactPointMapService.saveFhirContactPointMap`
     generates an **unquoted SQL `INSERT`** for the `rank`, `system`,
     `use` columns (MySQL reserved words) — a genuine bug in the
     vendor's Hibernate mapping, throwing `SQLSyntaxErrorException` on
     every create. Worked around with a native SQL DAO that
     backtick-quotes those columns explicitly.
   - That native query then failed with
     `TransactionRequiredException` because the DAO bean wasn't
     `@Transactional`.
2. Getting all three fixed made it work, but it was a large amount of
   machinery (a seeder class, an Activator hook, a native-SQL DAO, a
   `@Transactional` boundary) to sync one string into a database table —
   and it didn't match how this repo already handles GP-driven config
   anywhere else. **Abandoned in favor of the design below.**

## 5. Final design: direct, cached GP read — no database writes at all

This matches the existing convention in this repo
(`OpenmrsAppContext.getOrderTypeToLocationAttributeNameMap()` for
`fhir2Extension.orderTypeToReferralLocationAttributeMap`): read the
global property lazily, parse it, cache the result, and have business
logic consult it directly. No syncing into any other table, no
startup-time work, no transactions.

**`fhir2Extension.telecomAttributeTypeMap`** (new global property, declared
in `omod/src/main/resources/config.xml`) — format:

```
attributeTypeUuid:SYSTEM[:USE][:RANK] ; attributeTypeUuid:SYSTEM[:USE][:RANK] ; ...
```

`SYSTEM`/`USE` are FHIR `ContactPoint` enum constants, matched
case-insensitively (`phone`, `Phone`, `PHONE` all resolve the same way).
`USE` and `RANK` are optional — `uuid:EMAIL` sets neither; `uuid:PHONE::2`
sets rank without a use. Example:

```
14d4f66a-26fe-102b-80cb-0017a47871b2:PHONE::1;183addd6-26fe-102b-80cb-0017a47871b2:EMAIL;2c37bdc7-26fe-102b-80cb-0017a47871b2:PHONE:HOME:2
```

**`AppContext.getTelecomAttributeTypeMappings()`** (new interface method,
implemented in `OpenmrsAppContext`, `api/context/impl/OpenmrsAppContext.java`)
— `@Cacheable`, reads the GP via `AdministrationService.getGlobalProperty`,
parses it into `List<TelecomAttributeTypeMapping>` (new small model class,
`api/model/TelecomAttributeTypeMapping.java`: `attributeTypeUuid`, `system`,
`use`, `rank`). Malformed entries, unknown enum values, and non-numeric
ranks are logged and skipped individually — one bad entry doesn't prevent
the rest from being read.

**`BahmniPatientTranslatorImpl`** (`@Primary`, extends the base module's
`PatientTranslatorImpl`) consults that cached list directly:

- **Read** (`toFhirResource`): `super.toFhirResource(...)` runs first and
  may add *one* `ContactPoint` from the base module's own
  single-attribute-type logic (still backed by `fhir_contact_point_map` —
  see §6). `addAdditionalContactPoints` then loops over every other
  active person attribute, looks its type up in
  `appContext.getTelecomAttributeTypeMappings()` (by attribute type
  UUID), and adds a `ContactPoint` for every match not already present —
  so phone *and* email *and* any other configured type can all appear at
  once. `addPersonAttributeExtensions` then skips publishing as an
  extension anything whose attribute UUID is already present in the
  final `telecom` list, so nothing is duplicated regardless of which
  mechanism (base module's or ours) put it there.
- **Write** (`toOpenmrsType`): telecom is cleared before delegating to
  `super.toOpenmrsType(...)` (neutralizing the base module's
  single-attribute-type write bug — see §3), then restored and passed to
  `processContactPoints`, which calls
  `resolvePersonAttributeTypeForContactPoint` for each incoming
  `ContactPoint`: searches the same cached GP list for entries matching
  the contact point's `system`, picks the lowest `rank` on a tie, and
  resolves that to a `PersonAttributeType` via
  `personService.getPersonAttributeTypeByUuid`. Falls back to the base
  module's single GP-configured attribute type
  (`fhir2.personContactPointAttributeTypeUuid`) when the contact point
  has no system, or no configured mapping matches.

No changes anywhere in `openmrs-module-fhir2`, and — unlike the abandoned
design in §4 — **no writes to any database table** for this feature at
all.

## 6. What `fhir_contact_point_map` is used for now

**Only by the base module's own internal logic, for its single
GP-configured attribute type — our code never reads or writes it.**

- The base module's `PatientTranslatorImpl` still consults
  `fhir_contact_point_map` internally to build the *one* `ContactPoint`
  it adds from `fhir2.personContactPointAttributeTypeUuid`. That's
  unchanged, pre-existing base-module behaviour, opaque to us.
- Any additional contact points (email, alternate numbers, etc.) come
  entirely from `fhir2Extension.telecomAttributeTypeMap`, parsed
  in-memory. This module has zero code touching
  `fhir_contact_point_map` — no DAO, no service call, nothing.
- Practical implication: if you want the base module's single-attribute
  entry to have specific `use`/`rank` values, that's still configured via
  `fhir_contact_point_map` (out of our control) plus
  `fhir2.personContactPointAttributeTypeUuid`. Everything else is
  configured entirely through our own GP.

## 7. Known limitation — write ambiguity when two attribute types share a `system`

If two different attribute types are both mapped to `system = PHONE`
(e.g. `phoneNumber` and `alternatePhoneNumber`), `resolvePersonAttributeTypeForContactPoint`
resolves ties by `rank` only (lowest wins) — it does **not** consider
`ContactPoint.use`. A write with `{"system": "phone", ...}` always lands
on whichever attribute type has the lowest configured rank; there's
currently no way for a client to specifically target the higher-rank one
via write. Reads are unaffected — both still appear as separate `telecom`
entries. This is a deliberate scope cut, not a bug — flag it in review if
write access to secondary contact numbers is needed later (would require
also matching on `use`).

## 8. Verification performed

- Full `api` module test suite: 870/870 passing (0 failures, 0 errors, 3
  pre-existing skips), including:
  - `OpenmrsAppContextTest` — parsing of `fhir2Extension.telecomAttributeTypeMap`
    (multi-entry, case-insensitivity, malformed/unknown values, blank
    input).
  - `BahmniPatientTranslatorImplTest` — `addAdditionalContactPoints`,
    `addPersonAttributeExtensions` (dedup against final `telecom`),
    `resolvePersonAttributeTypeForContactPoint`, `processContactPoints`.
- Manual end-to-end verification against a local docker instance, with
  `fhir2Extension.telecomAttributeTypeMap` configured for `phoneNumber`,
  `email`, and `alternatePhoneNumber`:
  - `GET /openmrs/ws/fhir2/R4/Patient/{uuid}` — confirmed `telecom`
    containing phone, email, and alternate-phone entries with correct
    `system`/`use`/`rank`, and confirmed the corresponding
    `.../ext/patient/...` extensions no longer appear.
  - Confirmed via live thread dump and log inspection that the earlier
    seeder-based design's three bugs (§4) are moot under this design —
    there is no startup-time code path and no database write to fail.
  - Reported by the developer testing this as **working fine** in the
    running environment.

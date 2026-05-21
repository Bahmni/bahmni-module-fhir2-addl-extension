-- BAH-4718: Migrate legacy obs-based patient documents to FHIR DocumentReference
-- This migration converts obs records (parent + child structure) from bahmni-core
-- into document_reference and document_reference_content records in fhir2-addl-extension

-- Step 1: Create document_reference rows (1 per parent obs)
-- Each parent obs (obs_group_id IS NULL) with children becomes 1 document_reference row
INSERT INTO document_reference (
    uuid, master_identifier, status, type_concept_id,
    date_started, author_id, description,
    encounter_id, subject_id, creator, date_created, voided
)
SELECT
    UUID(),
    CONCAT('migration-obs-', o.obs_id),
    'CURRENT',
    o.concept_id,
    MAX(om.obs_datetime),
    MAX(ep.provider_id),
    SUBSTRING(COALESCE(MAX(CASE WHEN om.comments IS NOT NULL THEN om.comments END), ''), 1, 255),
    o.encounter_id,
    o.person_id,
    o.creator,
    NOW(),
    0
FROM obs o
    INNER JOIN obs om ON om.obs_group_id = o.obs_id
    INNER JOIN encounter e ON o.encounter_id = e.encounter_id
    INNER JOIN encounter_type et ON e.encounter_type = et.encounter_type_id
    LEFT JOIN encounter_provider ep ON e.encounter_id = ep.encounter_id AND ep.voided = 0
WHERE et.name = 'Patient Document'
  AND o.obs_group_id IS NULL
  AND o.voided = 0
  AND om.voided = 0
  AND om.value_text IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM document_reference dr
    WHERE dr.master_identifier = CONCAT('migration-obs-', o.obs_id)
  )
GROUP BY o.obs_id, o.person_id, o.encounter_id, o.concept_id, o.creator
ORDER BY o.obs_id;

-- Step 2: Create document_reference_content rows (1 per child obs)
-- Each child obs becomes 1 content row, linked to its parent document_reference
-- MIME types are derived from file extension (last segment after '.')
INSERT INTO document_reference_content (
    document_reference_id, content_url, content_type,
    uuid, creator, date_created, voided
)
SELECT
    dr.document_reference_id,
    SUBSTRING(om.value_text, 1, 512),
    CASE
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'pdf' THEN 'application/pdf'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) IN ('jpg', 'jpeg') THEN 'image/jpeg'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'png' THEN 'image/png'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'gif' THEN 'image/gif'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'bmp' THEN 'image/bmp'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'webp' THEN 'image/webp'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'avif' THEN 'image/avif'
        WHEN LOWER(SUBSTRING_INDEX(om.value_text, '.', -1)) = 'mp4' THEN 'video/mp4'
        ELSE 'application/octet-stream'
    END,
    UUID(),
    om.creator,
    NOW(),
    0
FROM obs o
    INNER JOIN obs om ON om.obs_group_id = o.obs_id
    INNER JOIN encounter e ON o.encounter_id = e.encounter_id
    INNER JOIN encounter_type et ON e.encounter_type = et.encounter_type_id
    INNER JOIN document_reference dr ON dr.master_identifier = CONCAT('migration-obs-', o.obs_id)
WHERE et.name = 'Patient Document'
  AND o.obs_group_id IS NULL
  AND o.voided = 0
  AND om.voided = 0
  AND om.value_text IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM document_reference_content drc
    WHERE drc.document_reference_id = dr.document_reference_id
      AND drc.content_url = SUBSTRING(om.value_text, 1, 512)
  )
ORDER BY o.obs_id, om.obs_id;

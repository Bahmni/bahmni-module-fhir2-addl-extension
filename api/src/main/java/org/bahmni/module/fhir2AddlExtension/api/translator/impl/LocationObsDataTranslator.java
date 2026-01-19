package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.ComplexObsDataTranslator;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.util.FhirUtils;

import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.createLocationReferenceByUuid;

class LocationObsDataTranslator implements ComplexObsDataTranslator {
	
	@Override
	public boolean supports(Concept concept) {
		return "LocationObsHandler".equals(((ConceptComplex) concept).getHandler());
	}
	
	@Override
	public Extension toFhirResource(Obs obs) {
		return new Extension(FhirConstants.OPENMRS_FHIR_EXT_OBS_LOCATION_VALUE,
		        createLocationReferenceByUuid(obs.getValueComplex()));
	}
	
	@Override
	public String toOpenmrsType(Observation observation) {
		Extension locationExt = observation.getExtensionByUrl(FhirConstants.OPENMRS_FHIR_EXT_OBS_LOCATION_VALUE);
		if (locationExt == null) {
			return null;
		}
		Type extValue = locationExt.getValue();
		if (extValue instanceof Reference) {
			return FhirUtils.referenceToId(((Reference) extValue).getReference()).orElse(null);
		} else {
			return null;
		}
	}
}

package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.service.BahmniPatientMatchService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("bahmniPatientMatchProvider")
@R4Provider
public class BahmniPatientMatchProvider implements IResourceProvider {
	
	private final BahmniPatientMatchService matchService;
	
	private final PatientTranslator patientTranslator;
	
	@Autowired
	public BahmniPatientMatchProvider(BahmniPatientMatchService matchService, PatientTranslator patientTranslator) {
		this.matchService = matchService;
		this.patientTranslator = patientTranslator;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}
	
	@Operation(name = "$match", idempotent = false)
	public Bundle match(@OperationParam(name = "resource") Patient resource, RequestDetails requestDetails) {
		if (resource == null) {
			throw new InvalidRequestException("Parameter 'resource' with a Patient is required");
		}
		HumanName name = resource.getNameFirstRep();
		if (!resource.hasName() || (!name.hasGiven() && !name.hasFamily())) {
			throw new InvalidRequestException("At least one name is required");
		}
		
		List<org.openmrs.Patient> matches = matchService.findMatches(resource);
		
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.setTotal(matches.size());
		for (org.openmrs.Patient match : matches) {
			Bundle.BundleEntryComponent entry = bundle.addEntry();
			Patient fhirPatient = patientTranslator.toFhirResource(match);
			entry.setFullUrl(requestDetails.getFhirServerBase() + "/Patient/" + fhirPatient.getIdElement().getIdPart());
			entry.setResource(fhirPatient);
			entry.getSearch().setMode(Bundle.SearchEntryMode.MATCH);
		}
		return bundle;
	}
}

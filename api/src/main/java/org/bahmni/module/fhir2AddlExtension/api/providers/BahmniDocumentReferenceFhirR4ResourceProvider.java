package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniDocumentReferenceSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDocumentReferenceService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("bahmniDocumentReferenceFhirR4ResourceProvider")
@R4Provider
public class BahmniDocumentReferenceFhirR4ResourceProvider implements IResourceProvider {
	
	final private BahmniFhirDocumentReferenceService fhirDocumentReferenceService;
	
	@Autowired
	public BahmniDocumentReferenceFhirR4ResourceProvider(BahmniFhirDocumentReferenceService fhirDocumentReferenceService) {
		this.fhirDocumentReferenceService = fhirDocumentReferenceService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return DocumentReference.class;
	}
	
	@Read
	public DocumentReference getDocumentReferenceByUuid(@IdParam IdType id) {
		DocumentReference docRef = fhirDocumentReferenceService.get(id.getIdPart());
		if (docRef == null) {
			throw new ResourceNotFoundException("Could not find DocumentReference with Id " + id.getIdPart());
		}
		return docRef;
	}
	
	@Create
	public MethodOutcome createDocumentReference(@ResourceParam DocumentReference documentReference) {
		DocumentReference createdDocRef = fhirDocumentReferenceService.create(documentReference);
		return FhirProviderUtils.buildCreate(createdDocRef);
	}
	
	@Search
	public IBundleProvider searchDocumentReference(
	        @OptionalParam(name = DocumentReference.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_NAME, Patient.SP_GIVEN, Patient.SP_FAMILY }, targetTypes = Patient.class) ReferenceAndListParam subjectReference,
	        @OptionalParam(name = DocumentReference.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_NAME, Patient.SP_GIVEN, Patient.SP_FAMILY }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = DocumentReference.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
	        @OptionalParam(name = ServiceRequest.SP_ENCOUNTER, chainWhitelist = { "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @Sort SortSpec sort) {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference, id,
		        lastUpdated, encounterReference, sort);
		return fhirDocumentReferenceService.searchDocumentReferences(searchParams);
	}
	
	@Update
	public MethodOutcome updateDocumentReference(@IdParam IdType id, @ResourceParam DocumentReference updatedDocRef) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		updatedDocRef.setId(id);
		return FhirProviderUtils.buildUpdate(fhirDocumentReferenceService.update(id.getIdPart(), updatedDocRef));
	}
	
	@Delete
	@SuppressWarnings("unused")
	public OperationOutcome deleteDocumentReference(@IdParam @Nonnull IdType id) {
		fhirDocumentReferenceService.delete(id.getIdPart());
		return FhirProviderUtils.buildDeleteR4();
	}
}

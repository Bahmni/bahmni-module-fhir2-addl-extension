package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.collections.CollectionUtils;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniConditionSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirConditionService;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.ConditionFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@R4Provider
public class BahmniConditionFhirR4ResourceProvider extends ConditionFhirResourceProvider {
	
	@Autowired
	private BahmniFhirConditionService conditionService;
	
	@Search
	public IBundleProvider searchConditions(@RequiredParam(name = Condition.SP_CATEGORY) StringParam category,
	        @OptionalParam(name = Condition.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }) ReferenceAndListParam patientParam,
	        @OptionalParam(name = Condition.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }) ReferenceAndListParam subjectParam,
	        @OptionalParam(name = Condition.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = Condition.SP_CLINICAL_STATUS) TokenAndListParam clinicalStatus,
	        @OptionalParam(name = Condition.SP_ONSET_DATE) DateRangeParam onsetDate,
	        @OptionalParam(name = Condition.SP_ONSET_AGE) QuantityAndListParam onsetAge,
	        @OptionalParam(name = Condition.SP_RECORDED_DATE) DateRangeParam recordedDate,
	        @OptionalParam(name = Condition.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = Condition.SP_VERIFICATION_STATUS) TokenAndListParam verificationStatus,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
	        @IncludeParam(allow = { "Condition:" + Condition.SP_PATIENT }) HashSet<Include> includes) {
		if (patientParam == null && subjectParam == null) {
			throw new InvalidRequestException("Patient or subject filter must be passed");
		}
		if (patientParam == null) {
			patientParam = subjectParam;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		return conditionService.searchConditions(new BahmniConditionSearchParams(patientParam, code, clinicalStatus,
		        onsetDate, onsetAge, recordedDate, id, verificationStatus, category, lastUpdated, sort, includes));
	}
	
}

package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniTaskSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirTaskService;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.TaskFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@R4Provider
public class BahmniTaskFhirR4ResourceProvider extends TaskFhirResourceProvider {
	
	@Autowired
	private BahmniFhirTaskService bahmniFhirTaskService;
	
	@Search
	public IBundleProvider searchTasks(
	        @OptionalParam(name = Task.SP_BASED_ON) ReferenceAndListParam basedOnReference,
	        @OptionalParam(name = Task.SP_OWNER, chainWhitelist = { "",
	                Practitioner.SP_IDENTIFIER }, targetTypes = Practitioner.class) ReferenceAndListParam ownerReference,
	        @OptionalParam(name = Task.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_GIVEN,
	                Patient.SP_FAMILY, Patient.SP_NAME, Task.SP_CODE }, targetTypes = Patient.class) ReferenceAndListParam forReference,
	        @OptionalParam(name = Task.SP_FOCUS, chainWhitelist = { "" }) ReferenceAndListParam focusReference,
	        @OptionalParam(name = Task.SP_STATUS) TokenAndListParam status,
	        @OptionalParam(name = Task.SP_CODE) TokenAndListParam taskCode,
	        @OptionalParam(name = Task.SP_ENCOUNTER, chainWhitelist = { "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = BahmniFhirConstants.SP_TASK_NAME) StringAndListParam name,
	        @OptionalParam(name = Task.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
	        @IncludeParam(allow = { "Task:" + Task.SP_BASED_ON, "Task:" + Task.SP_OWNER,
	                "Task:" + Task.SP_SUBJECT }) HashSet<Include> includes,
	        @Sort SortSpec sort) {

		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		params.setBasedOnReference(basedOnReference);
		params.setOwnerReference(ownerReference);
		params.setForReference(forReference);
		params.setFocusReference(focusReference);
		params.setStatus(status);
		params.setTaskCode(taskCode);
		params.setEncounterReference(encounterReference);
		params.setName(name);
		params.setId(id);
		params.setLastUpdated(lastUpdated);
		params.setIncludes(includes != null ? includes : new HashSet<>());
		params.setSort(sort);

		return bahmniFhirTaskService.searchForTasks(params);
	}
}

package org.bahmni.module.fhir2addlextension.api.dao;

import org.openmrs.module.fhir2.api.dao.FhirTaskDao;
import org.openmrs.module.fhir2.model.FhirTask;

public interface BahmniFhirTaskDao extends FhirTaskDao {
	
	FhirTask getTaskByOrderUuid(String orderUuid);
}

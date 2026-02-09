package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.bahmni.module.fhir2AddlExtension.api.PrivilegeConstants;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.List;

public interface BahmniFhirAppointmentDao extends FhirDao<org.openmrs.module.appointments.model.Appointment> {
	
	@Override
	@Authorized({ PrivilegeConstants.GET_APPOINTMENTS })
	org.openmrs.module.appointments.model.Appointment get(@Nonnull String uuid);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_APPOINTMENTS })
	List<org.openmrs.module.appointments.model.Appointment> getSearchResults(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_APPOINTMENTS })
	int getSearchResultsCount(@Nonnull SearchParameterMap searchParameterMap);
}

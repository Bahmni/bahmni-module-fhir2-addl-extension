package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hibernate.criterion.Criterion;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.dao.impl.FhirMedicationRequestDaoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;

@Component
@Primary
public class BahmniFhirMedicationRequestDaoImpl extends FhirMedicationRequestDaoImpl {
	
	@Autowired
	private OrderService orderService;
	
	@Override
	public DrugOrder createOrUpdate(@Nonnull DrugOrder newEntry) {
		return (DrugOrder) orderService.saveOrder(newEntry, null);
	}
	
	@Override
	protected Criterion generateSystemQuery(String system, List<String> codes, String conceptReferenceTermAlias) {
		if (ModuleUtils.isConceptReferenceCodeEmpty(codes)) {
			return ModuleUtils.generateSystemQueryForEmptyCodes(system, conceptReferenceTermAlias);
		}
		return super.generateSystemQuery(system, codes, conceptReferenceTermAlias);
	}
}

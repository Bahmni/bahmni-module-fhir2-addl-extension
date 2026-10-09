package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.FhirConceptCodeSystemQuery;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.dao.impl.FhirMedicationRequestDaoImpl;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static org.hibernate.criterion.Restrictions.eq;

@Component
@Primary
public class BahmniFhirMedicationRequestDaoImpl extends FhirMedicationRequestDaoImpl implements FhirConceptCodeSystemQuery {
	
	@Autowired
	private OrderService orderService;
	
	@Override
	public DrugOrder createOrUpdate(@Nonnull DrugOrder newEntry) {
		return (DrugOrder) orderService.saveOrder(newEntry, null);
	}
	
	@Override
	protected Criterion generateSystemQuery(String system, List<String> codes, String conceptReferenceTermAlias) {
		if (isConceptReferenceCodeEmpty(codes)) {
			return generateSystemQueryForEmptyCodes(system, conceptReferenceTermAlias);
		}
		return super.generateSystemQuery(system, codes, conceptReferenceTermAlias);
	}
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(entry -> {
			if (BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(
				    param -> handleLocationReference(criteria, (ReferenceAndListParam) param.getParam()));
			}
		});
	}
	
	private void handleLocationReference(Criteria criteria, ReferenceAndListParam locationReference) {
		if (locationReference == null)
			return;
		if (lacksAlias(criteria, "e"))
			criteria.createAlias("encounter", "e");
		if (lacksAlias(criteria, "l"))
			criteria.createAlias("e.location", "l");

		handleAndListParam(locationReference, token -> Optional.of(eq("l.uuid", token.getValue()))).ifPresent(criteria::add);
	}
}

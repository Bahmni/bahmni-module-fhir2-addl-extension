package org.bahmni.module.fhir2addlextension.api.service.impl;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@Setter(AccessLevel.PACKAGE)
@Slf4j
public class DispositionOrderTypeResolverImpl implements DispositionOrderTypeResolver {

	private static final String EMRAPI_CONCEPT_SOURCE_NAME = "org.openmrs.module.emrapi";

	private static final String ADT_ORDER_TYPE_NAME = "ADT Order";

	private static final Set<String> DISPOSITION_EMRAPI_CODES = new HashSet<>(
	        Arrays.asList("ADMIT", "DISCHARGE", "TRANSFER", "UNDO_DISCHARGE"));

	@Autowired
	private OrderService orderService;

	@Override
	public OrderType resolve(Concept concept) {
		if (concept == null || concept.getConceptMappings() == null) {
			return null;
		}

		boolean isDispositionConcept = concept.getConceptMappings().stream().anyMatch(this::isDispositionMapping);

		if (!isDispositionConcept) {
			return null;
		}

		OrderType adtOrderType = orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME);
		if (adtOrderType == null) {
			log.warn("Disposition concept matched an emrapi reference code, but OrderType '{}' was not found",
			    ADT_ORDER_TYPE_NAME);
		}
		return adtOrderType;
	}

	private boolean isDispositionMapping(ConceptMap conceptMap) {
		if (conceptMap.getConceptReferenceTerm() == null || conceptMap.getConceptReferenceTerm().getConceptSource() == null) {
			return false;
		}
		String sourceName = conceptMap.getConceptReferenceTerm().getConceptSource().getName();
		String code = conceptMap.getConceptReferenceTerm().getCode();
		return EMRAPI_CONCEPT_SOURCE_NAME.equals(sourceName) && DISPOSITION_EMRAPI_CODES.contains(code);
	}
}

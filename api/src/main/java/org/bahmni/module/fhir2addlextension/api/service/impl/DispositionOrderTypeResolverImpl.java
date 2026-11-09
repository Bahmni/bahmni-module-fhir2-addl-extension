package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.model.DispositionConfig;
import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.OrderType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter(AccessLevel.PACKAGE)
public class DispositionOrderTypeResolverImpl implements DispositionOrderTypeResolver {
	
	private static final String EMRAPI_CONCEPT_SOURCE_NAME = "org.openmrs.module.emrapi";
	
	private static final String EMRAPI_DISPOSITION_CONCEPT_CODE = "Disposition";
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private ConceptService conceptService;
	
	@Autowired
	private AppContext appContext;
	
	@Override
	public OrderType resolve(Concept concept) {
		if (getDisposition(concept) == null) {
			return null;
		}
		String orderTypeName = getAdtOrderTypeName();
		OrderType adtOrderType = orderService.getOrderTypeByName(orderTypeName);
		if (adtOrderType == null) {
			throw new InvalidRequestException("OrderType '" + orderTypeName + "' is not configured");
		}
		return adtOrderType;
	}
	
	@Override
	public String getAdtOrderTypeName() {
		return appContext.getDispositionConfig().getAdtOrderTypeName();
	}
	
	@Override
	public DispositionInfo getDisposition(Concept concept) {
		if (concept == null || concept.getConceptMappings() == null) {
			return null;
		}
		DispositionConfig config = appContext.getDispositionConfig();
		for (ConceptMap conceptMap : concept.getConceptMappings()) {
			if (conceptMap.getConceptReferenceTerm() == null
			        || conceptMap.getConceptReferenceTerm().getConceptSource() == null
			        || !EMRAPI_CONCEPT_SOURCE_NAME.equals(conceptMap.getConceptReferenceTerm().getConceptSource().getName())) {
				continue;
			}
			String code = conceptMap.getConceptReferenceTerm().getCode();
			DispositionConfig.Rule rule = config.getRule(code);
			if (rule != null || isDispositionAnswer(concept)) {
				return new DispositionInfo(code, labelOf(concept, code), rule);
			}
		}
		return null;
	}
	
	private boolean isDispositionAnswer(Concept concept) {
		Concept dispositionConcept = conceptService.getConceptByMapping(EMRAPI_DISPOSITION_CONCEPT_CODE,
		    EMRAPI_CONCEPT_SOURCE_NAME);
		if (dispositionConcept == null || dispositionConcept.getAnswers() == null) {
			return false;
		}
		for (ConceptAnswer answer : dispositionConcept.getAnswers()) {
			if (concept.equals(answer.getAnswerConcept())) {
				return true;
			}
		}
		return false;
	}
	
	private String labelOf(Concept concept, String fallback) {
		ConceptName name = concept.getName();
		return name != null && name.getName() != null ? name.getName() : fallback;
	}
}

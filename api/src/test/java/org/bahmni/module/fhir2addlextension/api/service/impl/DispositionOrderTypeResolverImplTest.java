package org.bahmni.module.fhir2addlextension.api.service.impl;

import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DispositionOrderTypeResolverImplTest {
	
	private static final String EMRAPI_SOURCE_NAME = "org.openmrs.module.emrapi";
	
	private static final String ADT_ORDER_TYPE_NAME = "ADT Order";
	
	@Mock
	private OrderService orderService;
	
	private DispositionOrderTypeResolver resolver;
	
	private OrderType adtOrderType;
	
	@Before
	public void setup() {
		DispositionOrderTypeResolverImpl resolverImpl = new DispositionOrderTypeResolverImpl();
		resolverImpl.setOrderService(orderService);
		resolver = resolverImpl;
		
		adtOrderType = new OrderType();
		adtOrderType.setName(ADT_ORDER_TYPE_NAME);
	}
	
	private Concept conceptWithMapping(String sourceName, String code) {
		Concept concept = new Concept();
		ConceptSource source = new ConceptSource();
		source.setName(sourceName);
		ConceptReferenceTerm term = new ConceptReferenceTerm();
		term.setConceptSource(source);
		term.setCode(code);
		ConceptMap conceptMap = new ConceptMap();
		conceptMap.setConceptReferenceTerm(term);
		Set<ConceptMap> mappings = new HashSet<>();
		mappings.add(conceptMap);
		concept.setConceptMappings(mappings);
		return concept;
	}
	
	@Test
	public void shouldResolveAdtOrderTypeForAdmitCode() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "ADMIT");
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(adtOrderType);
		
		assertEquals(adtOrderType, resolver.resolve(concept));
	}
	
	@Test
	public void shouldResolveAdtOrderTypeForDischargeCode() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "DISCHARGE");
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(adtOrderType);
		
		assertEquals(adtOrderType, resolver.resolve(concept));
	}
	
	@Test
	public void shouldResolveAdtOrderTypeForTransferCode() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "TRANSFER");
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(adtOrderType);
		
		assertEquals(adtOrderType, resolver.resolve(concept));
	}
	
	@Test
	public void shouldResolveAdtOrderTypeForUndoDischargeCode() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "UNDO_DISCHARGE");
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(adtOrderType);
		
		assertEquals(adtOrderType, resolver.resolve(concept));
	}
	
	@Test
	public void shouldReturnNullWhenConceptHasNoMappings() {
		Concept concept = new Concept();
		concept.setConceptMappings(Collections.emptySet());
		
		assertNull(resolver.resolve(concept));
	}
	
	@Test
	public void shouldReturnNullWhenConceptIsNull() {
		assertNull(resolver.resolve(null));
	}
	
	@Test
	public void shouldReturnNullWhenEmrapiCodeDoesNotMatchDispositionCodes() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "SOME_OTHER_CODE");
		
		assertNull(resolver.resolve(concept));
	}
	
	@Test
	public void shouldReturnNullWhenSourceIsNotEmrapi() {
		Concept concept = conceptWithMapping("some.other.source", "ADMIT");
		
		assertNull(resolver.resolve(concept));
	}
	
	@Test
	public void shouldReturnNullWhenCodeMatchesButOrderTypeNotFound() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "ADMIT");
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(null);
		
		assertNull(resolver.resolve(concept));
	}
}

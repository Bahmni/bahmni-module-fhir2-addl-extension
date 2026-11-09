package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.model.DispositionConfig;
import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.OrderType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DispositionOrderTypeResolverImplTest {
	
	private static final String EMRAPI_SOURCE_NAME = "org.openmrs.module.emrapi";
	
	private static final String ADT_ORDER_TYPE_NAME = "ADT Order";
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private AppContext appContext;
	
	private DispositionOrderTypeResolver resolver;
	
	private OrderType adtOrderType;
	
	@Before
	public void setup() {
		DispositionOrderTypeResolverImpl resolverImpl = new DispositionOrderTypeResolverImpl();
		resolverImpl.setOrderService(orderService);
		resolverImpl.setConceptService(conceptService);
		resolverImpl.setAppContext(appContext);
		Map<String, DispositionConfig.Rule> rules = new HashMap<>();
		for (String code : new String[] { "ADMIT", "DISCHARGE", "TRANSFER", "UNDO_DISCHARGE" }) {
			rules.put(code, new DispositionConfig.Rule());
		}
		lenient().when(appContext.getDispositionConfig()).thenReturn(new DispositionConfig(ADT_ORDER_TYPE_NAME, rules));
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
	public void shouldThrowWhenCodeMatchesButOrderTypeNotConfigured() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "ADMIT");
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(null);
		
		InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> resolver.resolve(concept));
		assertEquals("OrderType 'ADT Order' is not configured", e.getMessage());
	}
	
	@Test
	public void shouldReturnDispositionWithLabelAndRule() {
		Concept concept = org.mockito.Mockito.spy(conceptWithMapping(EMRAPI_SOURCE_NAME, "TRANSFER"));
		org.mockito.Mockito.doReturn(new ConceptName("Transfer Patient", Locale.ENGLISH)).when(concept).getName();
		
		DispositionInfo info = resolver.getDisposition(concept);
		assertEquals("TRANSFER", info.getCode());
		assertEquals("Transfer Patient", info.getLabel());
		assertNotNull(info.getRule());
	}
	
	@Test
	public void shouldReturnNullDispositionForNonDispositionOrNullConcept() {
		assertNull(resolver.getDisposition(null));
		assertNull(resolver.getDisposition(conceptWithMapping(EMRAPI_SOURCE_NAME, "SOME_OTHER_CODE")));
		assertNull(resolver.getDisposition(conceptWithMapping("some.other.source", "ADMIT")));
	}
	
	@Test
	public void shouldRecogniseAnswerOfDispositionConceptEvenWithoutConfiguredRule() {
		Concept answerConcept = conceptWithMapping(EMRAPI_SOURCE_NAME, "NEW_DISPOSITION");
		Concept dispositionConcept = new Concept();
		dispositionConcept.addAnswer(new ConceptAnswer(answerConcept));
		when(conceptService.getConceptByMapping("Disposition", EMRAPI_SOURCE_NAME)).thenReturn(dispositionConcept);
		when(orderService.getOrderTypeByName(ADT_ORDER_TYPE_NAME)).thenReturn(adtOrderType);
		
		DispositionInfo info = resolver.getDisposition(answerConcept);
		assertEquals("NEW_DISPOSITION", info.getCode());
		assertNull(info.getRule());
		assertEquals(adtOrderType, resolver.resolve(answerConcept));
	}
	
	@Test
	public void shouldReturnDispositionEvenWhenAnotherEmrapiMappingExists() {
		Concept concept = conceptWithMapping(EMRAPI_SOURCE_NAME, "SOME_OTHER_CODE");
		concept.getConceptMappings().addAll(conceptWithMapping(EMRAPI_SOURCE_NAME, "ADMIT").getConceptMappings());
		
		assertEquals("ADMIT", resolver.getDisposition(concept).getCode());
	}
}

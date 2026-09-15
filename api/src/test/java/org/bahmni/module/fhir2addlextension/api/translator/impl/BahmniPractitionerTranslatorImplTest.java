package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.PractitionerIdentifierTranslator;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniPractitionerTranslatorImplTest {
	
	@Mock
	private PractitionerIdentifierTranslator practitionerIdentifierTranslator;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	private BahmniPractitionerTranslatorImpl translator;
	
	private Provider provider;
	
	@Before
	public void setup() {
		translator = new BahmniPractitionerTranslatorImpl();
		translator.setPractitionerIdentifierTranslator(practitionerIdentifierTranslator);
		when(globalPropertyService.getGlobalProperty(anyString())).thenReturn("");
		setSuperField("globalPropertyService", globalPropertyService);
		
		provider = new Provider();
		provider.setUuid("provider-uuid");
		provider.setIdentifier("base-identifier-value");
	}
	
	private void setSuperField(String fieldName, Object value) {
		try {
			java.lang.reflect.Field field = translator.getClass().getSuperclass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(translator, value);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void toFhirResource_shouldAppendAttributeDerivedIdentifiersToBaseIdentifiers() {
		Identifier extraIdentifier = new Identifier().setSystem("http://fhir.bahmni.org/identifier/provider-license")
		        .setValue("LIC-123");
		when(practitionerIdentifierTranslator.getAttributeDerivedIdentifiers(provider)).thenReturn(
		    Collections.singletonList(extraIdentifier));

		Practitioner result = translator.toFhirResource(provider);

		Assert.assertNotNull(result);
		List<String> identifierValues = new ArrayList<>();
		for (Identifier identifier : result.getIdentifier()) {
			identifierValues.add(identifier.getValue());
		}
		Assert.assertTrue(identifierValues.contains("base-identifier-value"));
		Assert.assertTrue(identifierValues.contains("LIC-123"));
		Assert.assertEquals(2, result.getIdentifier().size());
	}
	
	@Test
	public void toFhirResource_shouldReturnOnlyBaseIdentifierWhenNoAttributeDerivedIdentifiers() {
		when(practitionerIdentifierTranslator.getAttributeDerivedIdentifiers(provider)).thenReturn(Collections.emptyList());
		
		Practitioner result = translator.toFhirResource(provider);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getIdentifier().size());
		Assert.assertEquals("base-identifier-value", result.getIdentifier().get(0).getValue());
	}
}

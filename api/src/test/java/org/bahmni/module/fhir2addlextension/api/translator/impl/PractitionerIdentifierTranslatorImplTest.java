package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PractitionerIdentifierTranslatorImplTest {
	
	@Mock
	private AppContext appContext;
	
	private PractitionerIdentifierTranslatorImpl translator;
	
	@Before
	public void setup() {
		translator = new PractitionerIdentifierTranslatorImpl();
		setField(translator, appContext);
	}
	
	private void setField(PractitionerIdentifierTranslatorImpl translator, AppContext appContext) {
		try {
			java.lang.reflect.Field field = PractitionerIdentifierTranslatorImpl.class.getDeclaredField("appContext");
			field.setAccessible(true);
			field.set(translator, appContext);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private ProviderAttribute buildAttribute(String typeName, String value) {
		ProviderAttributeType type = new ProviderAttributeType();
		type.setName(typeName);
		ProviderAttribute attribute = new ProviderAttribute();
		attribute.setAttributeType(type);
		attribute.setValueReferenceInternal(value);
		return attribute;
	}
	
	@Test
	public void shouldReturnIdentifierForMappedAttributeTypeWithValue() {
		Map<String, String> map = new HashMap<>();
		map.put("License Number", "http://fhir.bahmni.org/identifier/provider-license");
		when(appContext.getPractitionerAttributeIdentifierSystemMap()).thenReturn(map);

		Provider provider = new Provider();
		provider.addAttribute(buildAttribute("License Number", "LIC-123"));

		List<Identifier> identifiers = translator.getAttributeDerivedIdentifiers(provider);

		Assert.assertEquals(1, identifiers.size());
		Assert.assertEquals("http://fhir.bahmni.org/identifier/provider-license", identifiers.get(0).getSystem());
		Assert.assertEquals("LIC-123", identifiers.get(0).getValue());
	}
	
	@Test
	public void shouldNotReturnIdentifierForUnmappedAttributeType() {
		Map<String, String> map = new HashMap<>();
		map.put("License Number", "http://fhir.bahmni.org/identifier/provider-license");
		when(appContext.getPractitionerAttributeIdentifierSystemMap()).thenReturn(map);

		Provider provider = new Provider();
		provider.addAttribute(buildAttribute("Some Other Type", "value"));

		List<Identifier> identifiers = translator.getAttributeDerivedIdentifiers(provider);

		Assert.assertTrue(identifiers.isEmpty());
	}
	
	@Test
	public void shouldSkipMappedAttributeTypeWithNullOrBlankValue() {
		Map<String, String> map = new HashMap<>();
		map.put("License Number", "http://fhir.bahmni.org/identifier/provider-license");
		when(appContext.getPractitionerAttributeIdentifierSystemMap()).thenReturn(map);

		Provider provider = new Provider();
		provider.addAttribute(buildAttribute("License Number", null));
		provider.addAttribute(buildAttribute("License Number", "  "));

		List<Identifier> identifiers = translator.getAttributeDerivedIdentifiers(provider);

		Assert.assertTrue(identifiers.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListWhenMapIsEmpty() {
		when(appContext.getPractitionerAttributeIdentifierSystemMap()).thenReturn(Collections.emptyMap());
		
		Provider provider = new Provider();
		provider.addAttribute(buildAttribute("License Number", "LIC-123"));
		
		List<Identifier> identifiers = translator.getAttributeDerivedIdentifiers(provider);
		
		Assert.assertTrue(identifiers.isEmpty());
	}
	
	@Test
	public void shouldOnlyReturnIdentifiersForMappedAndValuedAttributesAmongMultiple() {
		Map<String, String> map = new HashMap<>();
		map.put("License Number", "http://fhir.bahmni.org/identifier/provider-license");
		map.put("Registration Number", "http://fhir.bahmni.org/identifier/provider-registration");
		when(appContext.getPractitionerAttributeIdentifierSystemMap()).thenReturn(map);

		Provider provider = new Provider();
		provider.addAttribute(buildAttribute("License Number", "LIC-123"));
		provider.addAttribute(buildAttribute("Registration Number", null));
		provider.addAttribute(buildAttribute("Unmapped Type", "some-value"));

		List<Identifier> identifiers = translator.getAttributeDerivedIdentifiers(provider);

		Assert.assertEquals(1, identifiers.size());
		Assert.assertEquals("LIC-123", identifiers.get(0).getValue());
	}
}

package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.dao.OrderAttributeTypeDao;
import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestAttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestExtensionTranslator;
import org.hl7.fhir.r4.model.Extension;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderGroupAttributeType;
import org.openmrs.OrderGroupAttributeType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.bahmni.module.fhir2addlextension.api.TestDataFactory.exampleOrderAttrTypeIsBillingExempt;
import static org.bahmni.module.fhir2addlextension.api.TestDataFactory.exampleOrderAttrTypePriority;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestExtensionTranslatorImplTest {
	
	@Mock
	private OrderAttributeTypeDao attributeTypeDao;
	
	private ServiceRequestExtensionTranslator extensionTranslator;
	
	private ServiceRequestAttributeTranslatorImpl defaultAttributeTranslator;
	
	private final List<OrderAttributeType> supportedAttributes = Arrays.asList(exampleOrderAttrTypeIsBillingExempt(),
	    exampleOrderAttrTypePriority());
	
	@Before
	public void setUp() {
		when(attributeTypeDao.getOrderAttributeTypes(false)).thenReturn(supportedAttributes);
		defaultAttributeTranslator = new ServiceRequestAttributeTranslatorImpl(attributeTypeDao);
		extensionTranslator = new ServiceRequestExtensionTranslatorImpl(defaultAttributeTranslator);
	}
	
	@Test
	public void shouldReturnDefaultAttributeTranslatorForExtensionUrl() {
		Optional<ServiceRequestAttributeTranslator> attributeTranslator = extensionTranslator
		        .getAttributeTranslator("http://fhir.bahmni.org/ext/service-request/is-billing-exempt");
		Assert.assertTrue(attributeTranslator.isPresent());
		Assert.assertTrue(attributeTranslator.get() instanceof ServiceRequestAttributeTranslatorImpl);
	}
	
	@Test
	public void shouldNotReturnAttributeTranslatorForUnknownExtensionUrl() {
		Optional<ServiceRequestAttributeTranslator> attributeTranslator = extensionTranslator
		        .getAttributeTranslator("http://fhir.bahmni.org/ext/unknown/is-billing-exempt");
		Assert.assertFalse(attributeTranslator.isPresent());
	}
	
	@Test
	public void shouldReturnRegisteredTranslatorForIsBillingExemptAttribute() {
		extensionTranslator.registerAttributeTranslator(proxyTranslator("Is billing exempt"));
		Optional<ServiceRequestAttributeTranslator> attributeTranslator = extensionTranslator
		        .getAttributeTranslator("http://fhir.bahmni.org/ext/service-request/is-billing-exempt");
		Assert.assertTrue(attributeTranslator.isPresent());
		Assert.assertFalse(attributeTranslator.get() instanceof ServiceRequestAttributeTranslatorImpl);
	}
	
	private ServiceRequestAttributeTranslator proxyTranslator(String attrName) {
		return new ServiceRequestAttributeTranslator() {
			
			@Override
			public boolean supports(OrderAttribute attribute) {
				return attribute.equals(supportedAttributes.get(0));
			}
			
			@Override
			public List<OrderAttribute> toOpenmrsType(String extUrl, List<Extension> extensions) {
				return Collections.emptyList();
			}
			
			@Override
			public Extension toFhirResource(OrderAttribute attribute) {
				return null;
			}
			
			@Override
			public Optional<OrderAttributeType> getAttributeType(String extUrl) {
				OrderAttributeType attributeType = new OrderAttributeType();
				attributeType.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
				attributeType.setName(attrName);
				attributeType.setMaxOccurs(1);
				return Optional.of(attributeType);
			}
		};
	}
}

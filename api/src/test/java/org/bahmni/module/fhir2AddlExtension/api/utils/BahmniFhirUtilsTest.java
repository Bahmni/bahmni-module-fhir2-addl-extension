package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadDiagnosticReportBundle;

public class BahmniFhirUtilsTest {
	
	@Test
	public void shouldExtractIdFromReference() {
		String reference = "urn:uuid:123";
		Assert.assertEquals("123", BahmniFhirUtils.referenceToId(reference).get());
		reference = "urn:uuid:";
		Assert.assertEquals(false, BahmniFhirUtils.referenceToId(reference).isPresent());
		reference = "https://example.org/ServiceRequest/123";
		Assert.assertEquals("123", BahmniFhirUtils.referenceToId(reference).get());
		reference = "ServiceRequest/123";
		Assert.assertEquals("123", BahmniFhirUtils.referenceToId(reference).get());
	}
	
	@Test
	public void shouldExtractIdFromString() {
		String reference = "urn:uuid:123";
		Assert.assertEquals("123", BahmniFhirUtils.extractId(reference));
		reference = "urn:uuid:";
		Assert.assertEquals(null, BahmniFhirUtils.extractId(reference));
		reference = "https://example.org/ServiceRequest/123";
		Assert.assertEquals("123", BahmniFhirUtils.extractId(reference));
		reference = "ServiceRequest/123";
		Assert.assertEquals("123", BahmniFhirUtils.extractId(reference));
		reference = "123";
		Assert.assertEquals("123", BahmniFhirUtils.extractId(reference));
	}
	
	@Test
	public void shouldFindResourceInBundle() throws IOException {
		DiagnosticReportBundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-with-encounter-and-service-request-reference-and-result-observation.json");
		Optional<Observation> observation = BahmniFhirUtils.findResourceInBundle(reportBundle,
		    "49a86246-4004-42eb-9bdc-f542f93f9228", Observation.class);
		Assert.assertTrue(observation.isPresent());
		Assert.assertEquals("1331AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", observation.get().getCode().getCoding().get(0).getCode());
	}
	
	@Test
	public void shouldFindResourceOfTypeInBundle() throws IOException {
		DiagnosticReportBundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-reference.json");
		List<Observation> observations = BahmniFhirUtils.findResourcesOfTypeInBundle(reportBundle, Observation.class);
		Assert.assertEquals(2, observations.size());
	}
}

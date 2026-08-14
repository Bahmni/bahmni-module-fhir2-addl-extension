package org.bahmni.module.fhir2addlextension.api.utils;

import static org.bahmni.module.fhir2addlextension.api.TestDataFactory.loadDiagnosticReportBundle;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.Assert;
import org.junit.Test;

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
		Bundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-with-encounter-and-service-request-reference-and-result-observation.json");
		Optional<Observation> observation = BahmniFhirUtils.findResourceInBundle(reportBundle,
		    "49a86246-4004-42eb-9bdc-f542f93f9228", Observation.class);
		Assert.assertTrue(observation.isPresent());
		Assert.assertEquals("1331AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", observation.get().getCode().getCoding().get(0).getCode());
	}
	
	@Test
	public void shouldFindResourceOfTypeInBundle() throws IOException {
		Bundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-reference.json");
		List<Observation> observations = BahmniFhirUtils.findResourcesOfTypeInBundle(reportBundle, Observation.class);
		Assert.assertEquals(2, observations.size());
	}
	
	@Test
	public void isSameDay_givenSameDayDifferentTime_shouldReturnTrue() {
		Calendar cal = Calendar.getInstance();
		cal.set(2025, Calendar.JUNE, 10, 8, 0, 0);
		Date morning = cal.getTime();
		cal.set(2025, Calendar.JUNE, 10, 23, 59, 59);
		Date evening = cal.getTime();
		
		assertThat(BahmniFhirUtils.isSameDay(morning, evening), equalTo(true));
	}
	
	@Test
	public void isSameDay_givenDifferentDays_shouldReturnFalse() {
		Calendar cal = Calendar.getInstance();
		cal.set(2025, Calendar.JUNE, 10, 12, 0, 0);
		Date day1 = cal.getTime();
		cal.set(2025, Calendar.JUNE, 11, 12, 0, 0);
		Date day2 = cal.getTime();
		
		assertThat(BahmniFhirUtils.isSameDay(day1, day2), equalTo(false));
	}
	
	@Test
	public void isFutureDate_givenTomorrow_shouldReturnTrue() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 1);
		Date tomorrow = cal.getTime();
		
		assertThat(BahmniFhirUtils.isFutureDate(tomorrow, now), equalTo(true));
	}
	
	@Test
	public void isFutureDate_givenToday_shouldReturnFalse() {
		Date now = new Date();
		
		assertThat(BahmniFhirUtils.isFutureDate(now, now), equalTo(false));
	}
	
	@Test
	public void isFutureDate_givenYesterday_shouldReturnFalse() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterday = cal.getTime();
		
		assertThat(BahmniFhirUtils.isFutureDate(yesterday, now), equalTo(false));
	}
	
	@Test
	public void isFutureDate_givenLaterTodaySameDay_shouldReturnFalse() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 8);
		Date morning = cal.getTime();
		cal.set(Calendar.HOUR_OF_DAY, 23);
		Date evening = cal.getTime();
		
		assertThat(BahmniFhirUtils.isFutureDate(evening, morning), equalTo(false));
	}
}

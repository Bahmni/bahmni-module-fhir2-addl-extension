package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.hl7.fhir.r4.model.Timing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptSource;
import org.openmrs.Duration;
import org.openmrs.module.fhir2.api.FhirConceptService;
import org.openmrs.module.fhir2.api.FhirConceptSourceService;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniDurationUnitTranslatorImplTest {
	
	private static final String CONCEPT_UUID = "test-concept-uuid";
	
	private static final String CONCEPT_NAME = "Test Concept";
	
	@Mock
	private FhirConceptService fhirConceptService;
	
	@Mock
	private FhirConceptSourceService fhirConceptSourceService;
	
	@InjectMocks
	private BahmniDurationUnitTranslatorImpl translator;
	
	private ConceptSource conceptSource;
	
	private Concept concept;
	
	@Before
	public void setup() {
		// Create a sample ConceptSource
		conceptSource = new ConceptSource();
		conceptSource.setName("SNOMED CT");
		conceptSource.setHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE);
		
		// Create a sample Concept
		concept = new Concept();
		concept.setUuid(CONCEPT_UUID);
		concept.setConceptId(123);
	}
	
	@Test
	public void shouldTranslateSecondsUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_SECONDS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.S);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_SECONDS_CODE);
	}
	
	@Test
	public void shouldTranslateMinutesUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_MINUTES_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.MIN);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_MINUTES_CODE);
	}
	
	@Test
	public void shouldTranslateHoursUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_HOURS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.H);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_HOURS_CODE);
	}
	
	@Test
	public void shouldTranslateDaysUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_DAYS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.D);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_DAYS_CODE);
	}
	
	@Test
	public void shouldTranslateWeeksUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_WEEKS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.WK);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_WEEKS_CODE);
	}
	
	@Test
	public void shouldTranslateMonthsUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_MONTHS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.MO);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_MONTHS_CODE);
	}
	
	@Test
	public void shouldTranslateYearsUnitsOfTimeToConcept() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_YEARS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.A);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_YEARS_CODE);
	}
	
	@Test
	public void shouldReturnNullWhenConceptSourceNotFound() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.empty());
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.S);
		
		// Then
		assertThat(result, nullValue());
		verify(fhirConceptService, never()).getConceptWithSameAsMappingInSource(any(), any());
	}
	
	@Test
	public void shouldReturnNullWhenConceptNotFoundForDurationCode() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		when(fhirConceptService.getConceptWithSameAsMappingInSource(eq(conceptSource), any())).thenReturn(Optional.empty());
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.S);
		
		// Then
		assertThat(result, nullValue());
		verify(fhirConceptService).getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_SECONDS_CODE);
	}
	
	@Test
	public void shouldReturnNullForUnsupportedUnitsOfTime() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		
		// When - using NULL which is not mapped in the codeMap
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.NULL);
		
		// Then
		assertThat(result, nullValue());
		// Verify that concept service is never called since the unit is not in the map
		verify(fhirConceptService, never()).getConceptWithSameAsMappingInSource(any(), any());
	}
	
	@Test
	public void shouldReturnNullWhenTranslatingNullUnitsOfTime() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		
		// When
		Concept result = translator.toOpenmrsType(null);
		
		// Then
		assertThat(result, nullValue());
		// Verify that concept service is never called since null won't match any units
		verify(fhirConceptService, never()).getConceptWithSameAsMappingInSource(any(), any());
	}
	
	@Test
	public void shouldReturnFirstMatchingConceptFound() {
		// Given
		when(fhirConceptSourceService.getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE)).thenReturn(
		    Optional.of(conceptSource));
		// Only set up the stub for the years code since that's what we're testing
		when(fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource, Duration.SNOMED_CT_YEARS_CODE))
		        .thenReturn(Optional.of(concept));
		
		// When
		Concept result = translator.toOpenmrsType(Timing.UnitsOfTime.A);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result, equalTo(concept));
		// Verify only the years code was checked (since we're looking for A = years)
		verify(fhirConceptService, times(1)).getConceptWithSameAsMappingInSource(conceptSource,
		    Duration.SNOMED_CT_YEARS_CODE);
		// Verify no other codes were checked
		verify(fhirConceptService, times(1)).getConceptWithSameAsMappingInSource(any(), any());
	}
}

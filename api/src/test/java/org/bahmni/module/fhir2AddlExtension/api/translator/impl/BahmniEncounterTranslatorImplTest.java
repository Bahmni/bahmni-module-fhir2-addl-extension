package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.validators.BahmniEncounterValidator;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.*;
import org.openmrs.module.fhir2.api.translators.*;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniEncounterTranslatorImplTest {
	
	@Mock
	private EncounterParticipantTranslator participantTranslator;
	
	@Mock
	private EncounterLocationTranslator encounterLocationTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Visit> visitReferenceTranslator;
	
	@Mock
	private EncounterTypeTranslator<EncounterType> encounterTypeTranslator;
	
	@Mock
	private EncounterPeriodTranslator<org.openmrs.Encounter> encounterPeriodTranslator;
	
	@Mock
	private BahmniEncounterValidator bahmniEncounterValidator;
	
	private BahmniEncounterTranslatorImpl translator;
	
	private org.openmrs.Encounter existingEncounter;
	
	private Encounter fhirEncounter;
	
	private Provider existingProvider;
	
	private Provider newProvider;
	
	private EncounterProvider existingEncounterProvider;
	
	private EncounterProvider newEncounterProvider;
	
	private EncounterRole encounterRole;
	
	@Before
	public void setup() {
		translator = new BahmniEncounterTranslatorImpl();
		translator.setParticipantTranslator(participantTranslator);
		translator.setEncounterLocationTranslator(encounterLocationTranslator);
		translator.setPatientReferenceTranslator(patientReferenceTranslator);
		translator.setVisitReferenceTranlator(visitReferenceTranslator);
		translator.setEncounterTypeTranslator(encounterTypeTranslator);
		translator.setEncounterPeriodTranslator(encounterPeriodTranslator);
		translator.setBahmniEncounterValidator(bahmniEncounterValidator);
		existingEncounter = new org.openmrs.Encounter();
		existingEncounter.setUuid("existing-encounter-uuid");
		
		fhirEncounter = new Encounter();
		fhirEncounter.setId("fhir-encounter-id");
		fhirEncounter.setPeriod(new Period());
		
		fhirEncounter.setSubject(new Reference("Patient/123"));
		
		existingProvider = new Provider();
		existingProvider.setUuid("existing-provider-uuid");
		existingProvider.setName("Existing Provider");
		
		newProvider = new Provider();
		newProvider.setUuid("new-provider-uuid");
		newProvider.setName("New Provider");
		
		encounterRole = new EncounterRole();
		encounterRole.setUuid("encounter-role-uuid");
		encounterRole.setName("Clinician");
		
		existingEncounterProvider = new EncounterProvider();
		existingEncounterProvider.setUuid("existing-encounter-provider-uuid");
		existingEncounterProvider.setProvider(existingProvider);
		existingEncounterProvider.setEncounterRole(encounterRole);
		existingEncounterProvider.setEncounter(existingEncounter);
		
		newEncounterProvider = new EncounterProvider();
		newEncounterProvider.setUuid("new-encounter-provider-uuid");
		newEncounterProvider.setProvider(newProvider);
		newEncounterProvider.setEncounterRole(encounterRole);
		newEncounterProvider.setEncounter(existingEncounter);
		
		when(patientReferenceTranslator.toOpenmrsType(any())).thenReturn(new Patient());
		when(encounterLocationTranslator.toOpenmrsType(any())).thenReturn(new Location());
		when(visitReferenceTranslator.toOpenmrsType(any())).thenReturn(new Visit());
		when(encounterTypeTranslator.toOpenmrsType(any())).thenReturn(new EncounterType());
		when(encounterPeriodTranslator.toOpenmrsType(existingEncounter, fhirEncounter.getPeriod())).thenReturn(
		    existingEncounter);
		doNothing().when(bahmniEncounterValidator).validate(any(), any());
	}
	
	@Test
    public void shouldPreserveExistingProviderWhenProviderExistsInBothEncounters() {
        Set<EncounterProvider> existingProviders = new LinkedHashSet<>();
        existingProviders.add(existingEncounterProvider);
        existingEncounter.setEncounterProviders(existingProviders);

        Encounter.EncounterParticipantComponent participantComponent = new Encounter.EncounterParticipantComponent();
        fhirEncounter.addParticipant(participantComponent);

        EncounterProvider updatedEncounterProvider = new EncounterProvider();
        updatedEncounterProvider.setProvider(existingProvider);
        updatedEncounterProvider.setEncounterRole(encounterRole);

        when(participantTranslator.toOpenmrsType(any(), any())).thenReturn(updatedEncounterProvider);

        org.openmrs.Encounter result = translator.toOpenmrsType(existingEncounter, fhirEncounter);

        Set<EncounterProvider> resultProviders = result.getEncounterProviders();
        assertEquals(1, resultProviders.size());

        // The important assertion: verify that the existing provider instance is preserved
        EncounterProvider resultProvider = resultProviders.iterator().next();
        assertSame(existingEncounterProvider, resultProvider);

        // Verify the provider reference is the same
        assertSame(existingProvider, resultProvider.getProvider());
    }
	
	@Test
    public void shouldAddNewProviderWhenProviderDoesNotExistInExistingEncounter() {
        // Arrange
        Set<EncounterProvider> existingProviders = new LinkedHashSet<>();
        existingProviders.add(existingEncounterProvider);
        existingEncounter.setEncounterProviders(existingProviders);

        Encounter.EncounterParticipantComponent participantComponent = new Encounter.EncounterParticipantComponent();
        fhirEncounter.addParticipant(participantComponent);

        when(participantTranslator.toOpenmrsType(any(), any())).thenReturn(newEncounterProvider);

        org.openmrs.Encounter result = translator.toOpenmrsType(existingEncounter, fhirEncounter);

        Set<EncounterProvider> resultProviders = result.getEncounterProviders();
        assertEquals(2, resultProviders.size());

        boolean foundExisting = false;
        boolean foundNew = false;

        for (EncounterProvider provider : resultProviders) {
            if (provider.getProvider().getUuid().equals(existingProvider.getUuid())) {
                foundExisting = true;
                assertSame(existingEncounterProvider, provider);
            } else if (provider.getProvider().getUuid().equals(newProvider.getUuid())) {
                foundNew = true;
                assertSame(newEncounterProvider, provider);
            }
        }

        assertTrue("Existing provider should be preserved", foundExisting);
        assertTrue("New provider should be added", foundNew);
    }
	
	@Test
    public void shouldAddAllProvidersWhenNoExistingProviders() {
        // Arrange
        existingEncounter.setEncounterProviders(new LinkedHashSet<>());

        Encounter.EncounterParticipantComponent participantComponent1 = new Encounter.EncounterParticipantComponent();
        Encounter.EncounterParticipantComponent participantComponent2 = new Encounter.EncounterParticipantComponent();
        fhirEncounter.addParticipant(participantComponent1);
        fhirEncounter.addParticipant(participantComponent2);

        when(participantTranslator.toOpenmrsType(any(), eq(participantComponent1))).thenReturn(existingEncounterProvider);
        when(participantTranslator.toOpenmrsType(any(), eq(participantComponent2))).thenReturn(newEncounterProvider);

        org.openmrs.Encounter result = translator.toOpenmrsType(existingEncounter, fhirEncounter);

        Set<EncounterProvider> resultProviders = result.getEncounterProviders();
        assertEquals(2, resultProviders.size());

        boolean foundExisting = false;
        boolean foundNew = false;

        for (EncounterProvider provider : resultProviders) {
            if (provider.getProvider().getUuid().equals(existingProvider.getUuid())) {
                foundExisting = true;
                assertSame(existingEncounterProvider, provider);
            } else if (provider.getProvider().getUuid().equals(newProvider.getUuid())) {
                foundNew = true;
                assertSame(newEncounterProvider, provider);
            }
        }

        assertTrue("First provider should be added", foundExisting);
        assertTrue("Second provider should be added", foundNew);
    }
	
	@Test
    public void shouldHandleEmptyProvidersInFhirEncounter() {
        Set<EncounterProvider> existingProviders = new LinkedHashSet<>();
        existingProviders.add(existingEncounterProvider);
        existingEncounter.setEncounterProviders(existingProviders);

        org.openmrs.Encounter result = translator.toOpenmrsType(existingEncounter, fhirEncounter);

        Set<EncounterProvider> resultProviders = result.getEncounterProviders();
        assertEquals(1, resultProviders.size());

        EncounterProvider resultProvider = resultProviders.iterator().next();
        assertSame(existingEncounterProvider, resultProvider);
        assertSame(existingProvider, resultProvider.getProvider());
    }
}

package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.TestUtils;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.DiagnosticReportTranslatorImpl;

import java.io.IOException;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadResourceFromFile;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDiagnosticReportTranslatorTest {
	
	private BahmniFhirDiagnosticReportTranslator translator;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Mock
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private BahmniServiceRequestReferenceTranslator serviceRequestReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Before
	public void setup() throws NoSuchFieldException, IllegalAccessException {
		//when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
		
		DiagnosticReportTranslatorImpl openmrsTranslator = new DiagnosticReportTranslatorImpl();
		TestUtils.setPropertyOnObject(openmrsTranslator, "observationReferenceTranslator", observationReferenceTranslator);
		TestUtils.setPropertyOnObject(openmrsTranslator, "conceptTranslator", conceptTranslator);
		TestUtils.setPropertyOnObject(openmrsTranslator, "encounterReferenceTranslator", encounterReferenceTranslator);
		TestUtils.setPropertyOnObject(openmrsTranslator, "patientReferenceTranslator", patientReferenceTranslator);
		
		translator = new BahmniFhirDiagnosticReportTranslatorImpl(openmrsTranslator, serviceRequestReferenceTranslator,
		        providerReferenceTranslator);
	}
	
	@Test
	public void toFhirResource() {
	}
	
	@Test
    public void shouldTranslateToOpenMrsTypeAndNotTranslateContainedObservations() throws IOException {
        Order testOrder = new Order();
        testOrder.setUuid("0137d86f-e27b-4f3b-a701-5f3ca9a6756f");
        DiagnosticReport resource = (DiagnosticReport) loadResourceFromFile("example-diagnostic-report-with-contained-resources.json");

        when(serviceRequestReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("ServiceRequest/0137d86f-e27b-4f3b-a701-5f3ca9a6756f");
                })))
                .thenReturn(testOrder);
		Provider performer = new Provider();
		performer.setUuid("444a609f-263f-11ee-8e08-02d2d2293862");
		when(providerReferenceTranslator.toOpenmrsType(
				ArgumentMatchers.argThat(reference -> reference.getReference().equals("Practitioner/444a609f-263f-11ee-8e08-02d2d2293862"))
		)).thenReturn(performer);
        FhirDiagnosticReportExt report = translator.toOpenmrsType(resource);
        assertEquals(1, report.getOrders().size());
        assertEquals(testOrder, report.getOrders().iterator().next());
        //contained resources are not handled by the translator
        assertTrue(report.getResults().isEmpty());
        assertTrue(report.getUuid() != null);
    }
	
	@Test
    public void shouldTranslateReportWithReferenceToExistingObservation() throws IOException {
        Order testOrder = new Order();
        testOrder.setUuid("0137d86f-e27b-4f3b-a701-5f3ca9a6756f");
        DiagnosticReport resource = (DiagnosticReport) loadResourceFromFile("example-diagnostic-report-with-observation-references.json");

        when(serviceRequestReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("ServiceRequest/0137d86f-e27b-4f3b-a701-5f3ca9a6756f");
                })))
                .thenReturn(testOrder);

        Obs exampleObs = new Obs();
        exampleObs.setUuid("eb61fbd8-156b-4f19-a445-cbd13ded268a");
        exampleObs.setValueText("example-value");
        when(observationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> reference.getReference().equals("Observation/eb61fbd8-156b-4f19-a445-cbd13ded268a"))
        )).thenReturn(exampleObs);
		Provider performer = new Provider();
		performer.setUuid("444a609f-263f-11ee-8e08-02d2d2293862");
		when(providerReferenceTranslator.toOpenmrsType(
				ArgumentMatchers.argThat(reference -> reference.getReference().equals("Practitioner/444a609f-263f-11ee-8e08-02d2d2293862"))
		)).thenReturn(performer);
        FhirDiagnosticReportExt report = translator.toOpenmrsType(resource);
        assertEquals(1, report.getOrders().size());
        assertEquals(testOrder, report.getOrders().iterator().next());
        //contained resources are not parsed yet
        assertTrue(report.getUuid() != null);
        assertEquals(1, report.getResults().size());
        assertEquals("example-value", report.getResults().iterator().next().getValueText());
		assertEquals("444a609f-263f-11ee-8e08-02d2d2293862", report.getPerformers().iterator().next().getUuid());
    }
	
	@Test
	public void testToOpenmrsType() {
	}
	
}

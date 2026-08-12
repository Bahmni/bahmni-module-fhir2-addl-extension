package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Patient;

public class FhirPatientSaveAdvice extends BaseAdvice {
	
	private static final String TEMPLATE = "/openmrs/ws/rest/v1/patient/%s?v=full";
	
	public static final String TITLE = "Patient";
	
	public static final String CATEGORY = "patient";
	
	@Override
	protected boolean shouldRaiseEvent(Object returnValue) {
		Patient patient = (Patient) returnValue;
		return patient != null;
	}
	
	@Override
	protected String getUrl(Object returnValue) {
		Patient patient = (Patient) returnValue;
		return String.format(TEMPLATE, patient.getIdElement().getIdPart());
	}
	
	@Override
	protected String getTitle() {
		return TITLE;
	}
	
	@Override
	protected String getCategory() {
		return CATEGORY;
	}
}

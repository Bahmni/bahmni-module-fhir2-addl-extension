package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class FhirPatientSaveAdvice extends BaseAdvice {
	
	private static final String TEMPLATE = "/openmrs/ws/rest/v1/patient/%s?v=full";
	
	public static final String TITLE = "Patient";
	
	public static final String CATEGORY = "patient";
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object emrPatientService) throws Throwable {
		Patient patient = (Patient) returnValue;
		if (patient == null) {
			return;
		}
		String url = String.format(TEMPLATE, patient.getIdElement().getIdPart());
		raiseEvent(TITLE, CATEGORY, null, url);
	}
}

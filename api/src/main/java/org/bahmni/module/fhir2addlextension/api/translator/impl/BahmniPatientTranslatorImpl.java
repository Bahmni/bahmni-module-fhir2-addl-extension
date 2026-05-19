package org.bahmni.module.fhir2addlextension.api.translator.impl;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.Date;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.fhir2.api.translators.impl.PatientTranslatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BahmniPatientTranslatorImpl extends PatientTranslatorImpl {

	private static final Logger log = LoggerFactory.getLogger(BahmniPatientTranslatorImpl.class);

	private static final String BIRTH_TIME_EXT_URL = "http://hl7.org/fhir/StructureDefinition/patient-birthTime";

	@Autowired
	private PersonAttributeExtensionTranslator personAttributeTranslator;

	void setPersonAttributeTranslator(PersonAttributeExtensionTranslator translator) {
		this.personAttributeTranslator = translator;
	}

	@Override
	public Patient toFhirResource(org.openmrs.Patient openmrsPatient) {
		Patient patient = super.toFhirResource(openmrsPatient);

		for (PersonAttribute attr : openmrsPatient.getActiveAttributes()) {
			Extension ext = personAttributeTranslator.toFhirResource(attr);
			if (ext != null) {
				patient.addExtension(ext);
			}
		}

		Date birthtime = openmrsPatient.getBirthtime();
		if (birthtime != null && patient.hasBirthDateElement()) {
			patient.getBirthDateElement().addExtension(BIRTH_TIME_EXT_URL, new DateTimeType(birthtime));
		}

		return patient;
	}

	@Override
	public org.openmrs.Patient toOpenmrsType(org.openmrs.Patient currentPatient, Patient patient) {
		notNull(currentPatient, "The existing Openmrs Patient object should not be null");
		notNull(patient, "The Patient object should not be null");

		if (patient.hasAddress()) {
			currentPatient.getAddresses().forEach(addr -> {
				addr.setVoided(true);
				addr.setVoidReason("Replaced via FHIR update");
			});
		}

		org.openmrs.Patient openmrsPatient = super.toOpenmrsType(currentPatient, patient);

		if (patient.hasBirthDateElement()) {
			Extension birthTimeExt = patient.getBirthDateElement().getExtensionByUrl(BIRTH_TIME_EXT_URL);
			if (birthTimeExt != null && birthTimeExt.getValue() instanceof DateTimeType) {
				openmrsPatient.setBirthtime(((DateTimeType) birthTimeExt.getValue()).getValue());
			}
		}

		for (Extension ext : patient.getExtension()) {
			String url = ext.getUrl();
			if (url == null || !url.startsWith(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX)) {
				continue;
			}

			PersonAttributeType attrType = personAttributeTranslator.resolveType(url);
			if (attrType == null) {
				log.warn("Unknown person attribute extension: {}", url);
				continue;
			}

			// Void existing attribute of this type first
			for (PersonAttribute existing : openmrsPatient.getActiveAttributes()) {
				if (existing.getAttributeType().equals(attrType)) {
					existing.setVoided(true);
					existing.setVoidReason("Updated via FHIR");
				}
			}

			Type value = ext.getValue();
			if (value != null && value.primitiveValue() != null) {
				PersonAttribute attr = new PersonAttribute();
				attr.setAttributeType(attrType);
				attr.setValue(value.primitiveValue());
				openmrsPatient.addAttribute(attr);
			}
		}

		return openmrsPatient;
	}
}

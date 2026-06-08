package org.bahmni.module.fhir2addlextension.api.translator.impl;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
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

	static final String BIRTH_TIME_EXT_URL = "http://hl7.org/fhir/StructureDefinition/patient-birthTime";

	static final String DATE_CREATED_EXT_URL = BahmniFhirConstants.FHIR_EXT_PATIENT_DATE_CREATED;

	@Autowired
	private org.bahmni.module.fhir2addlextension.api.translator.PersonAttributeExtensionTranslator personAttributeTranslator;

	void setPersonAttributeTranslator(org.bahmni.module.fhir2addlextension.api.translator.PersonAttributeExtensionTranslator translator) {
		this.personAttributeTranslator = translator;
	}

	@Override
	public Patient toFhirResource(@Nonnull org.openmrs.Patient openmrsPatient) {
		Patient patient = super.toFhirResource(openmrsPatient);
		addPersonAttributeExtensions(patient, openmrsPatient);
		addBirthTimeExtension(patient, openmrsPatient);
		addDateCreatedExtension(patient, openmrsPatient);
		addPhotoUrl(patient, openmrsPatient);
		return patient;
	}

	@Override
	public org.openmrs.Patient toOpenmrsType(@Nonnull org.openmrs.Patient currentPatient, @Nonnull Patient patient) {
		voidExistingAddresses(currentPatient, patient);
		org.openmrs.Patient openmrsPatient = super.toOpenmrsType(currentPatient, patient);
		setPreferredNameFlag(openmrsPatient);
		readBirthTime(openmrsPatient, patient);
		processPersonAttributeExtensions(openmrsPatient, patient);
		processPhoto(openmrsPatient, patient);
		return openmrsPatient;
	}

	void addPersonAttributeExtensions(Patient fhirPatient, org.openmrs.Patient openmrsPatient) {
		for (PersonAttribute attr : openmrsPatient.getActiveAttributes()) {
			Extension ext = personAttributeTranslator.toFhirResource(attr);
			if (ext != null) {
				fhirPatient.addExtension(ext);
			}
		}
	}

	void addBirthTimeExtension(Patient fhirPatient, org.openmrs.Patient openmrsPatient) {
		Date birthtime = openmrsPatient.getBirthtime();
		if (birthtime != null && fhirPatient.hasBirthDateElement()) {
			fhirPatient.getBirthDateElement().addExtension(BIRTH_TIME_EXT_URL, new DateTimeType(birthtime));
		}
	}

	void addDateCreatedExtension(Patient fhirPatient, org.openmrs.Patient openmrsPatient) {
		Date dateCreated = openmrsPatient.getDateCreated();
		if (dateCreated != null) {
			fhirPatient.addExtension(DATE_CREATED_EXT_URL, new DateTimeType(dateCreated));
		}
	}

	void readBirthTime(org.openmrs.Patient openmrsPatient, Patient fhirPatient) {
		if (fhirPatient.hasBirthDateElement()) {
			Extension birthTimeExt = fhirPatient.getBirthDateElement().getExtensionByUrl(BIRTH_TIME_EXT_URL);
			if (birthTimeExt != null && birthTimeExt.getValue() instanceof DateTimeType) {
				openmrsPatient.setBirthtime(((DateTimeType) birthTimeExt.getValue()).getValue());
			}
		}
	}

	void voidExistingAddresses(org.openmrs.Patient currentPatient, Patient fhirPatient) {
		if (fhirPatient.hasAddress()) {
			currentPatient.getAddresses().forEach(addr -> {
				addr.setVoided(true);
				addr.setVoidReason("Replaced via FHIR update");
				addr.setVoidedBy(Context.getAuthenticatedUser());
				addr.setDateVoided(new Date());
			});
		}
	}

	void setPreferredNameFlag(org.openmrs.Patient openmrsPatient) {
		PersonName preferredName = openmrsPatient.getPersonName();
		if (preferredName != null && !preferredName.getPreferred()) {
			preferredName.setPreferred(true);
		}
	}

	void processPersonAttributeExtensions(org.openmrs.Patient openmrsPatient, Patient fhirPatient) {
		Map<String, PersonAttributeType> slugToTypeMap = personAttributeTranslator.buildSlugToTypeMap();

		for (Extension ext : fhirPatient.getExtension()) {
			String url = ext.getUrl();
			if (url == null || !url.startsWith(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX)) {
				continue;
			}

			PersonAttributeType attrType = personAttributeTranslator.resolveType(url, slugToTypeMap);
			if (attrType == null) {
				log.warn("Unknown person attribute extension: {}", url);
				continue;
			}

			for (PersonAttribute existing : openmrsPatient.getActiveAttributes()) {
				if (existing.getAttributeType().equals(attrType)) {
					existing.setVoided(true);
					existing.setVoidReason("Updated via FHIR");
					existing.setVoidedBy(Context.getAuthenticatedUser());
					existing.setDateVoided(new Date());
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
	}
	
	void addPhotoUrl(Patient fhirPatient, org.openmrs.Patient openmrsPatient) {
		String uuid = openmrsPatient.getUuid();
		if (uuid == null || !hasPhotoOnDisk(openmrsPatient)) {
			return;
		}
		Attachment attachment = new Attachment();
		attachment.setContentType(BahmniFhirConstants.PATIENT_PHOTO_CONTENT_TYPE);
		attachment.setUrl(String.format(BahmniFhirConstants.PATIENT_PHOTO_URL_TEMPLATE, uuid));
		fhirPatient.setPhoto(Collections.singletonList(attachment));
	}

	private boolean hasPhotoOnDisk(org.openmrs.Patient openmrsPatient) {
		try {
			File imageFile = getImageFile(openmrsPatient);
			return imageFile != null && imageFile.exists();
		} catch (Exception e) {
			log.warn("Could not check photo for patient {}: {}", openmrsPatient.getUuid(), e.getMessage());
			return false;
		}
	}

	void processPhoto(org.openmrs.Patient openmrsPatient, Patient fhirPatient) {
		List<Attachment> photos = fhirPatient.getPhoto();
		if (photos == null) {
			return;
		}
		if (photos.isEmpty()) {
			deletePhoto(openmrsPatient);
			return;
		}
		String base64Data = photos.get(0).getDataElement().getValueAsString();
		if (base64Data == null || base64Data.isEmpty()) {
			return;
		}
		savePhoto(openmrsPatient, base64Data);
	}

	private void savePhoto(org.openmrs.Patient openmrsPatient, String base64Data) {
		try {
			Object imageService = getEmrImageService();
			Class<?> personImageClass = Context.loadClass("org.openmrs.module.emrapi.person.image.PersonImage");
			Object personImage = personImageClass.getConstructor().newInstance();
			personImageClass.getMethod("setPerson", org.openmrs.Person.class).invoke(personImage, openmrsPatient);
			personImageClass.getMethod("setBase64EncodedImage", String.class).invoke(personImage, base64Data);
			imageService.getClass().getMethod("savePersonImage", personImageClass).invoke(imageService, personImage);
		} catch (Exception e) {
			log.error("Failed to save patient photo for {}", openmrsPatient.getUuid(), e);
		}
	}

	void deletePhoto(org.openmrs.Patient openmrsPatient) {
		try {
			File imageFile = getImageFile(openmrsPatient);
			if (imageFile != null && imageFile.exists() && imageFile.delete()) {
				log.info("Deleted patient photo for {}", openmrsPatient.getUuid());
			}
		} catch (Exception e) {
			log.error("Failed to delete patient photo for {}", openmrsPatient.getUuid(), e);
		}
	}

	private Object getEmrImageService() throws Exception {
		Class<?> serviceClass = Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService");
		return Context.getRegisteredComponent("emrPersonImageService", serviceClass);
	}

	private File getImageFile(org.openmrs.Patient openmrsPatient) throws Exception {
		Object imageService = getEmrImageService();
		Object personImage = imageService.getClass()
				.getMethod("getCurrentPersonImage", org.openmrs.Person.class)
				.invoke(imageService, openmrsPatient);
		return (File) personImage.getClass().getMethod("getSavedImage").invoke(personImage);
	}
}

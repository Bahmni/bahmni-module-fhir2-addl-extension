package org.bahmni.module.fhir2addlextension.api.service;

import java.io.File;

import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BahmniPatientPhotoService {

	private static final Logger log = LoggerFactory.getLogger(BahmniPatientPhotoService.class);

	public boolean hasPhoto(org.openmrs.Patient patient) {
		try {
			File imageFile = getImageFile(patient);
			return imageFile != null && imageFile.exists();
		} catch (Exception e) {
			log.warn("Could not check photo for patient {}: {}", patient.getUuid(), e.getMessage());
			return false;
		}
	}

	public void savePhoto(org.openmrs.Patient patient, String base64Data) {
		try {
			Object imageService = getEmrImageService();
			Class<?> personImageClass = Context.loadClass("org.openmrs.module.emrapi.person.image.PersonImage");
			Object personImage = personImageClass.getConstructor().newInstance();
			personImageClass.getMethod("setPerson", org.openmrs.Person.class).invoke(personImage, patient);
			personImageClass.getMethod("setBase64EncodedImage", String.class).invoke(personImage, base64Data);
			imageService.getClass().getMethod("savePersonImage", personImageClass).invoke(imageService, personImage);
		} catch (Exception e) {
			log.error("Failed to save patient photo for {}", patient.getUuid(), e);
		}
	}

	public void deletePhoto(org.openmrs.Patient patient) {
		try {
			File imageFile = getImageFile(patient);
			if (imageFile != null && imageFile.exists()) {
				if (imageFile.delete()) {
					log.info("Deleted patient photo for {}", patient.getUuid());
				} else {
					log.warn("Failed to delete patient photo file: {}", imageFile.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			log.error("Failed to delete patient photo for {}", patient.getUuid(), e);
		}
	}

	public File getImageFile(org.openmrs.Patient patient) throws Exception {
		Object imageService = getEmrImageService();
		Object personImage = imageService.getClass()
				.getMethod("getCurrentPersonImage", org.openmrs.Person.class)
				.invoke(imageService, patient);
		return (File) personImage.getClass().getMethod("getSavedImage").invoke(personImage);
	}

	private Object getEmrImageService() throws Exception {
		Class<?> serviceClass = Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService");
		return Context.getRegisteredComponent("emrPersonImageService", serviceClass);
	}
}

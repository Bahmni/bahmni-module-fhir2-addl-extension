package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.bahmni.module.fhir2addlextension.api.service.BahmniPatientPhotoService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@R4Provider
public class BahmniPatientPhotoProvider implements IResourceProvider {

	private static final Logger log = LoggerFactory.getLogger(BahmniPatientPhotoProvider.class);

	@Autowired
	private BahmniPatientPhotoService photoService;

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}

	@Operation(name = "$photo", type = Patient.class, idempotent = true, manualResponse = true)
	public void getPhoto(@IdParam IdType patientId, HttpServletResponse response) throws IOException {
		if (!Context.getUserContext().hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)) {
			throw new ForbiddenOperationException("User does not have privilege: " + PrivilegeConstants.GET_PATIENT_PHOTO);
		}

		org.openmrs.Patient patient = Context.getPatientService().getPatientByUuid(patientId.getIdPart());
		if (patient == null) {
			throw new ResourceNotFoundException("Patient not found: " + patientId.getIdPart());
		}

		try {
			File imageFile = photoService.getImageFile(patient);
			if (imageFile == null || !imageFile.exists()) {
				throw new ResourceNotFoundException("Photo not found for patient: " + patientId.getIdPart());
			}

			response.setContentType("image/jpeg");
			response.setStatus(HttpServletResponse.SC_OK);
			Files.copy(imageFile.toPath(), response.getOutputStream());
		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to retrieve patient photo for {}", patientId.getIdPart(), e);
			throw new InternalErrorException("Failed to retrieve patient photo", e);
		}
	}
}

package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@R4Provider
public class BahmniPatientPhotoProvider implements IResourceProvider {

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}

	@Operation(name = "$photo", type = Patient.class, idempotent = true, manualResponse = true)
	public void getPhoto(@IdParam IdType patientId, HttpServletResponse response) throws IOException {
		if (!Context.getUserContext().hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		try {
			Class<?> serviceClass = Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService");
			Object imageService = Context.getRegisteredComponent("emrPersonImageService", serviceClass);

			Person person = Context.getPersonService().getPersonByUuid(patientId.getIdPart());
			if (person == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			Object personImage = serviceClass.getMethod("getCurrentPersonImage", Person.class).invoke(imageService, person);
			File imageFile = (File) personImage.getClass().getMethod("getSavedImage").invoke(personImage);

			if (imageFile == null || !imageFile.exists()) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			response.setContentType("image/jpeg");
			response.setStatus(HttpServletResponse.SC_OK);
			Files.copy(imageFile.toPath(), response.getOutputStream());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}

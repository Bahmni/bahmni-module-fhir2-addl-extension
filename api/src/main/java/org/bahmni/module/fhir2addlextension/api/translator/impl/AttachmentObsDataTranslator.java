package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.ComplexObsDataTranslator;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.Obs;

import java.util.Arrays;
import java.util.List;

import static org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE;

class AttachmentObsDataTranslator implements ComplexObsDataTranslator {
	
	private static final List<String> ATTACHMENT_CLASSES = Arrays.asList("Image", "Video");
	
	@Override
	public boolean supports(Concept concept) {
		return ATTACHMENT_CLASSES.contains(concept.getConceptClass().getName());
	}
	
	@Override
	public Extension toFhirResource(Obs obs) {
		Extension extension = new Extension();
		extension.setUrl(FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE);
		extension.setValue(createAttachment(obs.getValueComplex()));
		//			return Optional.of(new AbstractMap.SimpleImmutableEntry<>(
		//					FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE, createAttachment(obs.getValueComplex())));
		return extension;
	}
	
	@Override
	public String toOpenmrsType(Observation observation) {
		Extension attachmentExt = observation.getExtensionByUrl(FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE);
		if (attachmentExt == null) {
			return null;
		}
		Type extValue = attachmentExt.getValue();
		if (extValue instanceof Attachment) {
			Attachment attachment = (Attachment) extValue;
			String url = attachment.getUrl();
			String title = attachment.getTitle();
			if (title != null && !title.isEmpty() && url != null) {
				return title + " | " + url;
			}
			return url;
		} else {
			return null;
		}
	}
	
	private Type createAttachment(String valueComplex) {
		Attachment attachment = new Attachment();
		if (valueComplex != null && valueComplex.contains(" | ")) {
			String[] parts = valueComplex.split(" \\| ", 2);
			attachment.setTitle(parts[0]);
			attachment.setUrl(parts[1]);
		} else {
			attachment.setUrl(valueComplex);
		}
		return attachment;
	}
}

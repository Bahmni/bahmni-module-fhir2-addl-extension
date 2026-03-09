package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.ComplexObsDataTranslator;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.Obs;

import java.util.Arrays;
import java.util.List;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE;

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
			return ((Attachment) extValue).getUrl();
		} else {
			return null;
		}
	}
	
	private Type createAttachment(String attachmentUrl) {
		//TODO set content-type
		Attachment attachment = new Attachment();
		attachment.setUrl(attachmentUrl);
		return attachment;
	}
}

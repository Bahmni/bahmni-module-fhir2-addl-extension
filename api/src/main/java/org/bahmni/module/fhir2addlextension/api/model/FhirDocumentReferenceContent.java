package org.bahmni.module.fhir2addlextension.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmrs.BaseOpenmrsData;

@Data
@NoArgsConstructor
public class FhirDocumentReferenceContent extends BaseOpenmrsData {
	
	private Integer documentReferenceContentId;
	
	private FhirDocumentReference documentReference;
	
	private String contentFormat;
	
	private String contentType;
	
	private String contentUrl;
	
	@Override
	public Integer getId() {
		return getDocumentReferenceContentId();
	}
	
	@Override
	public void setId(Integer id) {
		setDocumentReferenceContentId(id);
	}
}

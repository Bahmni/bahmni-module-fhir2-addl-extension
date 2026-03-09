package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.BaseOpenmrsData;

@Getter
@Setter
@NoArgsConstructor
public class Attachment extends BaseOpenmrsData {
	
	private Integer attachmentId;
	
	private String title;
	
	private String contentType;
	
	private String contentUrl;
	
	@Override
	public Integer getId() {
		return attachmentId;
	}
	
	@Override
	public void setId(Integer value) {
		this.attachmentId = value;
	}
}

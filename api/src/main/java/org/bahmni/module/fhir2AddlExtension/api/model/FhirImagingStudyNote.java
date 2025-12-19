package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Provider;

import java.util.Date;

@Data
@NoArgsConstructor
public class FhirImagingStudyNote extends BaseOpenmrsData {
	
	private Integer imagingStudyNoteId;
	
	private FhirImagingStudy imagingStudy;
	
	private Provider performer;
	
	private String note;
	
	private Date dateCreated;
	
	@Override
	public Integer getId() {
		return getImagingStudyNoteId();
	}
	
	@Override
	public void setId(Integer id) {
		setImagingStudyNoteId(id);
	}
}

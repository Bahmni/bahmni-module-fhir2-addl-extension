package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Provider;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = { "imagingStudy" })
@EqualsAndHashCode(exclude = { "imagingStudy" }, callSuper = false)
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

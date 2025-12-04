package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.Date;

@Data
@NoArgsConstructor
public class FhirImagingStudy extends BaseOpenmrsData {
	
	private Integer imagingStudyId;
	
	private String studyInstanceUuid;
	
	private Patient subject;
	
	private Order order;
	
	private Location location;
	
	private Provider performer;
	
	private String description;
	
	private String notes;
	
	private Encounter encounter;
	
	private Date dateStarted;
	
	private FhirImagingStudyStatus status;
	
	@Override
	public Integer getId() {
		return imagingStudyId;
	}
	
	@Override
	public void setId(Integer id) {
		setImagingStudyId(id);
	}
	
	public enum FhirImagingStudyStatus {
		REGISTERED, AVAILABLE, CANCELLED, UNKNOWN, INACTIVE, ENTEREDINERROR
	}
}

package org.bahmni.module.fhir2addlextension.api.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"notes", "results"})
@EqualsAndHashCode(exclude = {"notes", "results"}, callSuper = false)
public class FhirImagingStudy extends BaseOpenmrsData {
	
	private Integer imagingStudyId;
	
	private String studyInstanceUuid;
	
	private Patient subject;
	
	private Order order;
	
	private Location location;
	
	private Provider performer;
	
	private String description;

	private Encounter encounter;
	
	private Date dateStarted;

    private Date dateCompleted;

    private Set<FhirImagingStudyNote> notes = new HashSet<>();
	
	private Set<Obs> results = new HashSet<>();
	
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

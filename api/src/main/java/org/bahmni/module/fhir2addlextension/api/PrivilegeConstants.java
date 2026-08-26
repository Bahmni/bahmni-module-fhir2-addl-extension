package org.bahmni.module.fhir2addlextension.api;

public class PrivilegeConstants {
	
	private PrivilegeConstants() {
	}
	
	public static final String GET_TASKS = "Get Tasks";
	
	public static final String EDIT_TASKS = "Edit Tasks";
	
	public static final String ADD_TASKS = "Add Tasks";
	
	public static final String GET_IMAGING_STUDY = "Get Imaging Study";
	
	public static final String EDIT_IMAGING_STUDY = "Edit Imaging Study";
	
	public static final String CREATE_IMAGING_STUDY = "Add Imaging Study";
	
	public static final String DELETE_IMAGING_STUDY = "Delete Imaging Study";
	
	public static final String GET_APPOINTMENTS = "Get Appointments";
	
	// Episodes (Bahmni-specific, not in org.openmrs.util.PrivilegeConstants)
	public static final String GET_EPISODES = "Get Episodes";
	
	public static final String ADD_EPISODES = "Add Episodes";
	
	public static final String EDIT_EPISODES = "Edit Episodes";
	
	public static final String DELETE_EPISODES = "Delete Episodes";
	
	// DocumentReference (Bahmni FHIR-specific)
	public static final String GET_DOCUMENT_REFERENCE = "Get DocumentReference";
	
	public static final String ADD_DOCUMENT_REFERENCE = "Add DocumentReference";
	
	public static final String EDIT_DOCUMENT_REFERENCE = "Edit DocumentReference";
	
	public static final String DELETE_DOCUMENT_REFERENCE = "Delete DocumentReference";
	
	// Diagnoses — ADD_DIAGNOSES is not in org.openmrs.util.PrivilegeConstants;
	// use org.openmrs.util.PrivilegeConstants for GET/EDIT/DELETE_DIAGNOSES
	public static final String ADD_DIAGNOSES = "Add Diagnoses";
	
	// Observations — DELETE_OBSERVATIONS is not in org.openmrs.util.PrivilegeConstants;
	// use org.openmrs.util.PrivilegeConstants GET_OBS/ADD_OBS/EDIT_OBS for read/write
	public static final String DELETE_OBSERVATIONS = "Delete Observations";
	
	// Conditions — ADD_CONDITIONS is not in org.openmrs.util.PrivilegeConstants;
	// use org.openmrs.util.PrivilegeConstants for GET/EDIT/DELETE_CONDITIONS
	public static final String ADD_CONDITIONS = "Add Conditions";
	
	// DiagnosticReport (Bahmni FHIR-specific)
	public static final String GET_DIAGNOSTIC_REPORT = "Get Diagnostic Report";
	
	public static final String ADD_DIAGNOSTIC_REPORT = "Add Diagnostic Report";
	
	public static final String EDIT_DIAGNOSTIC_REPORT = "Edit Diagnostic Report";
	
	public static final String DELETE_DIAGNOSTIC_REPORT = "Delete Diagnostic Report";
}

package org.bahmni.module.fhir2addlextension.api.model;

import lombok.Value;

@Value
public class DispositionInfo {
	
	String code;
	
	String label;
	
	DispositionConfig.Rule rule;
}

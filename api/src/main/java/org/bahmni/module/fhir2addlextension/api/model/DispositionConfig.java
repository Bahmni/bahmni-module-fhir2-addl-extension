package org.bahmni.module.fhir2addlextension.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class DispositionConfig {
	
	private final String adtOrderTypeName;
	
	private final Map<String, Rule> rulesByCode;
	
	public Rule getRule(String code) {
		return rulesByCode.get(code);
	}
	
	@Data
	@NoArgsConstructor
	public static class Rule {
		
		private boolean duplicateChecked;
		
		private Set<String> closedBy = new HashSet<>();
		
		private Set<String> fulfilledByEncounterTypes = new HashSet<>();
		
		private boolean fulfilledByVoiding;
	}
}

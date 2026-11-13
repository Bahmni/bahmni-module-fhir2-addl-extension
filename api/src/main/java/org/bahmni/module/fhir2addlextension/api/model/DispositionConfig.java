package org.bahmni.module.fhir2addlextension.api.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class DispositionConfig {
	
	private final String adtOrderTypeName;
	
	private final Map<String, Rule> rulesByCode;
	
	public DispositionConfig(String adtOrderTypeName, Map<String, Rule> rulesByCode) {
		this.adtOrderTypeName = adtOrderTypeName;
		this.rulesByCode = rulesByCode;
	}
	
	public Rule getRule(String code) {
		return rulesByCode.get(code);
	}
	
	@Getter
	public static class Rule {
		
		@Setter
		private boolean duplicateChecked;
		
		private final Set<String> closedBy = new HashSet<>();
		
		private final Set<String> fulfilledByEncounterTypes = new HashSet<>();
		
		@Setter
		private boolean fulfilledByVoiding;
	}
}

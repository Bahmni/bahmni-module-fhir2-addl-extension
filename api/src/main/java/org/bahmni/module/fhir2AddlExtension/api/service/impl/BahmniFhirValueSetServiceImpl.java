package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirValueSetService;
import org.hl7.fhir.r4.model.ValueSet;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.api.impl.FhirValueSetServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Extended FHIR ValueSet service implementation with support for hierarchical expansion
 */
@Component
@Primary
public class BahmniFhirValueSetServiceImpl extends FhirValueSetServiceImpl implements BahmniFhirValueSetService {
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private ConceptService conceptService;
	
	@Override
	public ValueSet expandedValueSet(@Nonnull String valueSetId, @Nullable Boolean includeHierarchy,
	        @Nullable String filter, @Nullable Integer count, @Nullable Integer offset) {
		
		// Get the base ValueSet
		ValueSet valueSet = this.get(valueSetId);
		if (valueSet == null) {
			throw new RuntimeException("ValueSet not found with ID: " + valueSetId);
		}
		
		// Get the concept from OpenMRS
		Concept concept = conceptService.getConceptByUuid(valueSetId);
		if (concept == null) {
			throw new RuntimeException("Concept not found with UUID: " + valueSetId);
		}
		
		// Create expansion
		ValueSet.ValueSetExpansionComponent expansion = createExpansion(concept, includeHierarchy, filter, count, offset);
		valueSet.setExpansion(expansion);
		
		return valueSet;
	}
	
	/**
	 * Creates the expansion component for a concept
	 */
	private ValueSet.ValueSetExpansionComponent createExpansion(Concept concept, Boolean includeHierarchy,
			String filter, Integer count, Integer offset) {
		
		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		List<ValueSet.ValueSetExpansionContainsComponent> contains = new ArrayList<>();
		
		boolean hierarchical = Boolean.TRUE.equals(includeHierarchy);
		
		if (hierarchical) {
			// Build hierarchical structure
			addConceptHierarchically(concept, contains, filter, new HashSet<>());
		} else {
			// Build flat structure
			addConceptFlat(concept, contains, filter, new HashSet<>());
		}
		
		// Apply filtering
		if (StringUtils.isNotBlank(filter)) {
			contains = contains.stream()
				.filter(c -> StringUtils.containsIgnoreCase(c.getDisplay(), filter) || 
							StringUtils.containsIgnoreCase(c.getCode(), filter))
				.collect(Collectors.toList());
		}
		
		// Apply pagination
		int startIndex = offset != null ? offset : 0;
		int endIndex = count != null ? Math.min(startIndex + count, contains.size()) : contains.size();
		
		if (startIndex < contains.size()) {
			contains = contains.subList(startIndex, endIndex);
		} else {
			contains = Collections.emptyList();
		}
		
		expansion.setContains(contains);
		expansion.setTotal(contains.size());
		expansion.setTimestamp(new Date());
		
		return expansion;
	}
	
	/**
	 * Adds concepts hierarchically (parent-child relationships preserved)
	 */
	private void addConceptHierarchically(Concept concept, List<ValueSet.ValueSetExpansionContainsComponent> contains,
			String filter, Set<String> processed) {
		
		if (processed.contains(concept.getUuid())) {
			return; // Avoid infinite loops
		}
		processed.add(concept.getUuid());
		
		ValueSet.ValueSetExpansionContainsComponent parentComponent = createContainsComponent(concept);
		List<ValueSet.ValueSetExpansionContainsComponent> childContains = new ArrayList<>();
		
		// Add child concepts from setMembers
		Collection<Concept> setMembers = concept.getSetMembers();
		if (setMembers != null && !setMembers.isEmpty()) {
			for (Concept memberConcept : setMembers) {
				if (memberConcept != null) {
					addConceptHierarchically(memberConcept, childContains, filter, processed);
				}
			}
		}
		
		parentComponent.setContains(childContains);
		contains.add(parentComponent);
	}
	
	/**
	 * Adds concepts in a flat structure (no hierarchical nesting)
	 */
	private void addConceptFlat(Concept concept, List<ValueSet.ValueSetExpansionContainsComponent> contains, String filter,
	        Set<String> processed) {
		
		if (processed.contains(concept.getUuid())) {
			return; // Avoid infinite loops
		}
		processed.add(concept.getUuid());
		
		// Add the current concept
		ValueSet.ValueSetExpansionContainsComponent component = createContainsComponent(concept);
		contains.add(component);
		
		// Add child concepts from setMembers (flattened)
		Collection<Concept> setMembers = concept.getSetMembers();
		if (setMembers != null) {
			for (Concept memberConcept : setMembers) {
				if (memberConcept != null) {
					addConceptFlat(memberConcept, contains, filter, processed);
				}
			}
		}
	}
	
	/**
	 * Creates a contains component for a concept
	 */
	private ValueSet.ValueSetExpansionContainsComponent createContainsComponent(Concept concept) {
		ValueSet.ValueSetExpansionContainsComponent component = new ValueSet.ValueSetExpansionContainsComponent();
		
		// Use concept UUID as code for now (could be improved to use proper coding system)
		component.setCode(concept.getUuid());
		component.setDisplay(concept.getDisplayString());
		boolean isInactive = concept.isRetired() || concept.getConceptClass() == null
		        || concept.getConceptClass().isRetired();
		
		if (isInactive) {
			component.setInactive(true);
		}
		
		return component;
	}
}

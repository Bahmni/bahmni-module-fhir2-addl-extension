package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirValueSetService;
import org.hl7.fhir.r4.model.ValueSet;
import org.openmrs.Concept;
import org.openmrs.ConceptSearchResult;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.api.impl.FhirValueSetServiceImpl;
import org.openmrs.util.LocaleUtility;
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
	public ValueSet expandedValueSet(@Nonnull String valueSetId) {
		
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
		
		// Create hierarchical expansion
		ValueSet.ValueSetExpansionComponent expansion = createExpansion(concept);
		valueSet.setExpansion(expansion);
		
		return valueSet;
	}
	
	@Override
    public ValueSet filterAndExpandValueSet(@Nonnull String filter) {
        List<Locale> locales = new ArrayList<>(LocaleUtility.getLocalesInOrder());
        List<ConceptSearchResult> searchResult = conceptService.getConcepts(filter, locales, false, null, null, null, null, null, 0, null);
        List<Concept> conceptsByName = searchResult.stream().map(ConceptSearchResult::getConcept).collect(Collectors.toList());
        if (conceptsByName.isEmpty()) {
            throw new InvalidRequestException("No concept found with name: " + filter);
        } else if (conceptsByName.size() > 1) {
            throw new InvalidRequestException("Multiple concepts found with name: " + filter);
        }

        Concept filteredConcept = conceptsByName.get(0);
        ValueSet valueSet = getTranslator().toFhirResource(filteredConcept);
        ValueSet.ValueSetExpansionComponent expansion = createExpansion(filteredConcept);
        valueSet.setExpansion(expansion);

        return valueSet;
    }
	
	/**
	 * Creates the expansion component for a concept with hierarchical structure Only includes
	 * setMembers of the root concept, not the root concept itself
	 */
	private ValueSet.ValueSetExpansionComponent createExpansion(Concept concept) {

	       ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
	       List<ValueSet.ValueSetExpansionContainsComponent> contains = new ArrayList<>();

	       // Add only the setMembers of the root concept, not the root concept itself
	       Collection<Concept> setMembers = concept.getSetMembers();
	       if (setMembers != null && !setMembers.isEmpty()) {
	           for (Concept memberConcept : setMembers) {
	               if (memberConcept != null) {
	                   // Create a new path set for each top-level branch to track ancestors
	                   Set<String> currentPath = new HashSet<>();
	                   currentPath.add(concept.getUuid()); // Add root to path
	                   addConceptHierarchically(memberConcept, contains, currentPath);
	               }
	           }
	       }

	       expansion.setContains(contains);
	       expansion.setTotal(contains.size());
	       expansion.setTimestamp(new Date());

	       return expansion;
	   }
	
	/**
	 * Adds concepts hierarchically (parent-child relationships preserved)
	 * 
	 * @param concept The concept to add
	 * @param contains The list to add the concept to
	 * @param currentPath Set of UUIDs in the current path to prevent cycles within a branch
	 */
	private void addConceptHierarchically(Concept concept, List<ValueSet.ValueSetExpansionContainsComponent> contains,
	                                         Set<String> currentPath) {

	       // Check if this concept is already in the current path (would create a cycle)
	       if (currentPath.contains(concept.getUuid())) {
	           return; // Avoid infinite loops within this branch
	       }

	       ValueSet.ValueSetExpansionContainsComponent parentComponent = createContainsComponent(concept);
	       List<ValueSet.ValueSetExpansionContainsComponent> childContains = new ArrayList<>();

	       // Process child concepts from setMembers
	       Collection<Concept> setMembers = concept.getSetMembers();
	       if (setMembers != null && !setMembers.isEmpty()) {
	           // Add current concept to path before processing children
	           Set<String> childPath = new HashSet<>(currentPath);
	           childPath.add(concept.getUuid());
	           
	           for (Concept memberConcept : setMembers) {
	               if (memberConcept != null) {
	                   addConceptHierarchically(memberConcept, childContains, childPath);
	               }
	           }
	       }

	       parentComponent.setContains(childContains);
	       contains.add(parentComponent);
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

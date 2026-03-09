package org.bahmni.module.fhir2AddlExtension.api.helper;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openmrs.module.fhir2.FhirConstants;

import java.util.*;

public class ConsultationBundleEntriesHelper {
	
	public static List<Bundle.BundleEntryComponent> orderEntriesByReference(List<Bundle.BundleEntryComponent> entries) {
        List<Bundle.BundleEntryComponent> orderedEntries = new ArrayList<>();

        // If there are no entries or only one entry, return the entries as is
        if (entries == null || entries.size() <= 1) {
            return entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        }

        // Create a map of resource ID to bundle entry for quick lookup
        Map<String, Bundle.BundleEntryComponent> resourceUrlToEntry = new HashMap<>();

        // Create a map of bundle entry to the set of entries it references
        Map<Bundle.BundleEntryComponent, Set<Bundle.BundleEntryComponent>> entryDependencies = new HashMap<>();

        // Initialize the maps
        for (Bundle.BundleEntryComponent entry : entries) {
            if (entry.hasResource() && entry.hasFullUrl()) {
                resourceUrlToEntry.put(entry.getFullUrl(), entry);
                entryDependencies.put(entry, new HashSet<>());
            }
        }

        // Build the dependency graph
        for (Bundle.BundleEntryComponent entry : entries) {
            if (entry.hasResource()) {
                Resource resource = entry.getResource();
                Set<Reference> references = extractReferences(resource);

                for (Reference reference : references) {
                    if (reference.hasReference()) {
                        String referenceValue = reference.getReference();

                        // Check if this reference points to another resource in the bundle
                        Bundle.BundleEntryComponent referencedEntry = resourceUrlToEntry.get(referenceValue);

                        if (referencedEntry != null && !referencedEntry.equals(entry)) {
                            // Add the dependency
                            entryDependencies.get(entry).add(referencedEntry);
                        }
                    }
                }
            }
        }

        // Set of entries that have been processed
        Set<Bundle.BundleEntryComponent> processedEntries = new HashSet<>();

        // Process entries in topological order
        while (processedEntries.size() < entries.size()) {
            boolean progress = false;

            for (Bundle.BundleEntryComponent entry : entries) {
                // Skip entries that have already been processed
                if (processedEntries.contains(entry)) {
                    continue;
                }

                // Check if all dependencies have been processed
                Set<Bundle.BundleEntryComponent> dependencies = entryDependencies.get(entry);
                if (dependencies == null || dependencies.isEmpty() || processedEntries.containsAll(dependencies)) {
                    // All dependencies have been processed, so we can process this entry
                    orderedEntries.add(entry);
                    processedEntries.add(entry);
                    progress = true;
                }
            }

            // If we didn't make any progress in this iteration, there might be a circular dependency
            if (!progress) {
                // Add remaining entries in their original order to avoid an infinite loop
                for (Bundle.BundleEntryComponent entry : entries) {
                    if (!processedEntries.contains(entry)) {
                        orderedEntries.add(entry);
                        processedEntries.add(entry);
                    }
                }
            }
        }

        return orderedEntries;
    }
	
	public static Bundle.BundleEntryComponent resolveReferences(Bundle.BundleEntryComponent entry,
	        Map<String, Bundle.BundleEntryComponent> processedEntries) {
		Resource resource = entry.getResource();
		ResourceType resourceType = resource.getResourceType();
		switch (resourceType) {
			case Condition:
				org.hl7.fhir.r4.model.Condition condition = (org.hl7.fhir.r4.model.Condition) resource;
				if (condition.hasEncounter()) {
					String placeholderReferenceUrl = condition.getEncounter().getReference();
					condition.setEncounter(createEncounterReference(getIdForPlaceHolderReference(placeholderReferenceUrl,
					    processedEntries)));
					entry.setResource(condition);
				}
				break;
			case AllergyIntolerance:
				org.hl7.fhir.r4.model.AllergyIntolerance allergyIntolerance = (org.hl7.fhir.r4.model.AllergyIntolerance) resource;
				if (allergyIntolerance.hasEncounter()) {
					String placeholderReferenceUrl = allergyIntolerance.getEncounter().getReference();
					allergyIntolerance.setEncounter(createEncounterReference(getIdForPlaceHolderReference(
					    placeholderReferenceUrl, processedEntries)));
					entry.setResource(allergyIntolerance);
				}
				break;
			case ServiceRequest:
				org.hl7.fhir.r4.model.ServiceRequest service = (org.hl7.fhir.r4.model.ServiceRequest) resource;
				if (service.hasEncounter()) {
					String placeholderReferenceUrl = service.getEncounter().getReference();
					service.setEncounter(createEncounterReference(getIdForPlaceHolderReference(placeholderReferenceUrl,
					    processedEntries)));
					entry.setResource(service);
				}
				break;
			case MedicationRequest:
				org.hl7.fhir.r4.model.MedicationRequest medicationRequest = (org.hl7.fhir.r4.model.MedicationRequest) resource;
				if (medicationRequest.hasEncounter()) {
					String placeholderReferenceUrl = medicationRequest.getEncounter().getReference();
					medicationRequest.setEncounter(createEncounterReference(getIdForPlaceHolderReference(
					    placeholderReferenceUrl, processedEntries)));
					entry.setResource(medicationRequest);
				}
				break;
			case Observation:
				org.hl7.fhir.r4.model.Observation observation = (org.hl7.fhir.r4.model.Observation) resource;
				if (observation.hasEncounter()) {
					String placeholderReferenceUrl = observation.getEncounter().getReference();
					observation.setEncounter(createEncounterReference(getIdForPlaceHolderReference(placeholderReferenceUrl,
					    processedEntries)));
					entry.setResource(observation);
				}
                if (observation.hasHasMember()) {
                    observation.getHasMember().forEach(reference -> {
                        String placeholderReferenceUrl = reference.getReference();
                        String observationUuid = getIdForPlaceHolderReference(placeholderReferenceUrl, processedEntries);
                        reference.setReference(FhirConstants.OBSERVATION + "/" + observationUuid);
                        reference.setType(FhirConstants.OBSERVATION);
                    });
                    entry.setResource(observation);
                }
				break;
			default:
				break;
		}
		return entry;
	}
	
	public static List<Observation> sortObservationsByDepth(List<Observation> inputs) {
        List<Observation> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> beingVisited = new HashSet<>(); // For cycle detection

        for (Observation obj : inputs) {
            depthFirstSearch(obj, visited, beingVisited, result);
        }
        return result;
    }
	
	private static void depthFirstSearch(Observation node, Set<String> visited, Set<String> beingVisited,
	        List<Observation> result) {
		if (visited.contains(node.getId()))
			return;
		
		if (beingVisited.contains(node.getId())) {
			throw new RuntimeException("Circular dependency detected at: " + node.getId());
		}
		
		beingVisited.add(node.getId()); // Mark as currently in the recursion stack
		
		for (Reference child : node.getHasMember()) {
			Observation memberObs = (Observation) child.getResource();
			if (memberObs == null) {
				continue;
			}
			depthFirstSearch(memberObs, visited, beingVisited, result);
		}
		
		beingVisited.remove(node.getId()); // Remove from stack
		visited.add(node.getId()); // Mark as fully processed
		result.add(node); // Add to final list
	}
	
	private static Set<Reference> extractReferences(Resource resource) {
        Set<Reference> references = new HashSet<>();

        if (resource == null) {
            return references;
        }

        ResourceType resourceType = resource.getResourceType();

        switch (resourceType) {
            case Condition:
                org.hl7.fhir.r4.model.Condition condition = (org.hl7.fhir.r4.model.Condition) resource;
                if (condition.hasEncounter()) {
                    references.add(condition.getEncounter());
                }
                break;

            case AllergyIntolerance:
                org.hl7.fhir.r4.model.AllergyIntolerance allergyIntolerance = (org.hl7.fhir.r4.model.AllergyIntolerance) resource;
                if (allergyIntolerance.hasEncounter()) {
                    references.add(allergyIntolerance.getEncounter());
                }
                break;
            case ServiceRequest:
                org.hl7.fhir.r4.model.ServiceRequest serviceRequest = (org.hl7.fhir.r4.model.ServiceRequest) resource;
                if (serviceRequest.hasEncounter()) {
                    references.add(serviceRequest.getEncounter());
                }
                break;
            case MedicationRequest:
                org.hl7.fhir.r4.model.MedicationRequest medicationRequest = (org.hl7.fhir.r4.model.MedicationRequest) resource;
                if (medicationRequest.hasEncounter()) {
                    references.add(medicationRequest.getEncounter());
                }
                break;
            case Observation:
                org.hl7.fhir.r4.model.Observation observation = (org.hl7.fhir.r4.model.Observation) resource;
                if (observation.hasEncounter()) {
                    references.add(observation.getEncounter());
                }
                if (observation.hasHasMember()) {
                    references.addAll(observation.getHasMember());
                }
                break;
            default:
                break;
        }

        return references;
    }
	
	private static Reference createEncounterReference(String encounterUUID) {
		Reference encounterReference = new Reference();
		encounterReference.setType(FhirConstants.ENCOUNTER);
		encounterReference.setReference(FhirConstants.ENCOUNTER + "/" + encounterUUID);
		return encounterReference;
	}
	
	private static String getIdForPlaceHolderReference(String placeHolderReference,
	        Map<String, Bundle.BundleEntryComponent> processedEntries) {
		Bundle.BundleEntryComponent processedEntry = processedEntries.get(placeHolderReference);
		if (processedEntry == null) {
			throw new InternalErrorException("Could not find processed entry for " + placeHolderReference);
		}
		return processedEntry.getResource().getId();
	}
}

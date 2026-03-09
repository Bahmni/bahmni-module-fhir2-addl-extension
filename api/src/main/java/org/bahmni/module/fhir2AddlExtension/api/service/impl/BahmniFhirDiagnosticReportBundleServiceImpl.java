package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.helper.ConsultationBundleEntriesHelper;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportBundleService;
import org.bahmni.module.fhir2AddlExtension.api.service.LabResultsEncounterService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportBundleTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.utils.BahmniFhirUtils;
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportBundlePatchValidator;
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportBundleUpdateValidator;
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional
public class BahmniFhirDiagnosticReportBundleServiceImpl extends BaseFhirService<Bundle, FhirDiagnosticReportExt> implements BahmniFhirDiagnosticReportBundleService {
	
	public static final String BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT = "A bundle containing Diagnostic Report must be supplied";
	
	public static final String REPORT_MUST_HAVE_VALID_PATIENT_REFERENCE = "Diagnostic Report must have valid patient reference";
	
	public static final String INVALID_ENCOUNTER_REFERENCE = "Invalid encounter reference for Diagnostic Report";
	
	public static final String INVALID_SERVICE_REQUEST_REFERENCE = "Invalid Service Request Reference for Diagnostic Report";
	
	public static final String RESULT_OR_PRESENTED_FORM_REQUIRED = "Diagnostic Report can not be created without any result or presented form";
	
	public static final String INVALID_RESULT_OBSERVATION_REFERENCE = "Invalid result observation reference in Diagnostic Report bundle.";
	
	public static final String INVALID_REFERENCED_OBSERVATION_RESOURCE = "Can not identify referenced observation resource";
	
	public static final String INVALID_REFERENCES_TO_PATIENT = "Diagnostic Report has invalid result references to patient";
	
	private static final String BUNDLE_HAS_MULTIPLE_REPORT = "Invalid request with multiple Diagnostic Reports";
	
	public static final String INVALID_BASED_ON_REFERENCE_FOR_RESULT_OBSERVATION = "Invalid basedOn reference for result observation";
	
	private final BahmniFhirDiagnosticReportDao dao;
	
	private final BahmniFhirDiagnosticReportBundleTranslator translator;
	
	private final SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	private final SearchQuery<FhirDiagnosticReportExt, Bundle, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportBundleTranslator, SearchQueryInclude<Bundle>> searchQuery;
	
	private final DiagnosticReportValidator diagnosticReportValidator;
	
	private final DiagnosticReportBundlePatchValidator diagnosticReportBundlePatchValidator;
	
	private final DiagnosticReportBundleUpdateValidator diagnosticReportBundleUpdateValidator;
	
	private final BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator;
	
	private final BahmniOrderReferenceTranslator serviceRequestReferenceTranslator;
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final FhirObservationService fhirObservationService;
	
	private final FhirEncounterService fhirEncounterService;
	
	private final LabResultsEncounterService labEncounterService;
	
	@Autowired
	public BahmniFhirDiagnosticReportBundleServiceImpl(
	    BahmniFhirDiagnosticReportDao dao,
	    BahmniFhirDiagnosticReportBundleTranslator translator,
	    SearchQueryInclude<DiagnosticReport> searchQueryInclude,
	    SearchQuery<FhirDiagnosticReportExt, Bundle, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportBundleTranslator, SearchQueryInclude<Bundle>> searchQuery,
	    DiagnosticReportValidator diagnosticReportValidator,
	    DiagnosticReportBundlePatchValidator diagnosticReportBundlePatchValidator,
	    DiagnosticReportBundleUpdateValidator diagnosticReportBundleUpdateValidator,
	    BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator,
	    BahmniOrderReferenceTranslator serviceRequestReferenceTranslator,
	    PatientReferenceTranslator patientReferenceTranslator, FhirObservationService fhirObservationService,
	    FhirEncounterService fhirEncounterService, LabResultsEncounterService labEncounterService) {
		this.dao = dao;
		this.translator = translator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
		this.diagnosticReportValidator = diagnosticReportValidator;
		this.diagnosticReportBundlePatchValidator = diagnosticReportBundlePatchValidator;
		this.diagnosticReportBundleUpdateValidator = diagnosticReportBundleUpdateValidator;
		this.diagnosticReportTranslator = diagnosticReportTranslator;
		this.serviceRequestReferenceTranslator = serviceRequestReferenceTranslator;
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.fhirObservationService = fhirObservationService;
		this.fhirEncounterService = fhirEncounterService;
		this.labEncounterService = labEncounterService;
	}
	
	/**
	 * @param bundle containing diagnostic report and result observations
	 * @return created bundle containing diagnostic report and result observations
	 */
	@Override
	public Bundle create(@Nonnull Bundle bundle) {
		if (bundle == null) {
			log.error(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
			throw new InvalidRequestException(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
		}
		DiagnosticReport report = getReportFromBundle(bundle);
		Optional<org.openmrs.Patient> omrsPatient = findPatient(report);
		if (!omrsPatient.isPresent()) {
			log.error(REPORT_MUST_HAVE_VALID_PATIENT_REFERENCE);
			throw new InvalidRequestException(REPORT_MUST_HAVE_VALID_PATIENT_REFERENCE);
		}
		Function<String, Optional<Observation>> obsLocator = referenceId -> BahmniFhirUtils.findResourceInBundle(bundle, referenceId, Observation.class);

		List<Order> basedOnOrders = identifyOrders(report);
		Function<String, Optional<Encounter>> bundleEncounterLocator = referenceId -> BahmniFhirUtils.findResourceInBundle(bundle, referenceId, Encounter.class);
		Reference encounterReference = resolveEncounter(report, omrsPatient.get(), basedOnOrders, bundleEncounterLocator);
		report.setEncounter(encounterReference);
		Map<String, Reference> obsReferenceMap = createResultObservations(report, encounterReference, obsLocator, basedOnOrders);
		//set the matching references from the above saved observation list
		for (Reference resultRef : report.getResult()) {
			Optional<Reference> obsRef = BahmniFhirUtils.referenceToId(resultRef.getReference()).map(obsReferenceMap::get);
			if (obsRef.isPresent()) {
				log.debug(String.format("resolving report.result references: %s => %s%n", resultRef.getReference(), obsRef.get().getReference()));
				resultRef.setReference(obsRef.get().getReference());
			}
		}
		diagnosticReportValidator.validate(report);
		FhirDiagnosticReportExt diagnosticReportExt = diagnosticReportTranslator.toOpenmrsType(report);
		if (diagnosticReportExt.getUuid() == null) {
			diagnosticReportExt.setUuid(FhirUtils.newUuid());
		}
		return getTranslator().toFhirResource(getDao().createOrUpdate(diagnosticReportExt));
	}
	
	/**
	 * @param bundle to search
	 * @return identified report from entries
	 * @throws InvalidRequestException if there are no report or more than 1 in the bundle
	 */
	private DiagnosticReport getReportFromBundle(Bundle bundle) throws InvalidRequestException {
		List<DiagnosticReport> reportsInBundle = BahmniFhirUtils.findResourcesOfTypeInBundle(bundle, DiagnosticReport.class);
		if (reportsInBundle == null || reportsInBundle.isEmpty()) {
			log.error(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
			throw new InvalidRequestException(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
		}
		
		if (reportsInBundle.size() > 1) {
			log.error(BUNDLE_HAS_MULTIPLE_REPORT);
			throw new InvalidRequestException(BUNDLE_HAS_MULTIPLE_REPORT);
		}
		return reportsInBundle.get(0);
	}
	
	private Reference resolveEncounter(DiagnosticReport report, Patient omrsPatient,
									   List<Order> basedOnOrders,
									   Function<String, Optional<Encounter>> encounterLocator) {
		Reference encounter;
		if (report.hasEncounter()) {
			encounter = findOrCreateReferencedEncounterResource(report, encounterLocator);
		} else {
			Optional<Order> orderReference = Optional.empty();
			if (basedOnOrders.size() == 1) {
				orderReference = Optional.of(basedOnOrders.get(0));
			} else if (basedOnOrders.size() > 1) {
				orderReference = basedOnOrders.stream().min(Comparator.comparing(Order::getOrderId));
			}
			encounter = labEncounterService.createLabResultEncounter(omrsPatient, orderReference);
		}
		if (encounter == null) {
			throw new InvalidRequestException(INVALID_ENCOUNTER_REFERENCE);
		}
		return encounter;
	}
	
	/**
	 * This method validates all report.result observations. The validation includes checking the
	 * presence of the observation resource in the bundle or the system, and validating the patient
	 * reference in the observation matches with the report's subject reference.
	 * 
	 * @param diagnosticReport
	 * @param encounterReference
	 * @param bundledObsLocator
	 * @param basedOnOrders
	 * @return
	 */
	private Map<String, Reference> createResultObservations(DiagnosticReport diagnosticReport,
															Reference encounterReference,
															Function<String, Optional<Observation>> bundledObsLocator,
															List<Order> basedOnOrders) {
		Map<Observation, String> resultObservationReferenceMap = new HashMap<>();
		List<String> preExistingObservationIds = new ArrayList<>();
		diagnosticReport.getResult().forEach(result -> {
			IBaseResource res = result.getResource();
			Optional<String> refResultId = BahmniFhirUtils.referenceToId(result.getReference());
			if (!refResultId.isPresent()) {
				throw new InvalidRequestException(INVALID_RESULT_OBSERVATION_REFERENCE);
			}
			Observation existing = findExistingObservation(refResultId.get());
			if (existing != null) {
				preExistingObservationIds.add(existing.getId());
			}
			if (res != null) {
				resultObservationReferenceMap.put((Observation) res, refResultId.get());
				validatePatientReference(diagnosticReport.getSubject(), (Observation) res);
			} else {
				// the report.result reference may be an existing observation that is not part of the bundle,
				// or a new observation that is part of the bundle. We need to check both places before erroring out
				Observation resource = bundledObsLocator.apply(refResultId.get()).orElse(existing);
				if (resource == null) {
					log.error(INVALID_REFERENCED_OBSERVATION_RESOURCE);
					System.out.println(INVALID_REFERENCED_OBSERVATION_RESOURCE);
				}
				validatePatientReference(diagnosticReport.getSubject(), resource);
				resultObservationReferenceMap.put(resource, refResultId.orElse(null));
			}
		});
		List<Observation> resultResources = new ArrayList<>(resultObservationReferenceMap.keySet());

		Function<String, Optional<Order>> orderReferenceLocator = orderUuid -> basedOnOrders.stream().filter(order -> order.getUuid().equals(orderUuid)).findFirst();

		List<Observation> sortedObservations = ConsultationBundleEntriesHelper.sortObservationsByDepth(resultResources);
		Map<String, Reference> observationsReferenceMap = new HashMap<>();
		for (Observation observation : sortedObservations) {
			String resourceId = Optional.ofNullable(observation.getIdElement().getValue()).orElseGet(() -> resultObservationReferenceMap.get(observation));
			String obsEntryId = BahmniFhirUtils.extractId(resourceId);
			observation.setEncounter(encounterReference);
			if (observation.hasBasedOn()) {
				Optional<Order> applicableOrder = orderReferenceLocator.apply(FhirUtils.referenceToId(observation.getBasedOnFirstRep().getReference()).orElse(""));
				if (!applicableOrder.isPresent()) {
					log.error(INVALID_BASED_ON_REFERENCE_FOR_RESULT_OBSERVATION);
					throw new InvalidRequestException(INVALID_BASED_ON_REFERENCE_FOR_RESULT_OBSERVATION);
				}
				observation.setBasedOn(Collections.singletonList(serviceRequestReferenceTranslator.toFhirResource(applicableOrder.get())));
			}

			observation.getHasMember().forEach(member -> {
				Observation memberObs = (Observation) member.getResource();
				if (memberObs != null) {
					Reference mappedRef = observationsReferenceMap.get(memberObs.getId());
					if (mappedRef != null) {
						log.debug(String.format("resolving obs.member ref: %s => %s%n", member.getReference(), mappedRef.getReference()));
						member.setReference(mappedRef.getReference());
					}
				}
			});

			Observation persistedObservation = preExistingObservationIds.contains(obsEntryId)
					? fhirObservationService.update(obsEntryId, observation)
					: fhirObservationService.create(observation);
			Reference persistedObsReference = new Reference().setReference(FhirConstants.OBSERVATION + "/" + persistedObservation.getId()).setType(FhirConstants.OBSERVATION);
			persistedObsReference.setResource(persistedObservation);
			observationsReferenceMap.put(obsEntryId, persistedObsReference);
		}
		return observationsReferenceMap;
	}
	
	private Observation findExistingObservation(String uuid) {
		try {
			return fhirObservationService.get(uuid);
		}
		catch (ResourceNotFoundException e) {
			// No existing observation found, will create a new one
			log.debug("No existing observation found with UUID: " + uuid);
		}
		return null;
	}
	
	private Reference findOrCreateReferencedEncounterResource(DiagnosticReport diagnosticReport, Function<String, Optional<Encounter>> encounterLocator) {
		if (!diagnosticReport.hasEncounter()) {
			return null;
		}
		Encounter reportEncounterResource = (Encounter) diagnosticReport.getEncounter().getResource();
		if (reportEncounterResource != null) {
			return createBundledEncounter(reportEncounterResource);
		}
		Optional<String> refEncounterId = BahmniFhirUtils.referenceToId(diagnosticReport.getEncounter().getReference());
		if (!refEncounterId.isPresent()) {
			throw new InvalidRequestException(INVALID_ENCOUNTER_REFERENCE);
		}
		Optional<Encounter> encounterFromBundle = encounterLocator.apply(refEncounterId.get());
		if (encounterFromBundle.isPresent()) {
			return createBundledEncounter(encounterFromBundle.get());
		}
		log.info("Trying to identify existing encounter for Diagnostic.");
		Optional<Encounter> existingEncounter = refEncounterId.map(fhirEncounterService::get);
		if (!existingEncounter.isPresent()) {
			return null;
		}
		Reference existingRef = new Reference()
				.setReference(FhirConstants.ENCOUNTER + "/" + existingEncounter.get().getId())
				.setType(FhirConstants.ENCOUNTER);
		existingRef.setResource(existingEncounter.get());
		return existingRef;
	}
	
	private Reference createBundledEncounter(Encounter reportEncounterResource) {
		Encounter encounter = fhirEncounterService.create(reportEncounterResource);
		log.debug("created diagnostic report encounter with bundled encounter resource");
		Reference reference = new Reference().setReference(FhirConstants.ENCOUNTER + "/" + encounter.getId()).setType(
		    FhirConstants.ENCOUNTER);
		reference.setResource(encounter);
		return reference;
	}
	
	private Optional<Patient> findPatient(DiagnosticReport report) {
		if (!report.hasSubject()) {
			return Optional.empty();
		}
		return FhirUtils.getReferenceType(report.getSubject()).map(refType -> {
			if (FhirConstants.PATIENT.equals(refType)) {
				return patientReferenceTranslator.toOpenmrsType(report.getSubject());
			} else {
				return null;
			}
		});
	}
	
	private List<Order> identifyOrders(DiagnosticReport report) {
		if (!report.hasBasedOn()) {
			return Collections.emptyList();
		}
		List<Order> serviceRequests = new ArrayList<>();
		report.getBasedOn().forEach(reference -> {
			Order aOrder = serviceRequestReferenceTranslator.toOpenmrsType(reference);
			if (aOrder == null) {
				throw new InvalidRequestException(INVALID_SERVICE_REQUEST_REFERENCE);
			}
			serviceRequests.add(aOrder);
		});
		return serviceRequests;
	}
	
	private void validatePatientReference(Reference patientReference, Observation observation) {
		boolean sameRef = observation.getSubject().getReference().equals(patientReference.getReference());
		if (!sameRef) {
			log.error(INVALID_REFERENCES_TO_PATIENT);
			throw new InvalidRequestException(INVALID_REFERENCES_TO_PATIENT);
		}
	}
	
	/**
	 * Patches a DiagnosticReportBundle The "temporaryIds" to insert reference array elements and
	 * subsequently removing them post applying patch is done to resolve array element patch error
	 * while applying JSON PATCH, and erroring with "parent of node to add does not exist" This
	 * happens when the patch process can't find a array key in the resource to begin with, so it
	 * doesn't know where to "hang" the new array element. In FHIR, if a field is empty, it is
	 * typically omitted from the JSON representation entirely. JSON Patch (RFC 6902) requires that
	 * the parent object of the target must exist for an add operation to work on a specific index
	 * or the - (end of list) character. In HAPI FHIR (and the FHIR specification in general), if a
	 * list is empty, the HAPI serializers treat it as non-existent. When HAPI converts your
	 * DiagnosticReport POJO to a JSON string or a parse-tree to apply that patch, it skips any
	 * field where isEmpty() is true. Even if we do report.setPresentedForm(new ArrayList<>()), the
	 * resulting JSON will not contain a "presentedForm": [] key. Consequently, the JSON Patch
	 * engine sees a missing node and throws the "parent of node to add does not exist" error.
	 * 
	 * @param uuid the UUID of the bundle to patch
	 * @param patchType the type of patch (only JSON_PATCH supported)
	 * @param body the JSON patch body
	 * @param requestDetails the request details
	 * @return the patched DiagnosticReportBundle
	 */
	@Override
	public Bundle patch(@Nonnull String uuid, @Nonnull PatchTypeEnum patchType,
	                                     @Nonnull String body, RequestDetails requestDetails) {
		// Only JSON Patch is supported
		if (patchType != PatchTypeEnum.JSON_PATCH) {
			throw new InvalidRequestException("Only JSON Patch is supported for DiagnosticReportBundle");
		}
		
		Bundle existingBundle = get(uuid);
		if (existingBundle == null) {
			throw new ResourceNotFoundException("DiagnosticReportBundle with UUID " + uuid + " not found");
		}
		
		DiagnosticReport existingReport = getReportFromBundle(existingBundle);
		
		diagnosticReportBundlePatchValidator.validateNotInTerminalState(existingReport);
		
		List<String> tempUuidList = addTemporaryArrayElementsIfEmpty(existingReport);
		Bundle patchedBundle = applyJsonPatchToBundle(existingBundle, body, requestDetails);
		DiagnosticReport patchedReport = getReportFromBundle(patchedBundle);
		removeTemporaryArrayElements(tempUuidList, patchedReport);

		diagnosticReportBundlePatchValidator.validatePatchChanges(existingReport, patchedReport);
		
		List<Order> basedOnOrders = identifyOrders(patchedReport);
		
		List<Reference> newResultRefs = identifyNewResultReferences(existingReport, patchedReport);
		
		if (!newResultRefs.isEmpty()) {
			Function<String, Optional<Observation>> obsLocator = 
				referenceId -> BahmniFhirUtils.findResourceInBundle(patchedBundle, referenceId, Observation.class);
			
			validateNewResults(patchedReport, newResultRefs, obsLocator);
			
			Map<String, Reference> newObsReferenceMap = createResultObservations(
				patchedReport,
				patchedReport.getEncounter(), // Encounter is immutable
				obsLocator,
				basedOnOrders
			);
			
			updateReportResultReferences(patchedReport, newObsReferenceMap);
		}
		
		patchedReport.setId(existingReport.getId());
		diagnosticReportValidator.validate(patchedReport);
		FhirDiagnosticReportExt diagnosticReportExt = diagnosticReportTranslator.toOpenmrsType(patchedReport);
		
		return getTranslator().toFhirResource(getDao().createOrUpdate(diagnosticReportExt));
	}
	
	private void removeTemporaryArrayElements(List<String> tempUuidList, DiagnosticReport patchedReport) {
		if (patchedReport.hasBasedOn()) {
			patchedReport.getBasedOn().removeIf(ref -> tempUuidList.contains(ref.getReference()));
		}
		if (patchedReport.hasResult()) {
			patchedReport.getResult().removeIf(ref -> tempUuidList.contains(ref.getReference()));
		}
		if (patchedReport.hasPresentedForm()) {
			patchedReport.getPresentedForm().removeIf(attachment -> tempUuidList.contains(attachment.getUrl()));
		}
		if (patchedReport.hasPerformer()) {
			patchedReport.getPerformer().removeIf(ref -> tempUuidList.contains(ref.getReference()));
		}
	}
	
	private List<String> addTemporaryArrayElementsIfEmpty(DiagnosticReport existingReport) {
		List<String> tempUuidList = new ArrayList<>();
		if (!existingReport.hasBasedOn()) {
			String tempBasedOnId = UUID.randomUUID().toString();
			existingReport.addBasedOn().setReference("ServiceRequest/" + tempBasedOnId);
			tempUuidList.add("ServiceRequest/" + tempBasedOnId);
		}

		if (!existingReport.hasResult()) {
			String tempResultObsRef = "Observation/" + UUID.randomUUID().toString();
			existingReport.addResult(new Reference(tempResultObsRef));
			tempUuidList.add(tempResultObsRef);
		}

		if (!existingReport.hasPresentedForm()) {
			String tempPresentedFormId = UUID.randomUUID().toString();
			existingReport.addPresentedForm().setUrl("urn:uuid:" + tempPresentedFormId);
			tempUuidList.add("urn:uuid:" + tempPresentedFormId);
		}

		if (!existingReport.hasPerformer()) {
			String tempPerformerId = UUID.randomUUID().toString();
			String ref = FhirConstants.PRACTITIONER.concat("/").concat(tempPerformerId);
			existingReport.addPerformer().setReference(ref);
			tempUuidList.add(ref);
		}
		return tempUuidList;
	}
	
	/**
	 * Applies JSON Patch to a bundle using openmrs fhir2 JsonPatchUtils
	 */
	private Bundle applyJsonPatchToBundle(Bundle existingBundle, String patchBody, RequestDetails requestDetails) {
		try {
			FhirContext ctx = requestDetails.getFhirContext();
			return (Bundle) org.openmrs.module.fhir2.api.util.JsonPatchUtils.applyJsonPatch(ctx, existingBundle, patchBody);
		}
		catch (Exception e) {
			log.error("Error applying JSON patch to DiagnosticReportBundle", e);
			throw new InvalidRequestException("Invalid JSON patch: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Identifies new result references that were added in the patch
	 */
	private List<Reference> identifyNewResultReferences(DiagnosticReport original, DiagnosticReport patched) {
		java.util.Set<String> originalRefs = original.getResult().stream()
			.map(Reference::getReference)
			.filter(ref -> ref != null)
			.collect(Collectors.toSet());
		
		return patched.getResult().stream()
			.filter(ref -> ref.getReference() != null && !originalRefs.contains(ref.getReference()))
			.collect(Collectors.toList());
	}
	
	/**
	 * Validates new result observations added via patch
	 */
	private void validateNewResults(DiagnosticReport patchedReport, List<Reference> newResultRefs,
	        Function<String, Optional<Observation>> obsLocator) {
		for (Reference newRef : newResultRefs) {
			Optional<String> refId = BahmniFhirUtils.referenceToId(newRef.getReference());
			if (!refId.isPresent()) {
				throw new InvalidRequestException("Invalid result reference: " + newRef.getReference());
			}
			
			Observation obs = obsLocator.apply(refId.get()).orElse(null);
			if (obs == null) {
				throw new InvalidRequestException("New result observation not found in bundle: " + refId.get());
			}
			
			// Validate patient reference matches
			validatePatientReference(patchedReport.getSubject(), obs);
		}
	}
	
	/**
	 * Updates report references after creating observations
	 */
	private void updateReportResultReferences(DiagnosticReport report, Map<String, Reference> newObsReferenceMap) {
		for (Reference resultRef : report.getResult()) {
			Optional<String> refId = BahmniFhirUtils.referenceToId(resultRef.getReference());
			if (refId.isPresent() && newObsReferenceMap.containsKey(refId.get())) {
				Reference savedRef = newObsReferenceMap.get(refId.get());
				resultRef.setReference(savedRef.getReference());
			}
		}
	}
	
	/**
	 * Updates a DiagnosticReportBundle using PUT semantics (complete replacement) All mutable
	 * fields are replaced with new values Immutable fields: patient reference, encounter reference
	 * 
	 * @param uuid the UUID of the bundle to update
	 * @param bundle the complete replacement bundle
	 * @return the updated DiagnosticReportBundle
	 */
	@Override
	public Bundle update(@Nonnull String uuid, @Nonnull Bundle bundle) {
		if (bundle == null) {
			log.error(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
			throw new InvalidRequestException(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
		}
		
		Bundle existingBundle = get(uuid);
		if (existingBundle == null) {
			throw new ResourceNotFoundException("DiagnosticReportBundle with UUID " + uuid + " not found");
		}
		DiagnosticReport existingReport = getReportFromBundle(existingBundle);
		
		diagnosticReportBundleUpdateValidator.validateNotInTerminalState(existingReport);
		
		DiagnosticReport newReport = getReportFromBundle(bundle);
		
		diagnosticReportBundleUpdateValidator.validateUpdateChanges(existingReport, newReport);
		
		FhirDiagnosticReportExt existingEntity = getDao().get(uuid);
		purgeExistingResults(existingEntity);
		purgeExistingAttachments(existingEntity);
		purgeExistingBasedOn(existingEntity);
		getDao().createOrUpdate(existingEntity);

		newReport.setEncounter(existingReport.getEncounter());
		
		List<Order> basedOnOrders = identifyOrders(newReport);
		
		Function<String, Optional<Observation>> obsLocator =
			referenceId -> BahmniFhirUtils.findResourceInBundle(bundle, referenceId, Observation.class);

		Map<String, Reference> obsReferenceMap = createResultObservations(
			newReport,
			existingReport.getEncounter(),
			obsLocator,
			basedOnOrders
		);
		
		// Update result references in new report
		for (Reference resultRef : newReport.getResult()) {
			Optional<Reference> obsRef = BahmniFhirUtils.referenceToId(resultRef.getReference())
				.map(obsReferenceMap::get);
			if (obsRef.isPresent()) {
				log.debug(String.format("resolving report.result references: %s => %s%n", 
					resultRef.getReference(), obsRef.get().getReference()));
				resultRef.setReference(obsRef.get().getReference());
			}
		}
		
		diagnosticReportValidator.validate(newReport);
		FhirDiagnosticReportExt updatedEntity = diagnosticReportTranslator.toOpenmrsType(existingEntity, newReport);
		return getTranslator().toFhirResource(getDao().createOrUpdate(updatedEntity));
	}
	
	private void purgeExistingBasedOn(FhirDiagnosticReportExt existingEntity) {
		existingEntity.getOrders().clear();
	}
	
	/**
	 * Voids all existing result observations and clears references
	 */
	private void purgeExistingResults(FhirDiagnosticReportExt existingEntity) {
		existingEntity.getResults().forEach(obs -> {
			fhirObservationService.delete(obs.getUuid());
		});
		existingEntity.getResults().clear();
	}
	
	/**
	 * Deletes existing attachment metadata (hard delete)
	 */
	private void purgeExistingAttachments(FhirDiagnosticReportExt existingEntity) {
		// Clear presentedForm attachments
		// TODO: Implement actual deletion from document_attachment table via DAO when attachment feature is implemented
		// Note, in actual operation, the attachment deletion may be handled in backoffice process, and the attachment
		// records may be retained for audit/history purposes with a voided flag, rather than hard deleted.
		// This is a design decision to be made.
		existingEntity.getPresentedForms().clear();
	}
	
	@Override
	protected FhirDao<FhirDiagnosticReportExt> getDao() {
		return dao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDiagnosticReportExt, Bundle> getTranslator() {
		return translator;
	}
	
}

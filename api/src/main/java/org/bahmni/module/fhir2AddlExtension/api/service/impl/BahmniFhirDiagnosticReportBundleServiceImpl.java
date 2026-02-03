package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional
public class BahmniFhirDiagnosticReportBundleServiceImpl extends BaseFhirService<DiagnosticReportBundle, FhirDiagnosticReportExt> implements BahmniFhirDiagnosticReportBundleService {
	
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
	
	private final SearchQuery<FhirDiagnosticReportExt, DiagnosticReportBundle, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportBundleTranslator, SearchQueryInclude<DiagnosticReportBundle>> searchQuery;
	
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
	    SearchQuery<FhirDiagnosticReportExt, DiagnosticReportBundle, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportBundleTranslator, SearchQueryInclude<DiagnosticReportBundle>> searchQuery,
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
	public DiagnosticReportBundle create(@Nonnull DiagnosticReportBundle bundle) {
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
		validateResults(report, report.getSubject(), obsLocator);

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
	private DiagnosticReport getReportFromBundle(DiagnosticReportBundle bundle) throws InvalidRequestException {
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
	
	private Map<String, Reference> createResultObservations(DiagnosticReport diagnosticReport,
															Reference encounterReference,
															Function<String, Optional<Observation>> obsLocator,
															List<Order> basedOnOrders) {
		List<Observation> resultResources = diagnosticReport.getResult().stream().map(reference -> {
			IBaseResource res = reference.getResource();
			if (res != null) {
				return (Observation) res;
			}
			Optional<String> refResultId = BahmniFhirUtils.referenceToId(reference.getReference());
			return obsLocator.apply(refResultId.get()).orElse(null);
		}).filter(Objects::nonNull).collect(Collectors.toList());

		Function<String, Optional<Order>> orderReferenceLocator = orderUuid -> basedOnOrders.stream().filter(order -> order.getUuid().equals(orderUuid)).findFirst();

		List<Observation> sortedObservations = ConsultationBundleEntriesHelper.sortObservationsByDepth(resultResources);
		Map<String, Reference> observationsReferenceMap = new HashMap<>();
		for (Observation observation : sortedObservations) {
			String obsEntryId = BahmniFhirUtils.extractId(observation.getIdElement().getValue());
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
			Observation newObservation = fhirObservationService.create(observation);
			Reference newObsReference = new Reference().setReference(FhirConstants.OBSERVATION + "/" + newObservation.getId()).setType(FhirConstants.OBSERVATION);
			newObsReference.setResource(newObservation);
			observationsReferenceMap.put(obsEntryId, newObsReference);
		}
		return observationsReferenceMap;
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
	
	private void validateResults(DiagnosticReport diagnosticReport, Reference patientReference, Function<String, Optional<Observation>> bundledObservationLocator) {
		if (!diagnosticReport.hasPresentedForm()
				&& !diagnosticReport.hasResult()
				&& !DiagnosticReportValidator.DIAGNOSTIC_REPORT_DRAFT_STATES.contains(diagnosticReport.getStatus())) {
			log.error(RESULT_OR_PRESENTED_FORM_REQUIRED);
			throw new InvalidRequestException(RESULT_OR_PRESENTED_FORM_REQUIRED);
		}
		if (diagnosticReport.hasResult()) {
			diagnosticReport.getResult().forEach(resultRef -> {
				Observation referencedResource = (Observation) resultRef.getResource();
				if (referencedResource != null) {
					validatePatientReference(patientReference, referencedResource);
					return;
				}
				Optional<String> refResultId = BahmniFhirUtils.referenceToId(resultRef.getReference());
				if (!refResultId.isPresent()) {
					throw new InvalidRequestException(INVALID_RESULT_OBSERVATION_REFERENCE);
				}

				referencedResource = bundledObservationLocator
						.apply(refResultId.get())
						.orElseGet(() -> fhirObservationService.get(refResultId.get()));

				if (referencedResource == null) {
					log.error(INVALID_REFERENCED_OBSERVATION_RESOURCE);
					System.out.println(INVALID_REFERENCED_OBSERVATION_RESOURCE);
				}
				validatePatientReference(patientReference, referencedResource);
			});
		}
	}
	
	private void validatePatientReference(Reference patientReference, Observation observation) {
		boolean sameRef = observation.getSubject().getReference().equals(patientReference.getReference());
		if (!sameRef) {
			log.error(INVALID_REFERENCES_TO_PATIENT);
			throw new InvalidRequestException(INVALID_REFERENCES_TO_PATIENT);
		}
	}
	
	/**
	 * Patches a DiagnosticReportBundle
	 * 
	 * @param uuid the UUID of the bundle to patch
	 * @param patchType the type of patch (only JSON_PATCH supported)
	 * @param body the JSON patch body
	 * @param requestDetails the request details
	 * @return the patched DiagnosticReportBundle
	 */
	@Override
	public DiagnosticReportBundle patch(@Nonnull String uuid, @Nonnull PatchTypeEnum patchType, 
	                                     @Nonnull String body, RequestDetails requestDetails) {
		// Only JSON Patch is supported
		if (patchType != PatchTypeEnum.JSON_PATCH) {
			throw new InvalidRequestException("Only JSON Patch is supported for DiagnosticReportBundle");
		}
		
		// Step 1: Retrieve existing bundle
		DiagnosticReportBundle existingBundle = get(uuid);
		if (existingBundle == null) {
			throw new ResourceNotFoundException("DiagnosticReportBundle with UUID " + uuid + " not found");
		}
		
		DiagnosticReport existingReport = getReportFromBundle(existingBundle);
		
		// Step 2: Validate current state (not in terminal state)
		diagnosticReportBundlePatchValidator.validateNotInTerminalState(existingReport);
		
		// Step 3: Apply JSON Patch to the bundle
		DiagnosticReportBundle patchedBundle = applyJsonPatchToBundle(existingBundle, body, requestDetails);
		
		// Step 4: Extract patched report from bundle
		DiagnosticReport patchedReport = getReportFromBundle(patchedBundle);
		
		// Step 5: Validate patch changes (immutability rules)
		diagnosticReportBundlePatchValidator.validatePatchChanges(existingReport, patchedReport);
		
		// Step 6: Identify basedOn orders
		List<Order> basedOnOrders = identifyOrders(patchedReport);
		
		// Step 7: Identify and handle new observations
		List<Reference> newResultRefs = identifyNewResultReferences(existingReport, patchedReport);
		
		if (!newResultRefs.isEmpty()) {
			Function<String, Optional<Observation>> obsLocator = 
				referenceId -> BahmniFhirUtils.findResourceInBundle(patchedBundle, referenceId, Observation.class);
			
			// Validate new observations
			validateNewResults(patchedReport, newResultRefs, obsLocator);
			
			// Step 8: Create new observations
			Map<String, Reference> newObsReferenceMap = createResultObservations(
				patchedReport,
				patchedReport.getEncounter(), // Encounter is immutable
				obsLocator,
				basedOnOrders
			);
			
			// Step 9: Update report with actual observation references
			updateReportResultReferences(patchedReport, newObsReferenceMap);
		}
		
		// Step 10: Validate and save updated report
		diagnosticReportValidator.validate(patchedReport);
		FhirDiagnosticReportExt diagnosticReportExt = diagnosticReportTranslator.toOpenmrsType(patchedReport);
		
		return getTranslator().toFhirResource(getDao().createOrUpdate(diagnosticReportExt));
	}
	
	/**
	 * Applies JSON Patch to a bundle using openmrs fhir2 JsonPatchUtils
	 */
	private DiagnosticReportBundle applyJsonPatchToBundle(DiagnosticReportBundle existingBundle, String patchBody,
	        RequestDetails requestDetails) {
		try {
			FhirContext ctx = requestDetails.getFhirContext();
			// Use openmrs fhir2 JsonPatchUtils static method
			return (DiagnosticReportBundle) org.openmrs.module.fhir2.api.util.JsonPatchUtils.applyJsonPatch(ctx,
			    existingBundle, patchBody);
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
	public DiagnosticReportBundle update(@Nonnull String uuid, @Nonnull DiagnosticReportBundle bundle) {
		if (bundle == null) {
			log.error(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
			throw new InvalidRequestException(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
		}
		
		// Step 1-2: Retrieve existing bundle and extract report
		DiagnosticReportBundle existingBundle = get(uuid);
		if (existingBundle == null) {
			throw new ResourceNotFoundException("DiagnosticReportBundle with UUID " + uuid + " not found");
		}
		DiagnosticReport existingReport = getReportFromBundle(existingBundle);
		
		// Step 3: Validate terminal state
		diagnosticReportBundleUpdateValidator.validateNotInTerminalState(existingReport);
		
		// Step 4: Extract new report from incoming bundle
		DiagnosticReport newReport = getReportFromBundle(bundle);
		
		// Step 5: Validate immutability rules (only patient and encounter)
		diagnosticReportBundleUpdateValidator.validateUpdateChanges(existingReport, newReport);
		
		// Step 6-8: PURGE existing data
		FhirDiagnosticReportExt existingEntity = dao.get(uuid);
		purgeExistingResults(existingEntity);
		purgeExistingAttachments(existingEntity);
		purgeExistingBasedOn(existingEntity);
		dao.createOrUpdate(existingEntity);

		// Step 11-15: UPDATE with new data
		// Preserve encounter (immutable)
		newReport.setEncounter(existingReport.getEncounter());
		
		// Identify and set new basedOn orders
		List<Order> basedOnOrders = identifyOrders(newReport);
		
		// Create new result observations
		Function<String, Optional<Observation>> obsLocator = 
			referenceId -> BahmniFhirUtils.findResourceInBundle(bundle, referenceId, Observation.class);
		validateResults(newReport, newReport.getSubject(), obsLocator);
		
		Map<String, Reference> obsReferenceMap = createResultObservations(
			newReport,
			existingReport.getEncounter(), // Use existing (immutable) encounter
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
		
		// Step 16: Validate complete report
		diagnosticReportValidator.validate(newReport);
		
		// Step 17: Convert to OpenMRS entity and save (reusing existing UUID and ID)
		FhirDiagnosticReportExt updatedEntity = diagnosticReportTranslator.toOpenmrsType(existingEntity, newReport);
//		updatedEntity.setUuid(uuid); // Preserve UUID for update
//		updatedEntity.setId(existingEntity.getId()); // Preserve DB ID

		return getTranslator().toFhirResource(dao.createOrUpdate(updatedEntity));
	}
	
	private void purgeExistingBasedOn(FhirDiagnosticReportExt existingEntity) {
		//		if (existingReport.hasBasedOn()) {
		//			existingReport.getBasedOn().clear();
		//		}
		existingEntity.getOrders().clear();
	}
	
	/**
	 * Voids all existing result observations and clears references
	 */
	private void purgeExistingResults(FhirDiagnosticReportExt existingEntity) {
//		List<String> resultUuids = existingReport.getResult().stream()
//			.map(ref -> FhirUtils.referenceToId(ref.getReference()).orElse(null))
//			.filter(Objects::nonNull)
//			.collect(Collectors.toList());

		existingEntity.getResults().forEach(obs -> {
			fhirObservationService.delete(obs.getUuid());
		});
		existingEntity.getResults().clear();
		
//		// Void each observation (soft delete for audit trail)
//		for (String obsUuid : resultUuids) {
//			try {
//				fhirObservationService.delete(obsUuid);
//				log.debug("Voided observation: {}", obsUuid);
//			} catch (Exception e) {
//				log.error("Failed to void observation: {}", obsUuid, e);
//				throw new InvalidRequestException("Failed to purge existing result observation: " + obsUuid, e);
//			}
//		}
//
//		// Clear result references from report
//		existingReport.getResult().clear();
	}
	
	/**
	 * Deletes existing attachment metadata (hard delete)
	 */
	private void purgeExistingAttachments(FhirDiagnosticReportExt existingEntity) {
		// Clear presentedForm attachments
		// Note: Actual deletion logic depends on how attachments are implemented
		// This should hard delete from document_attachment table
		//		if (existingReport.hasPresentedForm()) {
		//			existingReport.getPresentedForm().clear();
		//		}
		// TODO: Implement actual deletion from document_attachment table via DAO when attachment feature is implemented
		// In actual operation, the attachment deletion may be handled in backoffice process.
		existingEntity.getPresentedForms().clear();
	}
	
	@Override
	protected FhirDao<FhirDiagnosticReportExt> getDao() {
		return dao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDiagnosticReportExt, DiagnosticReportBundle> getTranslator() {
		return translator;
	}
	
}

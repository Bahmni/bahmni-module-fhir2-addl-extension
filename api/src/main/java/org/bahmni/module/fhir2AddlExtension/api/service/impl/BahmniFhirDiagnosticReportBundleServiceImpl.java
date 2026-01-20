package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.bahmni.module.fhir2AddlExtension.api.helper.ConsultationBundleEntriesHelper;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportBundleService;
import org.bahmni.module.fhir2AddlExtension.api.service.LabResultsEncounterService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportBundleTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.utils.BahmniFhirUtils;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
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

	private final BahmniFhirDiagnosticReportDao dao;
	
	private final BahmniFhirDiagnosticReportBundleTranslator translator;
	
	private final SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	private final SearchQuery<FhirDiagnosticReportExt, DiagnosticReportBundle, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportBundleTranslator, SearchQueryInclude<DiagnosticReportBundle>> searchQuery;
	
	private final DiagnosticReportValidator diagnosticReportValidator;
	
	private final BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator;
	
	private final BahmniServiceRequestReferenceTranslator serviceRequestReferenceTranslator;
	
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
	    BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator,
	    BahmniServiceRequestReferenceTranslator serviceRequestReferenceTranslator,
	    PatientReferenceTranslator patientReferenceTranslator, FhirObservationService fhirObservationService,
	    FhirEncounterService fhirEncounterService, LabResultsEncounterService labEncounterService) {
		this.dao = dao;
		this.translator = translator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
		this.diagnosticReportValidator = diagnosticReportValidator;
		this.diagnosticReportTranslator = diagnosticReportTranslator;
		this.serviceRequestReferenceTranslator = serviceRequestReferenceTranslator;
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.fhirObservationService = fhirObservationService;
		this.fhirEncounterService = fhirEncounterService;
		this.labEncounterService = labEncounterService;
	}
	
	/**
	 * TODO 1. validate in the bundle, - presence of a diagnostic report - presence of orders (based
	 * on service request) if present - set of observations in the bundle unless status is draft
	 * (registered, partial, null) - either a report.encounter reference or a ref to existing
	 * encounter, in absence of which a lab results encounter will be created 2. order of processing
	 * - save in order: encounter => observations > reports => attachments 3. Processing
	 * observations - process child members first or as together with parent - obs encounter and
	 * order id (if present) set - abstract out saving of attachments
	 * 
	 * @param bundle containing diagnostic report and result observations
	 * @return created bundle containing diagnostic report and result observations
	 */
	@Override
	public DiagnosticReportBundle create(@Nonnull DiagnosticReportBundle bundle) {
		if (bundle == null) {
			log.error(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
			throw new InvalidRequestException(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
		}
		Optional<DiagnosticReport> report = BahmniFhirUtils.findResourceOfTypeInBundle(bundle, FhirConstants.DIAGNOSTIC_REPORT,
		    DiagnosticReport.class);
		if (!report.isPresent()) {
			log.error(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
			throw new InvalidRequestException(BUNDLE_MUST_HAVE_DIAGNOSTIC_REPORT);
		}
		Optional<org.openmrs.Patient> omrsPatient = findPatient(report.get());
		if (!omrsPatient.isPresent()) {
			log.error(REPORT_MUST_HAVE_VALID_PATIENT_REFERENCE);
			throw new InvalidRequestException(REPORT_MUST_HAVE_VALID_PATIENT_REFERENCE);
		}
		Function<String, Optional<Observation>> obsLocator = referenceId -> BahmniFhirUtils.findResourceInBundle(bundle, referenceId, Observation.class);
		validateResults(report.get(), report.get().getSubject(), obsLocator);
		//TODO match observation code with order code to identify the right order, as a diagnostic report may submit for multiple order
		Optional<Map.Entry<Reference, Order>> orderReferenceMap = getServiceRequest(identifyOrders(report.get()));
		Reference encounterReference = resolveEncounter(report.get(), omrsPatient.get(), orderReferenceMap.map(entry -> entry.getValue()));
		//since diagnostic report supports submission for multiple orders,
		// we need to pass all the order references and resolve the observation to order relation by looking up the concept
		// there is a chance that observation.code does not map to order concept (e.g. panel, panel member, or individual test)
		// and in that case we do not set order reference
		Map<String, Reference> obsReferenceMap = createResultObservations(report.get(), encounterReference, orderReferenceMap.map(entry -> entry.getKey()), obsLocator);
		//set the matching references from the above saved observation list
		for (Reference resultRef : report.get().getResult()) {
			Optional<Reference> obsRef = BahmniFhirUtils.referenceToId(resultRef.getReference()).map(id -> obsReferenceMap.get(id));
			if (obsRef.isPresent()) {
				log.info("replacing report.result references: %s - %s%n", resultRef.getReference(), obsRef.get().getReference());
				resultRef.setReference(obsRef.get().getReference());
			}
		}
		diagnosticReportValidator.validate(report.get());
		FhirDiagnosticReportExt diagnosticReportExt = diagnosticReportTranslator.toOpenmrsType(report.get());
		if (diagnosticReportExt.getUuid() == null) {
			diagnosticReportExt.setUuid(FhirUtils.newUuid());
		}
		return getTranslator().toFhirResource(getDao().createOrUpdate(diagnosticReportExt));
	}
	
	private Reference resolveEncounter(DiagnosticReport report, Patient omrsPatient, Optional<Order> order) {
		Reference encounter = report.hasEncounter() ? findOrCreateReferencedEncounterResource(report) : labEncounterService
		        .createLabResultEncounter(omrsPatient, order);
		if (encounter == null) {
			throw new InvalidRequestException(INVALID_ENCOUNTER_REFERENCE);
		}
		return encounter;
	}
	
	private Map<String, Reference> createResultObservations(DiagnosticReport diagnosticReport, Reference encounterReference, Optional<Reference> orderReference, Function<String, Optional<Observation>> obsLocator) {
		List<Observation> resultResources = diagnosticReport.getResult().stream().map(reference -> {
			IBaseResource res = reference.getResource();
			if (res != null) {
				return (Observation) res;
			}
			Optional<String> refResultId = BahmniFhirUtils.referenceToId(reference.getReference());
			return obsLocator.apply(refResultId.get()).orElse(null);
		}).filter(Objects::nonNull).collect(Collectors.toList());

		List<Observation> sortedObservations = ConsultationBundleEntriesHelper.sortObservationsByDepth(resultResources);
		Map<String, Reference> observationsReferenceMap = new HashMap<>();
		for (Observation observation : sortedObservations) {
			String obsEntryId = BahmniFhirUtils.extractId(observation.getIdElement().getValue());
			observation.setEncounter(encounterReference);
			if (orderReference.isPresent()) {
				observation.setBasedOn(Collections.singletonList(orderReference.get()));
			}
			observation.getHasMember().forEach(member -> {
				Observation memberObs = (Observation) member.getResource();
				if (memberObs != null) {
					Reference mappedRef = observationsReferenceMap.get(memberObs.getId());
					if (mappedRef != null) {
						log.info("replacing obs.member ref: %s - %s%n", member.getReference(), mappedRef.getReference());
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
	
	private Reference findOrCreateReferencedEncounterResource(DiagnosticReport diagnosticReport) {
		if (diagnosticReport.hasEncounter()) {
			Encounter reportEncounterResource = (Encounter) diagnosticReport.getEncounter().getResource();
			if (reportEncounterResource == null) {
				log.info("Diagnostic Report was submitted with an existing encounter reference.");
				Optional<String> resourceUuid = FhirUtils.referenceToId(diagnosticReport.getEncounter().getReference());
				if (!resourceUuid.isPresent()) {
					return null;
				}
				Encounter resource = fhirEncounterService.get(resourceUuid.get());
				if (resource == null) {
					return null;
				}
				Reference encounterReference = new Reference()
				        .setReference(FhirConstants.ENCOUNTER + "/" + resource.getId()).setType(FhirConstants.ENCOUNTER);
				encounterReference.setResource(resource);
				return encounterReference;
			} else {
				Encounter encounter = fhirEncounterService.create(reportEncounterResource);
				log.debug("created diagnostic report encounter with bundled encounter resource");
				Reference reference = new Reference().setReference(FhirConstants.ENCOUNTER + "/" + encounter.getId())
				        .setType(FhirConstants.ENCOUNTER);
				reference.setResource(encounter);
				return reference;
			}
		}
		return null;
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
	
	private Optional<Map.Entry<Reference, Order>> getServiceRequest(List<Order> serviceRequests) {
		if (serviceRequests.isEmpty()) {
			return Optional.empty();
		}
		Reference orderReference = serviceRequestReferenceTranslator.toFhirResource(serviceRequests.get(0));
		return Optional.of(new AbstractMap.SimpleImmutableEntry<>(orderReference, serviceRequests.get(0)));
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
	
	@Override
	protected FhirDao<FhirDiagnosticReportExt> getDao() {
		return dao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDiagnosticReportExt, DiagnosticReportBundle> getTranslator() {
		return translator;
	}
	
}

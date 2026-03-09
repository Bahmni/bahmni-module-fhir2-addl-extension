package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.bahmni.module.fhir2AddlExtension.api.service.LabResultsEncounterService;
import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class LabResultsEncounterServiceImpl implements LabResultsEncounterService {
	
	private static final String UNABLE_TO_IDENTIFY_PROVIDER = "Unable to identify user as a registered Provider. Please contact your administrator";
	
	private static final String LAB_RESULT_ENC_TYPE = "LAB_RESULT";
	
	private static final String DEFAULT_LAB_VISIT_TYPE = "LAB_VISIT";
	
	private static final String UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT = "Can not process Diagnostic Report. Please check with your administrator.";
	
	private final AppContext appContext;
	
	private final VisitService visitService;
	
	private final EncounterService encounterService;
	
	private final ProviderService providerService;
	
	@Autowired
	public LabResultsEncounterServiceImpl(AppContext appContext, VisitService visitService,
	    EncounterService encounterService, ProviderService providerService) {
		this.appContext = appContext;
		this.visitService = visitService;
		this.encounterService = encounterService;
		this.providerService = providerService;
	}
	
	@Override
	public Reference createLabResultEncounter(Patient omrsPatient, Optional<Order> order) {
		Visit visit = findOrCreateLabVisit(omrsPatient, order);
		EncounterType encounterType = appContext.getEncounterType(LAB_RESULT_ENC_TYPE);
		if (encounterType == null) {
			log.error("Encounter type LAB_RESULT must be defined to support Diagnostic Report");
			throw new InvalidRequestException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
		}
		
		Location location = Context.getUserContext().getLocation();
		if (location == null) {
			log.error("Logged in location for user is null. Can not identify encounter session.");
			throw new InvalidRequestException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
		}
		
		if (visit == null) {
			log.error("Can not identify or create visit for the patient for lab results upload. Please check with your administrator");
			throw new InvalidRequestException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
		}
		
		org.openmrs.Encounter encounter = encounterService
		        .saveEncounter(newEncounterInstance(visit, encounterType, location));
		return new Reference().setReference(FhirConstants.ENCOUNTER + "/" + encounter.getUuid()).setType(
		    FhirConstants.ENCOUNTER);
	}
	
	private Encounter newEncounterInstance(Visit visit, EncounterType encounterType, Location location) {
        User authenticatedUser = Context.getAuthenticatedUser();

        Optional<Provider> provider = providerService.getProvidersByPerson(authenticatedUser.getPerson(), false).stream().findFirst();
        if (!provider.isPresent()) {
            log.error("Can not identify user as a registered Provider. Please check with your administrator");
            throw new InvalidRequestException(UNABLE_TO_IDENTIFY_PROVIDER);
        }
        org.openmrs.Encounter encounter = new org.openmrs.Encounter();
        encounter.setVisit(visit);
        encounter.setPatient(visit.getPatient());
        encounter.setEncounterType(encounterType);
        encounter.setUuid(UUID.randomUUID().toString());
        encounter.setEncounterDatetime(visit.getStartDatetime());
        encounter.setLocation(location);
        EncounterRole labEncROle = appContext.getLabEncounterRole();
        EncounterProvider encounterProvider = provider.map(prov -> {
            EncounterProvider encProv = new EncounterProvider();
            encProv.setEncounter(encounter);
            encProv.setProvider(prov);
            encProv.setEncounterRole(labEncROle);
            return encProv;
        }).get();
        encounter.setEncounterProviders(Collections.singleton(encounterProvider));

        encounter.setCreator(authenticatedUser);
        return encounter;
    }
	
	protected Visit findOrCreateLabVisit(Patient patient, Optional<Order> order) {
        //identify visit from order
        //or else get Active Visit
        //or create a LAB VISIT
        Visit visit = order.map(Order::getEncounter).map(org.openmrs.Encounter::getVisit).orElse(null);
        if (visit != null) {
            return visit;
        }
        visit = getActiveVisit(patient);
        if (visit != null) {
            return visit;
        }
        Location location = Context.getUserContext().getLocation();
        if (location == null) {
            log.error("Logged in location for user is null. Can not identify encounter session.");
            throw new RuntimeException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
        }

        return createLabVisitInAbsentia(patient, location);
    }
	
	protected Visit getActiveVisit(Patient patient) {
		List<Visit> activeVisits = visitService.getActiveVisitsByPatient(patient);
		if (CollectionUtils.isEmpty(activeVisits)) {
			return null;
		}
		return activeVisits.get(0);
	}
	
	protected Visit createLabVisitInAbsentia(Patient patient, Location location) {
        List<VisitType> labVisitTypes = visitService.getVisitTypes(DEFAULT_LAB_VISIT_TYPE);
        if (CollectionUtils.isEmpty(labVisitTypes)) {
            return null;
        }

        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Instant endOfDate = LocalTime.MAX.atDate(today).atZone(ZoneId.systemDefault()).toInstant();
        Date startDate = Date.from(startOfDay);
        Date endDate = Date.from(endOfDate);

        List<Visit> labVisitsForToday = visitService.getVisits(
                labVisitTypes,
                Collections.singletonList(patient),
                null,
                null,
                startDate, null,
                null, endDate,
                null,
                true,
                false);

        if (!CollectionUtils.isEmpty(labVisitsForToday)) {
            return labVisitsForToday.stream().reduce((first, second) -> second).orElse(null);
        }

        Date visitStartDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        Date visitEndDate = Date.from(LocalDateTime.now().plus(Duration.of(5, ChronoUnit.MINUTES)).atZone(ZoneId.systemDefault()).toInstant());
        Visit newVisit = new Visit();
        newVisit.setPatient(patient);
        newVisit.setVisitType(labVisitTypes.get(0));
        newVisit.setStartDatetime(visitStartDate);
        newVisit.setStopDatetime(visitEndDate);
        newVisit.setEncounters(new HashSet<>());
        newVisit.setLocation(ModuleUtils.getVisitLocation(location));
        return visitService.saveVisit(newVisit);
    }
}

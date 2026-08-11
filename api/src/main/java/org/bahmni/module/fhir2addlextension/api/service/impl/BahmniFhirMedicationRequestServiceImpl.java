package org.bahmni.module.fhir2addlextension.api.service.impl;

import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirMedicationRequestService;
import org.openmrs.module.fhir2.api.impl.FhirMedicationRequestServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BahmniFhirMedicationRequestServiceImpl extends FhirMedicationRequestServiceImpl implements BahmniFhirMedicationRequestService {}

package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniOrderReferenceTranslator;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirMedicationRequestDao;
import org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static lombok.AccessLevel.PROTECTED;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.getReferenceId;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.getReferenceType;

@Slf4j
@Component
public class BahmniOrderReferenceTranslatorImpl implements BahmniOrderReferenceTranslator {
	
	@Getter(value = AccessLevel.PROTECTED)
	private BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Getter(PROTECTED)
	private FhirMedicationRequestDao medicationRequestDao;
	
	@Autowired
	public BahmniOrderReferenceTranslatorImpl(BahmniFhirServiceRequestDao<Order> serviceRequestDao,
	    FhirMedicationRequestDao medicationRequestDao) {
		this.serviceRequestDao = serviceRequestDao;
		this.medicationRequestDao = medicationRequestDao;
	}
	
	@Override
	public Reference toFhirResource(@Nonnull Order order) {
		if (order == null) {
			return null;
		}
		return createRequestReference(order);
	}
	
	private Reference createRequestReference(Order order) {
		if (order instanceof DrugOrder) {
			return ReferenceHandlingTranslator.createDrugOrderReference((DrugOrder) order);
		}
		return new Reference().setReference(FhirConstants.SERVICE_REQUEST + "/" + order.getUuid()).setType(
		    FhirConstants.SERVICE_REQUEST);
	}
	
	@Override
    public Order toOpenmrsType(@Nonnull Reference reference) {
        if (reference == null) {
            return null;
        }

        if (getReferenceType(reference)
                .map(ref -> !(ref.equals(FhirConstants.SERVICE_REQUEST) || ref.equals(FhirConstants.MEDICATION_REQUEST)))
                .orElse(true)) {
            throw new IllegalArgumentException("Reference must be to a ServiceRequest or MedicationRequest");
        }

        return getReferenceId(reference).map(uuid -> {
            String refType = getReferenceType(reference).orElse(null);
            if (refType == null) {
                return null;
            }
            switch (refType) {
                case FhirConstants.MEDICATION_REQUEST:
                    return medicationRequestDao.get(uuid);
                case FhirConstants.SERVICE_REQUEST:
                    return serviceRequestDao.get(uuid);
                default:
                    return null;
            }
        }).orElse(null);
    }
}

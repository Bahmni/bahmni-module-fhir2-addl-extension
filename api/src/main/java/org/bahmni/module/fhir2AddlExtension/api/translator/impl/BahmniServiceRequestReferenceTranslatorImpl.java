package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
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
public class BahmniServiceRequestReferenceTranslatorImpl implements BahmniServiceRequestReferenceTranslator {
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Getter(PROTECTED)
	@Setter(value = PROTECTED, onMethod_ = @Autowired)
	private FhirMedicationRequestDao medicationRequestDao;
	
	@Override
	public Reference toFhirResource(@Nonnull Order order) {
		if (order == null) {
			return null;
		}
		return createServiceRequestReference(order);
	}
	
	private Reference createServiceRequestReference(Order order) {
		if (order instanceof Order) {
			return new Reference().setReference(FhirConstants.SERVICE_REQUEST + "/" + order.getUuid()).setType(
			    FhirConstants.SERVICE_REQUEST);
		} else if (order instanceof DrugOrder) {
			return ReferenceHandlingTranslator.createDrugOrderReference((DrugOrder) order);
		} else {
			log.warn("Could not determine order type for order {}", order);
			return null;
		}
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
            switch (reference.getType()) {
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

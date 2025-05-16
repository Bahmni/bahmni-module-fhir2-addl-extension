package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.translator.OrderTypeTranslator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static org.openmrs.module.fhir2.api.util.FhirUtils.getMetadataTranslation;

@Component
public class OrderTypeTranslatorImpl implements OrderTypeTranslator {
	
	@Autowired
	OrderService orderService;
	
	@Override
	public CodeableConcept toFhirResource(@Nonnull OrderType orderType) {
		
		CodeableConcept code = new CodeableConcept();
		code.addCoding().setSystem(BahmniFhirConstants.ORDER_TYPE_SYSTEM_URI).setCode(orderType.getUuid())
		        .setDisplay(getMetadataTranslation(orderType));
		code.setText(getMetadataTranslation(orderType));
		return code;
	}
	
	@Override
    public OrderType toOpenmrsType(@Nonnull CodeableConcept codeableConcept) {
        Coding orderTypeCoding = codeableConcept.getCoding().stream().filter(Coding::hasSystem)
                .filter(coding -> coding.getSystem().equals(BahmniFhirConstants.ORDER_TYPE_SYSTEM_URI))
                .findFirst().orElse(null);
        if (orderTypeCoding == null) {
            return null;
        }
        return orderService.getOrderTypeByUuid(orderTypeCoding.getCode());

    }
}

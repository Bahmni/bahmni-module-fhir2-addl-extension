package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestStatusTranslator;
import org.bahmni.module.fhir2addlextension.api.utils.ModuleUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Order;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ServiceRequestStatusTranslatorImpl implements ServiceRequestStatusTranslator {
    @Override
    public ServiceRequest.ServiceRequestStatus toFhirResource(@NonNull Order data) {
        return determineServiceRequestStatus(data);
    }

    protected ServiceRequest.ServiceRequestStatus determineServiceRequestStatus(Order order) {
        if (order.getVoided()) {
            return ServiceRequest.ServiceRequestStatus.ENTEREDINERROR;
        }
        Order.FulfillerStatus fulfillerStatus = order.getFulfillerStatus();
        if (fulfillerStatus != null) {
            switch (fulfillerStatus) {
                case COMPLETED:
                    return ServiceRequest.ServiceRequestStatus.COMPLETED;
                case RECEIVED:
                case IN_PROGRESS:
                    return ServiceRequest.ServiceRequestStatus.ACTIVE;
                case EXCEPTION:
                    return order.getDateStopped() != null ? ServiceRequest.ServiceRequestStatus.REVOKED : ServiceRequest.ServiceRequestStatus.UNKNOWN;
                default:
                    break;
            }
        }
        if (Order.Action.DISCONTINUE.equals(order.getAction())) {
            //this is a discontinued order, issued against the original order.
            //this order is active, the order.getPreviousOrder() is the original order and which has been revoked
            //determined by the date stopped. the original order action is still new
            return ServiceRequest.ServiceRequestStatus.COMPLETED;
        }
        //fulfiller status is null, so we must interpret the action, urgency and dates
        if (Order.Action.NEW.equals(order.getAction())) {
            Date currentDate = new Date();
            Date effectiveStartDate = order.getEffectiveStartDate();
            Date effectiveStopDate = order.getEffectiveStopDate();

            if (order.getDateStopped() != null) {
                return ServiceRequest.ServiceRequestStatus.REVOKED;
            }

            if (effectiveStartDate == null) {
                throw new IllegalArgumentException("Can not determine status for order with no effective start date");
            }

            int activated = ModuleUtils.compareDates(currentDate, effectiveStartDate, ChronoUnit.MINUTES); // order.isActivated(effectiveStartDate);
            if (activated < 0) {
                return ServiceRequest.ServiceRequestStatus.ACTIVE;
            }
            if (effectiveStopDate == null) {
                return ServiceRequest.ServiceRequestStatus.ACTIVE;
            }
            int comparisonResult = ModuleUtils.compareDates(effectiveStopDate, currentDate, ChronoUnit.MINUTES);
            return comparisonResult < 0 ? ServiceRequest.ServiceRequestStatus.COMPLETED : ServiceRequest.ServiceRequestStatus.ACTIVE;
        }

        return ServiceRequest.ServiceRequestStatus.UNKNOWN;
    }
}

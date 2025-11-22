package org.bahmni.module.fhir2AddlExtension.api.service;

import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderAttribute;

import javax.validation.constraints.NotNull;

public interface ServiceRequestLocationReferenceResolver {
	
	Reference getRequestedLocationReferenceForOrder(@NotNull final Order order);
	
	Location getPreferredLocation(@NotNull final Order order);
	
	OrderAttribute updateOrderRequestLocation(@NotNull final Reference locationReference, @NotNull final Order order);
	
	OrderAttribute updateOrderRequestLocation(Location preferredLocationForOrder, Order order);
	
	boolean hasRequestedLocation(Order order);
	
	OrderAttribute setOrderRequestLocation(Reference reference, Order order);
}

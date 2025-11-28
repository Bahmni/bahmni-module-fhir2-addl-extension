package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.bahmni.module.fhir2AddlExtension.api.dao.OrderAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.service.ServiceRequestLocationReferenceResolver;
import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.Order;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderType;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ServiceRequestLocationReferenceResolverImpl implements ServiceRequestLocationReferenceResolver {
	
	public static final String REQUESTED_LOCATION_FOR_ORDER = "REQUESTED_LOCATION";
	
	public static final String LOCATION_DATA_TYPE = "org.openmrs.customdatatype.datatype.LocationDatatype";
	
	private static final String NO_REQUESTED_LOCATION_ATTRIBUTE_SET = "There is no requested location attribute defined for order. Ignoring location reference";
	
	private static final String STR_INVALID_ORDER_REQUEST_LOCATION = "Could not find location for order's requested location";
	
	private static final String ERR_INVALID_ORDER_LOCATION_REFERENCE = "Invalid order location reference";
	
	private final LocationReferenceTranslator locationReferenceTranslator;
	
	private final OrderAttributeTypeDao attributeTypeDao;
	
	private final AppContext appContext;
	
	@Autowired
	public ServiceRequestLocationReferenceResolverImpl(LocationReferenceTranslator locationReferenceTranslator,
	    OrderAttributeTypeDao attributeTypeDao, AppContext appContext) {
		this.locationReferenceTranslator = locationReferenceTranslator;
		this.attributeTypeDao = attributeTypeDao;
		this.appContext = appContext;
	}
	
	@Override
    public Reference getRequestedLocationReferenceForOrder(@NotNull final Order order) {
        if (order.getActiveAttributes().isEmpty()) {
            return null;
        }
        Optional<Reference> locationReference = getRequestedLocationAttribute(order)
			.map(orderAttribute -> {
				Object value = orderAttribute.getValue();
				if (value instanceof Location) {
					Location location = (Location) orderAttribute.getValue();
					return new Reference()
						.setReference(FhirConstants.LOCATION + "/" + location.getUuid())
						.setType(FhirConstants.LOCATION)
						.setDisplay(location.getName());
				}  else {
					return null;
				}
			});
        return locationReference.orElse(null);
    }
	
	protected Optional<OrderAttribute> getRequestedLocationAttribute(Order order) {
		if (order.getActiveAttributes().isEmpty()) {
			return Optional.empty();
		}
        return order.getActiveAttributes().stream()
                .filter(orderAttribute -> {
					return orderAttribute.getAttributeType().getName().equals(REQUESTED_LOCATION_FOR_ORDER)
							&& orderAttribute.getAttributeType().getDatatypeClassname().equals(LOCATION_DATA_TYPE);
				})
                .findFirst();
    }
	
	@Override
	public OrderAttribute updateOrderRequestLocation(@NotNull final Reference reference, @NotNull final Order order) {
		OrderAttributeType attributeType = getLocationReferenceAttributeType();
		if (attributeType == null) {
			log.info(NO_REQUESTED_LOCATION_ATTRIBUTE_SET);
			return null;
		}
		
		Location location = locationReferenceTranslator.toOpenmrsType(reference);
		if (location == null) {
			log.error(STR_INVALID_ORDER_REQUEST_LOCATION);
			throw new InvalidRequestException(ERR_INVALID_ORDER_LOCATION_REFERENCE);
		}
		//Optional<String> locationUuid = FhirUtils.referenceToId(locationReference.getReference());
		
		Optional<OrderAttribute> existingAttribute = getRequestedLocationAttribute(order);
		if (existingAttribute.isPresent()) {
			return updateOrderAttribute(location, existingAttribute.get());
		}
		
		return null;
	}
	
	private OrderAttribute updateOrderAttribute(Location location, OrderAttribute existingAttribute) {
		log.info("Updating order's existing requested location attribute");
		existingAttribute.setValue(location);
		existingAttribute.setValueReferenceInternal(location.getUuid());
		return existingAttribute;
	}
	
	@Override
	public OrderAttribute updateOrderRequestLocation(Location preferredLocationForOrder, Order order) {
		Optional<OrderAttribute> existingAttribute = getRequestedLocationAttribute(order);
		if (existingAttribute.isPresent()) {
			return updateOrderAttribute(preferredLocationForOrder, existingAttribute.get());
		}
		return null;
	}
	
	@Override
	public boolean hasRequestedLocation(Order order) {
		return getRequestedLocationAttribute(order).isPresent();
	}
	
	@Override
	public OrderAttribute setOrderRequestLocation(Reference reference, Order order) {
		OrderAttributeType attributeType = getLocationReferenceAttributeType();
		if (attributeType == null) {
			log.info(NO_REQUESTED_LOCATION_ATTRIBUTE_SET);
			return null;
		}
		
		Location location = locationReferenceTranslator.toOpenmrsType(reference);
		if (location == null) {
			log.error(STR_INVALID_ORDER_REQUEST_LOCATION);
			throw new InvalidRequestException(ERR_INVALID_ORDER_LOCATION_REFERENCE);
		}
		
		log.info("Creating new order attribute for requested location");
		OrderAttribute attribute = new OrderAttribute();
		attribute.setAttributeType(attributeType);
		attribute.setValueReferenceInternal(location.getUuid());
		attribute.setValue(location);
		attribute.setDateCreated(new Date());
		attribute.setCreator(appContext.getCurrentUser());
		attribute.setVoided(false);
		order.addAttribute(attribute);
		return attribute;
	}
	
	@Override
	public Location getPreferredLocation(@NotNull final Order order) {
		if (order.getEncounter() == null || order.getEncounter().getLocation() == null) {
			return null;
		}
		Location visitLocation = ModuleUtils.getVisitLocation(order.getEncounter().getLocation());
		String locationAttributeTypeName = getLocationAttributeType(order.getOrderType());
		
		if (StringUtils.isEmpty(locationAttributeTypeName)) {
			return null;
		}
		
		for (LocationAttribute locationAttribute : visitLocation.getActiveAttributes()) {
			LocationAttributeType attributeType = locationAttribute.getAttributeType();
			if (attributeType.getRetired()) {
				continue;
			}
			if (!attributeType.getDatatypeClassname().equals(LOCATION_DATA_TYPE)) {
				continue;
			}
			if (attributeType.getName().equalsIgnoreCase(locationAttributeTypeName)) {
				Object value = locationAttribute.getValue();
				if (value instanceof Location) {
					return (Location) value;
				}
			}
		}
		return null;
	}
	
	protected OrderAttributeType getLocationReferenceAttributeType() {
		List<OrderAttributeType> orderAttributeTypes = attributeTypeDao.getOrderAttributeTypes(false);
		for (OrderAttributeType attributeType : orderAttributeTypes) {
			if (attributeType.getName().equals(REQUESTED_LOCATION_FOR_ORDER)
			        && attributeType.getDatatypeClassname().equals(LOCATION_DATA_TYPE)) {
				return attributeType;
			}
		}
		return null;
	}
	
	/**
	 * This employs a simple name matching strategy, the map above. This may be externalized or read
	 * from external configurations or a global property Subclasses may override this method if
	 * their strategy is to use Order Attribute to capture targeted location, and location lookup is
	 * based on location attribute
	 * 
	 * @param orderType
	 * @return Name of the location attribute
	 */
	protected String getLocationAttributeType(OrderType orderType) {
		return appContext.getOrderTypeToLocationAttributeNameMap().get(orderType.getName());
	}
	
}

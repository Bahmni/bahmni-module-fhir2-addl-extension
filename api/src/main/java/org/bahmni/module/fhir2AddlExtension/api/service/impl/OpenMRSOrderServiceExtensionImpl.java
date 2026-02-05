package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.bahmni.module.fhir2AddlExtension.api.service.OpenMRSOrderServiceExtension;
import org.hibernate.proxy.HibernateProxy;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.ReferralOrder;
import org.openmrs.TestOrder;
import org.openmrs.api.APIException;
import org.openmrs.api.AmbiguousOrderException;
import org.openmrs.api.CannotStopDiscontinuationOrderException;
import org.openmrs.api.CannotStopInactiveOrderException;
import org.openmrs.api.EditedOrderDoesNotMatchPreviousException;
import org.openmrs.api.MissingRequiredPropertyException;
import org.openmrs.api.OrderContext;
import org.openmrs.api.OrderEntryException;
import org.openmrs.api.OrderNumberGenerator;
import org.openmrs.api.UnchangeableObjectException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.OrderDAO;
import org.openmrs.order.OrderUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.openmrs.Order.Action.DISCONTINUE;
import static org.openmrs.Order.Action.NEW;
import static org.openmrs.Order.Action.REVISE;

/*
 * TODO: Remove this implementation once this JIRA changes are backported into 2.5.x and 2.6.x on OpenMRS Core
 * JIRA: https://openmrs.atlassian.net/browse/TRUNK-6534
 * Core PR: https://github.com/openmrs/openmrs-core/pull/5736
 * This has been done to support creation of linked orders
 * This is primarily extended to remove the validation check for orders linked with previous_order_id to have the same order concept.
 * The only addition is NEW != order.getAction() clause added in the same orderable check, rest all are just copied over from core.
 */

@Component
@Slf4j
public class OpenMRSOrderServiceExtensionImpl implements OpenMRSOrderServiceExtension, OrderNumberGenerator {
	
	protected OrderDAO dao;
	
	private static final String PARALLEL_ORDERS = "PARALLEL_ORDERS";
	
	private static OrderNumberGenerator orderNumberGenerator = null;
	
	private static final String ORDER_NUMBER_PREFIX = "ORD-";
	
	@Autowired
	public OpenMRSOrderServiceExtensionImpl(OrderDAO dao) {
		this.dao = dao;
	}
	
	@Override
	public Order validateAndSetMissingFields(Order order, OrderContext orderContext) {
		boolean isRetrospective = false;
		failOnExistingOrder(order);
		ensureDateActivatedIsSet(order);
		ensureConceptIsSet(order);
		ensureDrugOrderAutoExpirationDateIsSet(order);
		ensureOrderTypeIsSet(order, orderContext);
		ensureCareSettingIsSet(order, orderContext);
		failOnOrderTypeMismatch(order);
		
		Order previousOrder = order.getPreviousOrder();
		if (REVISE == order.getAction()) {
			if (previousOrder == null) {
				throw new MissingRequiredPropertyException("Order.previous.required", (Object[]) null);
			}
			stopOrder(previousOrder, aMomentBefore(order.getDateActivated()), isRetrospective);
		} else if (DISCONTINUE == order.getAction()) {
			discontinueExistingOrdersIfNecessary(order, isRetrospective);
		}
		
		if (previousOrder != null) {
			//concept should be the same as on previous order, same applies to drug for drug orders
			/*
			Note: This is the only addition on top of OpenMRS Order Service impl.
			 */
			if (NEW != order.getAction() && !order.hasSameOrderableAs(previousOrder)) {
				throw new EditedOrderDoesNotMatchPreviousException("Order.orderable.doesnot.match");
			} else if (!order.getOrderType().equals(previousOrder.getOrderType())) {
				throw new EditedOrderDoesNotMatchPreviousException("Order.type.doesnot.match");
			} else if (!order.getCareSetting().equals(previousOrder.getCareSetting())) {
				throw new EditedOrderDoesNotMatchPreviousException("Order.care.setting.doesnot.match");
			} else if (!getActualType(order).equals(getActualType(previousOrder))) {
				throw new EditedOrderDoesNotMatchPreviousException("Order.class.doesnot.match");
			}
		}
		
		if (DISCONTINUE != order.getAction()) {
			Date asOfDate = new Date();
			if (isRetrospective) {
				asOfDate = order.getDateActivated();
			}
			List<Order> activeOrders = getActiveOrders(order.getPatient(), null, order.getCareSetting(), asOfDate);
			List<String> parallelOrders = Collections.emptyList();
			if (orderContext != null && orderContext.getAttribute(PARALLEL_ORDERS) != null) {
				parallelOrders = Arrays.asList((String[]) orderContext.getAttribute(PARALLEL_ORDERS));
			}
			for (Order activeOrder : activeOrders) {
				//Reject if there is an active drug order for the same orderable with overlapping schedule
				if (!parallelOrders.contains(activeOrder.getUuid())
				        && areDrugOrdersOfSameOrderableAndOverlappingSchedule(order, activeOrder)) {
					throw new AmbiguousOrderException("Order.cannot.have.more.than.one");
				}
			}
		}
		if (order.getOrderId() == null) {
			setProperty(order, "orderNumber", getOrderNumberGenerator().getNewOrderNumber(orderContext));
			
			//DC orders should auto expire upon creating them
			if (DISCONTINUE == order.getAction()) {
				order.setAutoExpireDate(order.getDateActivated());
			} else if (order.getAutoExpireDate() != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(order.getAutoExpireDate());
				int hours = cal.get(Calendar.HOUR_OF_DAY);
				int minutes = cal.get(Calendar.MINUTE);
				int seconds = cal.get(Calendar.SECOND);
				cal.get(Calendar.MILLISECOND);
				//roll autoExpireDate to end of day (23:59:59) if no time portion is specified
				if (hours == 0 && minutes == 0 && seconds == 0) {
					cal.set(Calendar.HOUR_OF_DAY, 23);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					// the OpenMRS database is only precise to the second
					cal.set(Calendar.MILLISECOND, 0);
					order.setAutoExpireDate(cal.getTime());
				}
			}
		}
		return order;
	}
	
	private void failOnExistingOrder(Order order) {
		if (order.getOrderId() != null) {
			throw new UnchangeableObjectException("Order.cannot.edit.existing");
		}
	}
	
	private void ensureDateActivatedIsSet(Order order) {
		if (order.getDateActivated() == null) {
			order.setDateActivated(new Date());
		}
	}
	
	private void ensureConceptIsSet(Order order) {
		Concept concept = order.getConcept();
		if (concept == null && isDrugOrder(order)) {
			DrugOrder drugOrder = (DrugOrder) order;
			if (drugOrder.getDrug() != null) {
				concept = drugOrder.getDrug().getConcept();
				drugOrder.setConcept(concept);
			}
		}
		if (concept == null) {
			throw new MissingRequiredPropertyException("Order.concept.required");
		}
	}
	
	private void ensureDrugOrderAutoExpirationDateIsSet(Order order) {
		if (isDrugOrder(order)) {
			((DrugOrder) order).setAutoExpireDateBasedOnDuration();
		}
	}
	
	private void ensureOrderTypeIsSet(Order order, OrderContext orderContext) {
		if (order.getOrderType() != null) {
			return;
		}
		OrderType orderType = null;
		if (orderContext != null) {
			orderType = orderContext.getOrderType();
		}
		if (orderType == null) {
			orderType = getOrderTypeByConcept(order.getConcept());
		}
		if (orderType == null && order instanceof DrugOrder) {
			orderType = Context.getOrderService().getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID);
		}
		if (orderType == null && order instanceof TestOrder) {
			orderType = Context.getOrderService().getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID);
		}
		if (orderType == null && order instanceof ReferralOrder) {
			orderType = Context.getOrderService().getOrderTypeByUuid(OrderType.REFERRAL_ORDER_TYPE_UUID);
		}
		if (orderType == null) {
			throw new OrderEntryException("Order.type.cannot.determine");
		}
		Order previousOrder = order.getPreviousOrder();
		if (previousOrder != null && !orderType.equals(previousOrder.getOrderType())) {
			throw new OrderEntryException("Order.type.does.not.match");
		}
		order.setOrderType(orderType);
	}
	
	private void ensureCareSettingIsSet(Order order, OrderContext orderContext) {
		if (order.getCareSetting() != null) {
			return;
		}
		CareSetting careSetting = null;
		if (orderContext != null) {
			careSetting = orderContext.getCareSetting();
		}
		Order previousOrder = order.getPreviousOrder();
		if (careSetting == null || (previousOrder != null && !careSetting.equals(previousOrder.getCareSetting()))) {
			throw new OrderEntryException("Order.care.cannot.determine");
		}
		order.setCareSetting(careSetting);
	}
	
	private void failOnOrderTypeMismatch(Order order) {
		if (!order.getOrderType().getJavaClass().isAssignableFrom(order.getClass())) {
			throw new OrderEntryException("Order.type.class.does.not.match", new Object[] {
			        order.getOrderType().getJavaClass(), order.getClass().getName() });
		}
	}
	
	private boolean areDrugOrdersOfSameOrderableAndOverlappingSchedule(Order firstOrder, Order secondOrder) {
		return firstOrder.hasSameOrderableAs(secondOrder)
		        && !OpenmrsUtil.nullSafeEquals(firstOrder.getPreviousOrder(), secondOrder)
		        && OrderUtil.checkScheduleOverlap(firstOrder, secondOrder)
		        && firstOrder.getOrderType().equals(
		            Context.getOrderService().getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID));
	}
	
	private boolean isDrugOrder(Order order) {
		return DrugOrder.class.isAssignableFrom(getActualType(order));
	}
	
	/**
	 * Returns the class object of the specified persistent object returning the actual persistent
	 * class in case it is a hibernate proxy
	 * 
	 * @param persistentObject
	 * @return the Class object
	 */
	private Class<?> getActualType(Object persistentObject) {
		Class<?> type = persistentObject.getClass();
		if (persistentObject instanceof HibernateProxy) {
			type = ((HibernateProxy) persistentObject).getHibernateLazyInitializer().getPersistentClass();
		}
		return type;
	}
	
	/**
	 * @see org.openmrs.api.OrderService#getOrderTypeByConcept(org.openmrs.Concept)
	 */
	private OrderType getOrderTypeByConcept(Concept concept) {
		return Context.getOrderService().getOrderTypeByConceptClass(concept.getConceptClass());
	}
	
	private List<Order> getActiveOrders(Patient patient, OrderType orderType, CareSetting careSetting, Date asOfDate) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required when fetching active orders");
        }
        if (asOfDate == null) {
            asOfDate = new Date();
        }
        List<OrderType> orderTypes = null;
        if (orderType != null) {
            orderTypes = new ArrayList<>();
            orderTypes.add(orderType);
            orderTypes.addAll(getSubtypes(orderType, true));
        }
        return dao.getActiveOrders(patient, orderTypes, careSetting, asOfDate);
    }
	
	private List<OrderType> getSubtypes(OrderType orderType, boolean includeRetired) {
        List<OrderType> allSubtypes = new ArrayList<>();
        List<OrderType> immediateAncestors = dao.getOrderSubtypes(orderType, includeRetired);
        while (!immediateAncestors.isEmpty()) {
            List<OrderType> ancestorsAtNextLevel = new ArrayList<>();
            for (OrderType type : immediateAncestors) {
                allSubtypes.add(type);
                ancestorsAtNextLevel.addAll(dao.getOrderSubtypes(type, includeRetired));
            }
            immediateAncestors = ancestorsAtNextLevel;
        }
        return allSubtypes;
    }
	
	private void stopOrder(Order orderToStop, Date discontinueDate, boolean isRetrospective) {
		if (discontinueDate == null) {
			discontinueDate = new Date();
		}
		if (discontinueDate.after(new Date())) {
			throw new IllegalArgumentException("Discontinue date cannot be in the future");
		}
		if (DISCONTINUE == orderToStop.getAction()) {
			throw new CannotStopDiscontinuationOrderException();
		}
		
		if (isRetrospective && orderToStop.getDateStopped() != null) {
			throw new CannotStopInactiveOrderException();
		}
		if (!isRetrospective && !orderToStop.isActive()) {
			throw new CannotStopInactiveOrderException();
		} else if (isRetrospective && !orderToStop.isActive(discontinueDate)) {
			throw new CannotStopInactiveOrderException();
		}
		
		setProperty(orderToStop, "dateStopped", discontinueDate);
		saveOrderInternal(orderToStop);
	}
	
	private Date aMomentBefore(Date date) {
		return DateUtils.addSeconds(date, -1);
	}
	
	private void discontinueExistingOrdersIfNecessary(Order order, Boolean isRetrospective) {
		if (DISCONTINUE != order.getAction()) {
			return;
		}
		
		//Mark previousOrder as discontinued if it is not already
		Order previousOrder = order.getPreviousOrder();
		if (previousOrder != null) {
			stopOrder(previousOrder, aMomentBefore(order.getDateActivated()), isRetrospective);
			return;
		}
		
		//Mark first order found corresponding to this DC order as discontinued.
		Date asOfDate = null;
		if (isRetrospective) {
			asOfDate = order.getDateActivated();
		}
		List<? extends Order> orders = getActiveOrders(order.getPatient(), order.getOrderType(), order.getCareSetting(),
		    asOfDate);
		boolean isDrugOrderAndHasADrug = isDrugOrder(order)
		        && (((DrugOrder) order).getDrug() != null || ((DrugOrder) order).isNonCodedDrug());
		Order orderToBeDiscontinued = null;
		for (Order activeOrder : orders) {
			if (!getActualType(order).equals(getActualType(activeOrder))) {
				continue;
			}
			//For drug orders, the drug must match if the order has a drug
			if (isDrugOrderAndHasADrug) {
				Order existing = order.hasSameOrderableAs(activeOrder) ? activeOrder : null;
				if (existing != null) {
					if (orderToBeDiscontinued == null) {
						orderToBeDiscontinued = existing;
					} else {
						throw new AmbiguousOrderException("Order.discontinuing.ambiguous.orders");
					}
				}
			} else if (activeOrder.getConcept().equals(order.getConcept())) {
				if (orderToBeDiscontinued == null) {
					orderToBeDiscontinued = activeOrder;
				} else {
					throw new AmbiguousOrderException("Order.discontinuing.ambiguous.orders");
				}
			}
		}
		if (orderToBeDiscontinued != null) {
			order.setPreviousOrder(orderToBeDiscontinued);
			stopOrder(orderToBeDiscontinued, aMomentBefore(order.getDateActivated()), isRetrospective);
		}
	}
	
	private void setProperty(Order order, String propertyName, Object value) {
		Boolean isAccessible = null;
		Field field = null;
		try {
			field = Order.class.getDeclaredField(propertyName);
			field.setAccessible(true);
			field.set(order, value);
		}
		catch (Exception e) {
			throw new APIException("Order.failed.set.property", new Object[] { propertyName, order }, e);
		}
		finally {
			if (field != null && isAccessible != null) {
				field.setAccessible(isAccessible);
			}
		}
	}
	
	private OrderNumberGenerator getOrderNumberGenerator() {
		if (orderNumberGenerator == null) {
			String generatorBeanId = Context.getAdministrationService().getGlobalProperty(
			    OpenmrsConstants.GP_ORDER_NUMBER_GENERATOR_BEAN_ID);
			if (StringUtils.hasText(generatorBeanId)) {
				orderNumberGenerator = Context.getRegisteredComponent(generatorBeanId, OrderNumberGenerator.class);
				log.info("Successfully set the configured order number generator");
			} else {
				orderNumberGenerator = this;
				log.info("Setting default order number generator");
			}
		}
		
		return orderNumberGenerator;
	}
	
	private Order saveOrderInternal(Order order) {
		return dao.saveOrder(order);
	}
	
	@Override
	public String getNewOrderNumber(OrderContext orderContext) throws APIException {
		return ORDER_NUMBER_PREFIX + Context.getOrderService().getNextOrderNumberSeedSequenceValue();
	}
}

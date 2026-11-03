package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirServiceRequestDao;
import org.hibernate.Session;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Primary
public class BahmniFhirServiceRequestDaoImpl extends BahmniBaseFhirDao<Order> implements BahmniFhirServiceRequestDao<Order> {
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private OrderService orderService;
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private AppContext appContext;
	
	@Override
	public boolean hasDistinctResults() {
		return false;
	}
	
	@Override
	@Transactional(readOnly = true)
	public Order get(@Nonnull String uuid) {
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Order> cq = cb.createQuery(Order.class);
		Root<Order> root = cq.from(Order.class);
		Join<Order, OrderType> orderTypeJoin = root.join("orderType");
		cq.select(root).where(cb.equal(root.get("uuid"), uuid),
		    cb.notEqual(orderTypeJoin.get("uuid"), OrderType.DRUG_ORDER_TYPE_UUID));
		Order order = session.createQuery(cq).uniqueResult();
		return order == null ? null : deproxyResult(order);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<Order> get(@Nonnull Collection<String> uuids) {
		if (uuids == null || uuids.isEmpty()) {
			return Collections.emptyList();
		}
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Order> cq = cb.createQuery(Order.class);
		Root<Order> root = cq.from(Order.class);
		Join<Order, OrderType> otJoin = root.join("orderType");
		cq.select(root).where(root.get("uuid").in(uuids),
		    cb.notEqual(otJoin.get("uuid"), OrderType.DRUG_ORDER_TYPE_UUID), cb.equal(root.get("voided"), false));
		return session.createQuery(cq).getResultList().stream().filter(Objects::nonNull).map(this::deproxyResult)
		        .collect(Collectors.toList());
	}
	
	@Override
	public Order createOrUpdate(@Nonnull Order newEntry) {
		if (newEntry.getOrderType().getUuid().equals(OrderType.DRUG_ORDER_TYPE_UUID))
			throw new InvalidRequestException("Drug Orders cannot be submitted through ServiceRequest ");
		return orderService.saveOrder(newEntry, null);
	}
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<Order, U> ctx, SearchParameterMap theParams) {
		excludeDrugOrder(ctx);
		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(param -> getSearchQueryHelper().handleEncounterReference(ctx,
					    (ReferenceAndListParam) param.getParam(), "e"));
					break;
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(patientReference -> getSearchQueryHelper().handlePatientReference(ctx,
					    (ReferenceAndListParam) patientReference.getParam(), "patient"));
					break;
				case FhirConstants.CODED_SEARCH_HANDLER:
					entry.getValue().forEach(code -> handleCodedConcept(ctx, (TokenAndListParam) code.getParam()));
					break;
				case FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    participantReference -> getSearchQueryHelper().handleProviderReference(ctx,
					        (ReferenceAndListParam) participantReference.getParam()));
					break;
				case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    dateRangeParam -> handleServiceRequestDateRange(ctx, (DateRangeParam) dateRangeParam.getParam())
					            .ifPresent(ctx::addPredicate));
					break;
				case FhirConstants.CATEGORY_SEARCH_HANDLER:
					entry.getValue().forEach(
					    categoryReference -> handleCategoryReference(ctx, (ReferenceAndListParam) categoryReference.getParam()));
					break;
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    basedOnRef -> handleBasedOnReference(ctx, (ReferenceAndListParam) basedOnRef.getParam()));
					break;
				case BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER:
					entry.getValue().forEach(
					    locationReference -> handleLocationReference(ctx, (ReferenceAndListParam) locationReference.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, entry.getValue()).ifPresent(ctx::addPredicate);
					break;
			}
		});
	}
	
	private <U> void handleCodedConcept(OpenmrsFhirCriteriaContext<Order, U> ctx, TokenAndListParam code) {
		if (code == null) {
			return;
		}
		Join<?, ?> cJoin = ctx.getJoin("c").orElseGet(() -> ctx.addJoin("concept", "c"));
		getSearchQueryHelper().handleCodeableConcept(ctx, code, cJoin, "cm", "crt").ifPresent(ctx::addPredicate);
	}
	
	private <U> Optional<Predicate> handleServiceRequestDateRange(OpenmrsFhirCriteriaContext<Order, U> ctx,
	        DateRangeParam dateRangeParam) {
		if (dateRangeParam == null) {
			return Optional.empty();
		}
		CriteriaBuilder cb = ctx.getCriteriaBuilder();

		List<Predicate> lowerPreds = Stream
		        .of(getSearchQueryHelper().handleDate(ctx, "scheduledDate", dateRangeParam.getLowerBound()),
		            getSearchQueryHelper().handleDate(ctx, "dateActivated", dateRangeParam.getLowerBound()))
		        .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

		List<Predicate> upperPreds = Stream
		        .of(getSearchQueryHelper().handleDate(ctx, "dateStopped", dateRangeParam.getUpperBound()),
		            getSearchQueryHelper().handleDate(ctx, "autoExpireDate", dateRangeParam.getUpperBound()))
		        .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

		List<Predicate> combined = new ArrayList<>();
		if (!lowerPreds.isEmpty()) {
			combined.add(cb.or(lowerPreds.toArray(new Predicate[0])));
		}
		if (!upperPreds.isEmpty()) {
			combined.add(cb.or(upperPreds.toArray(new Predicate[0])));
		}

		if (combined.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(cb.and(combined.toArray(new Predicate[0])));
	}
	
	private <U> void handleCategoryReference(OpenmrsFhirCriteriaContext<Order, U> ctx,
	        ReferenceAndListParam categoryReference) {
		if (categoryReference == null) {
			return;
		}
		Join<?, ?> otJoin = ctx.getJoin("ot").orElseGet(() -> ctx.addJoin("orderType", "ot"));
		handleAndListParam(ctx.getCriteriaBuilder(), categoryReference,
		    token -> getSearchQueryHelper().propertyLike(ctx, otJoin, "uuid",
		        new StringParam(resolveOrderTypeUuid(token.getValue()), true))).ifPresent(ctx::addPredicate);
	}
	
	private <U> void handleBasedOnReference(OpenmrsFhirCriteriaContext<Order, U> ctx,
	        ReferenceAndListParam basedOnReference) {
		if (basedOnReference == null) {
			return;
		}
		Join<?, ?> poJoin = ctx.getJoin("po").orElseGet(() -> ctx.addJoin("previousOrder", "po"));
		handleAndListParam(ctx.getCriteriaBuilder(), basedOnReference,
		    param -> Optional.of(ctx.getCriteriaBuilder().equal(poJoin.get("uuid"), param.getValue())))
		        .ifPresent(ctx::addPredicate);
	}
	
	private String resolveOrderTypeUuid(String categoryValue) {
		Map<String, String> orderTypeNameToCategory = appContext.getOrderTypeToCategoryMap();
		String orderTypeName = orderTypeNameToCategory.entrySet().stream()
		        .filter(entry -> entry.getValue().equals(categoryValue)).map(Map.Entry::getKey).findFirst()
		        .orElse(null);
		if (orderTypeName == null) {
			return categoryValue;
		}
		OrderType orderType = orderService.getOrderTypeByName(orderTypeName);
		return orderType == null ? categoryValue : orderType.getUuid();
	}
	
	private <U> void handleLocationReference(OpenmrsFhirCriteriaContext<Order, U> ctx,
	        ReferenceAndListParam locationReference) {
		if (locationReference == null) {
			return;
		}
		Join<?, ?> eJoin = ctx.getJoin("e").orElseGet(() -> ctx.addJoin("encounter", "e"));
		Join<?, ?> lJoin = ctx.getJoin("l").orElseGet(() -> ctx.addJoin(eJoin, "location", "l"));
		handleAndListParam(ctx.getCriteriaBuilder(), locationReference,
		    token -> Optional.of(ctx.getCriteriaBuilder().equal(lJoin.get("uuid"), token.getValue())))
		        .ifPresent(ctx::addPredicate);
	}
	
	private <U> void excludeDrugOrder(OpenmrsFhirCriteriaContext<Order, U> ctx) {
		Join<?, ?> otJoin = ctx.getJoin("ot").orElseGet(() -> ctx.addJoin("orderType", "ot"));
		ctx.addPredicate(ctx.getCriteriaBuilder().notEqual(otJoin.get("uuid"), OrderType.DRUG_ORDER_TYPE_UUID));
	}
}

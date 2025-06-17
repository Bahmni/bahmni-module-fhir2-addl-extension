package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hibernate.criterion.Restrictions.*;

@Component
@Primary
public class BahmniFhirServiceRequestDaoImpl extends BahmniBaseFhirDao<Order> implements BahmniFhirServiceRequestDao<Order> {
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private OrderService orderService;
	
	@Override
	public boolean hasDistinctResults() {
		return false;
	}
	
	@Override
	public Order get(@Nonnull String uuid) {
		Criteria criteria = super.getSessionFactory().getCurrentSession().createCriteria(Order.class);
		criteria.add(eq("uuid", uuid));
		addCriteriaForDrugOrderFilter(criteria);
		Order result = (Order) criteria.uniqueResult();
		return result == null ? null : deproxyResult(result);
	}
	
	@Override
    public List<Order> get(@Nonnull Collection<String> uuids) {
        Criteria criteria = super.getSessionFactory().getCurrentSession().createCriteria(this.typeToken.getRawType());
        criteria.add(Restrictions.in("uuid", uuids));
        addCriteriaForDrugOrderFilter(criteria);
        handleVoidable(criteria);

        List<Order> results = criteria.list();

        return results.stream().filter(Objects::nonNull).map(this::deproxyResult).collect(Collectors.toList());
    }
	
	@Override
	public Order createOrUpdate(@Nonnull Order newEntry) {
		if (newEntry.getOrderType().getUuid().equals(OrderType.DRUG_ORDER_TYPE_UUID))
			throw new InvalidRequestException("Drug Orders cannot be submitted through ServiceRequest ");
		return orderService.saveOrder(newEntry, null);
	}
	
	@Override
	   protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
	       addCriteriaForDrugOrderFilter(criteria);
	       theParams.getParameters().forEach(entry -> {
	           switch (entry.getKey()) {
                case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach(
                            param -> handleEncounterReference(criteria, (ReferenceAndListParam) param.getParam(), "e"));
                    break;
                case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach(patientReference -> handlePatientReference(criteria,
                            (ReferenceAndListParam) patientReference.getParam(), "patient"));
                    break;
                case FhirConstants.CODED_SEARCH_HANDLER:
                    entry.getValue().forEach(code -> handleCodedConcept(criteria, (TokenAndListParam) code.getParam()));
                    break;
                case FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach(participantReference -> handleProviderReference(criteria,
                            (ReferenceAndListParam) participantReference.getParam()));
                    break;
                case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
                    entry.getValue().forEach(dateRangeParam -> handleDateRange((DateRangeParam) dateRangeParam.getParam())
                            .ifPresent(criteria::add));
                    break;
                case FhirConstants.CATEGORY_SEARCH_HANDLER:
                    entry.getValue().forEach(categoryReference -> handleCategoryReference(criteria, (ReferenceAndListParam) categoryReference.getParam()));
                    break;
                case FhirConstants.COMMON_SEARCH_HANDLER:
                    handleCommonSearchParameters(entry.getValue()).ifPresent(criteria::add);
                    break;
            }
        });
    }
	
	private void handleCodedConcept(Criteria criteria, TokenAndListParam code) {
        if (code != null) {
            if (lacksAlias(criteria, "c")) {
                criteria.createAlias("concept", "c");
            }

            handleCodeableConcept(criteria, code, "c", "cm", "crt").ifPresent(criteria::add);
        }
    }
	
	private Optional<Criterion> handleDateRange(DateRangeParam dateRangeParam) {
		if (dateRangeParam == null) {
			return Optional.empty();
		}
		
		return Optional.of(and(toCriteriaArray(Stream.of(Optional.of(or(toCriteriaArray(Stream.of(
		    handleDate("scheduledDate", dateRangeParam.getLowerBound()),
		    handleDate("dateActivated", dateRangeParam.getLowerBound()))))), Optional.of(or(toCriteriaArray(Stream.of(
		    handleDate("dateStopped", dateRangeParam.getUpperBound()),
		    handleDate("autoExpireDate", dateRangeParam.getUpperBound())))))))));
	}
	
	private void handleCategoryReference(Criteria criteria, ReferenceAndListParam categoryReference) {
        if (categoryReference == null)
            return;
        if (lacksAlias(criteria, "ot"))
            criteria.createAlias("orderType", "ot");

        handleAndListParam(categoryReference, token -> propertyLike("ot.uuid", new StringParam(token.getValue(), true))).ifPresent(criteria::add);

    }
	
	private void addCriteriaForDrugOrderFilter(Criteria criteria) {
		if (lacksAlias(criteria, "ot")) {
			criteria.createAlias("orderType", "ot");
		}
		criteria.add(ne("ot.uuid", OrderType.DRUG_ORDER_TYPE_UUID));
	}
	
}

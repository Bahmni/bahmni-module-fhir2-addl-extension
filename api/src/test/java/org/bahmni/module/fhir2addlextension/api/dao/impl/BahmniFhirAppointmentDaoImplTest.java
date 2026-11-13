package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirAppointmentDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Dataset: A1 (patient 2, Scheduled, Jan), A2 (patient 2, Completed, Feb), A3 (patient 6,
 * Cancelled, Mar), A4 (patient 6, Missed, Apr), A5 (patient 7, CheckedIn, May), A6 (patient 7,
 * WaitList, Jun), A7 (patient 8, Requested, Jul) and a voided Scheduled appointment of patient 2
 * (Aug).
 */
public class BahmniFhirAppointmentDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirAppointmentDaoImplTest_data.xml";
	
	private static final String A1 = "appt-9001-0000-0000-0000-000000000001";
	
	private static final String A2 = "appt-9002-0000-0000-0000-000000000002";
	
	private static final String A3 = "appt-9003-0000-0000-0000-000000000003";
	
	private static final String A4 = "appt-9004-0000-0000-0000-000000000004";
	
	private static final String A5 = "appt-9005-0000-0000-0000-000000000005";
	
	private static final String A6 = "appt-9006-0000-0000-0000-000000000006";
	
	private static final String A7 = "appt-9007-0000-0000-0000-000000000007";
	
	private static final String PATIENT_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PATIENT_6 = "a7e04421-525f-442f-8138-05b619d16def";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	@Autowired
	private BahmniFhirAppointmentDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void search_withoutParameters_returnsAllNonVoidedAppointments() {
		assertThat(uuids(new SearchParameterMap()), containsInAnyOrder(A1, A2, A3, A4, A5, A6, A7));
	}
	
	@Test
	public void search_byPatient_returnsOnlyThatPatientsNonVoidedAppointments() {
		assertThat(uuids(patientSearch(PATIENT_2)), containsInAnyOrder(A1, A2));
		assertThat(uuids(patientSearch(PATIENT_6)), containsInAnyOrder(A3, A4));
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(uuids(patientSearch("00000000-0000-0000-0000-000000000000")), empty());
	}
	
	@Test
	public void search_byStatusBooked_returnsScheduledAppointmentsOnly() {
		assertThat(uuids(statusSearch("booked")), contains(A1));
	}
	
	@Test
	public void search_byEachFhirStatus_mapsToTheMatchingBahmniStatus() {
		assertThat(uuids(statusSearch("fulfilled")), contains(A2));
		assertThat(uuids(statusSearch("cancelled")), contains(A3));
		assertThat(uuids(statusSearch("noshow")), contains(A4));
		assertThat(uuids(statusSearch("checked-in")), contains(A5));
		assertThat(uuids(statusSearch("waitlist")), contains(A6));
		assertThat(uuids(statusSearch("pending")), contains(A7));
	}
	
	@Test
	public void search_byStatusCode_isCaseInsensitive() {
		assertThat(uuids(statusSearch("BOOKED")), contains(A1));
	}
	
	@Test
	public void search_byStatusOrList_returnsAppointmentsOfEitherStatus() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenOrListParam().add("booked").add("cancelled")));
		assertThat(uuids(params), containsInAnyOrder(A1, A3));
	}
	
	@Test
	public void search_byTwoStatusesInAndList_returnsNothing() {
		SearchParameterMap params = new SearchParameterMap().addParameter(
		    FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenOrListParam().add("booked")).addAnd(
		        new TokenOrListParam().add("cancelled")));
		assertThat(uuids(params), empty());
	}
	
	@Test
	public void search_byUnrecognisedStatusCode_isIgnoredAndDoesNotRestrictResults() {
		assertThat(uuids(statusSearch("not-a-status")), containsInAnyOrder(A1, A2, A3, A4, A5, A6, A7));
	}
	
	@Test
	public void search_byStatusTokenWithoutValue_doesNotRestrictResults() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam())));
		assertThat(uuids(params), containsInAnyOrder(A1, A2, A3, A4, A5, A6, A7));
	}
	
	@Test
	public void search_byUnrecognisedStatusOrWithValidStatus_restrictsByTheValidOne() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenOrListParam().add("not-a-status").add("booked")));
		assertThat(uuids(params), contains(A1));
	}
	
	@Test
	public void search_withNullStatusAndDateRangeValues_areIgnored() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.STATUS_SEARCH_HANDLER, null);
		params.addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, null);
		assertThat(uuids(params), containsInAnyOrder(A1, A2, A3, A4, A5, A6, A7));
	}
	
	@Test
	public void search_byDateRangeWithBothBounds_returnsAppointmentsStartingInsideRange() {
		DateRangeParam range = new DateRangeParam().setLowerBound("2024-02-01").setUpperBound("2024-03-31");
		assertThat(uuids(dateSearch(range)), containsInAnyOrder(A2, A3));
	}
	
	@Test
	public void search_byDateRangeWithOnlyLowerBound_returnsAppointmentsFromThatDate() {
		DateRangeParam range = new DateRangeParam().setLowerBound("2024-06-01");
		assertThat(uuids(dateSearch(range)), containsInAnyOrder(A6, A7));
	}
	
	@Test
	public void search_byDateRangeWithOnlyUpperBound_returnsAppointmentsUntilThatDate() {
		DateRangeParam range = new DateRangeParam().setUpperBound("2024-02-28");
		assertThat(uuids(dateSearch(range)), containsInAnyOrder(A1, A2));
	}
	
	@Test
	public void search_byDateRangeWithNoMatch_returnsNothing() {
		DateRangeParam range = new DateRangeParam().setLowerBound("2030-01-01").setUpperBound("2030-12-31");
		assertThat(uuids(dateSearch(range)), empty());
	}
	
	@Test
	public void search_byPatientStatusAndDate_returnsIntersection() {
		SearchParameterMap params = patientSearch(PATIENT_2).addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenOrListParam().add("fulfilled")));
		assertThat(uuids(params), contains(A2));
		
		params.addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, new DateRangeParam().setUpperBound("2024-01-31"));
		assertThat(uuids(params), empty());
	}
	
	@Test
	public void search_byCommonIdParameter_returnsOnlyMatchingAppointment() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, new TokenAndListParam().addAnd(new TokenParam(A5)));
		assertThat(uuids(params), contains(A5));
	}
	
	@Test
	public void search_sortedByDateAscending_ordersByStartDateTime() {
		assertThat(uuids(sorted("date", SortOrderEnum.ASC)), contains(A1, A2, A3, A4, A5, A6, A7));
	}
	
	@Test
	public void search_sortedByDateDescending_ordersByStartDateTimeReversed() {
		assertThat(uuids(sorted("date", SortOrderEnum.DESC)), contains(A7, A6, A5, A4, A3, A2, A1));
	}
	
	@Test
	public void search_sortedByPatient_groupsAppointmentsByPatient() {
		List<String> uuids = uuids(sorted("patient", SortOrderEnum.ASC));
		// patient ids: 2 (A1,A2), 6 (A3,A4), 7 (A5,A6), 8 (A7)
		assertThat(uuids.subList(0, 2), containsInAnyOrder(A1, A2));
		assertThat(uuids.subList(2, 4), containsInAnyOrder(A3, A4));
		assertThat(uuids.subList(4, 6), containsInAnyOrder(A5, A6));
		assertThat(uuids.get(6), equalTo(A7));
	}
	
	@Test
	public void search_sortedByStatus_ordersByStatusName() {
		// statuses stored by name: Cancelled, CheckedIn, Completed, Missed, Requested, Scheduled, WaitList
		assertThat(uuids(sorted("status", SortOrderEnum.ASC)), contains(A3, A5, A2, A4, A7, A1, A6));
	}
	
	@Test
	public void search_sortedByUnknownParameter_stillReturnsAllMatches() {
		assertThat(uuids(sorted("not-sortable", SortOrderEnum.ASC)), containsInAnyOrder(A1, A2, A3, A4, A5, A6, A7));
	}
	
	@Test
	public void search_withPaging_returnsRequestedSliceOfSortedResults() {
		SearchParameterMap params = sorted("date", SortOrderEnum.ASC);
		params.setFromIndex(2);
		params.setToIndex(5);
		assertThat(uuids(params), contains(A3, A4, A5));
	}
	
	@Test
	public void getSearchResultsCount_countsDistinctMatchingAppointments() {
		assertThat(dao.getSearchResultsCount(new SearchParameterMap()), equalTo(7));
		assertThat(dao.getSearchResultsCount(patientSearch(PATIENT_7)), equalTo(2));
	}
	
	@Test
	public void get_byUuid_returnsAppointmentWithPatientAndStatus() {
		Appointment appointment = dao.get(A4);
		assertThat(appointment.getPatient().getUuid(), equalTo(PATIENT_6));
		assertThat(appointment.getStatus(), equalTo(org.openmrs.module.appointments.model.AppointmentStatus.Missed));
	}
	
	private SearchParameterMap patientSearch(String patientUuid) {
		return new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(patientUuid))));
	}
	
	private SearchParameterMap statusSearch(String code) {
		return new SearchParameterMap().addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenOrListParam().add(code)));
	}
	
	private SearchParameterMap dateSearch(DateRangeParam range) {
		return new SearchParameterMap().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, range);
	}
	
	private SearchParameterMap sorted(String param, SortOrderEnum order) {
		return new SearchParameterMap().setSortSpec(new SortSpec(param, order));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(Appointment::getUuid).collect(Collectors.toList());
	}
}

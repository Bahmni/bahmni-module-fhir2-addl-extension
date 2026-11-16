package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

public class BahmniFhirEpisodeOfCareDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirEpisodeOfCareDaoImplTest_data.xml";
	
	private static final String EOC_1 = "eoc-9001-0000-0000-0000-000000000001";
	
	private static final String EOC_2 = "eoc-9002-0000-0000-0000-000000000002";
	
	private static final String EOC_3 = "eoc-9003-0000-0000-0000-000000000003";
	
	private static final String PATIENT_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PATIENT_6 = "a7e04421-525f-442f-8138-05b619d16def";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	@Autowired
	private BahmniFhirEpisodeOfCareDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void search_withoutParameters_returnsAllNonVoidedEpisodes() {
		assertThat(uuids(new SearchParameterMap()), containsInAnyOrder(EOC_1, EOC_2, EOC_3));
	}
	
	@Test
	public void search_byPatient_returnsOnlyEpisodesOfThatPatient() {
		assertThat(uuids(patientSearch(PATIENT_7)), containsInAnyOrder(EOC_2, EOC_3));
		assertThat(uuids(patientSearch(PATIENT_2)), contains(EOC_1));
	}
	
	@Test
	public void search_byPatientWhoseOnlyEpisodeIsVoided_returnsNothing() {
		assertThat(uuids(patientSearch(PATIENT_6)), empty());
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(uuids(patientSearch("00000000-0000-0000-0000-000000000000")), empty());
	}
	
	@Test
	public void search_byEitherPatientInOrList_returnsEpisodesOfBothPatients() {
		ReferenceOrListParam or = new ReferenceOrListParam().add(new ReferenceParam(PATIENT_2)).add(
		    new ReferenceParam(PATIENT_6));
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(or));
		assertThat(uuids(params), contains(EOC_1));
	}
	
	@Test
	public void search_byTwoDifferentPatientsInAndList_returnsNothing() {
		SearchParameterMap params = new SearchParameterMap().addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(PATIENT_2))).addAnd(
		        new ReferenceOrListParam().add(new ReferenceParam(PATIENT_7))));
		assertThat(uuids(params), empty());
	}
	
	@Test
	public void search_byCommonIdParameter_returnsOnlyMatchingEpisode() {
		assertThat(uuids(idSearch(new SearchParameterMap(), EOC_2)), contains(EOC_2));
	}
	
	@Test
	public void search_byPatientAndCommonId_appliesBothFilters() {
		assertThat(uuids(idSearch(patientSearch(PATIENT_7), EOC_3)), contains(EOC_3));
		assertThat(uuids(idSearch(patientSearch(PATIENT_7), EOC_1)), empty());
	}
	
	@Test
	public void get_byUuid_returnsEpisodeWithItsPatientAndEncounters() {
		Episode episode = dao.get(EOC_2);
		assertThat(episode.getPatient().getUuid(), equalTo(PATIENT_7));
		assertThat(episode.getEncounters(), hasSize(3));
	}
	
	@Test
	public void get_unknownUuid_returnsNull() {
		assertThat(dao.get("does-not-exist"), nullValue());
	}
	
	private SearchParameterMap patientSearch(String patientUuid) {
		return new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(patientUuid))));
	}
	
	private SearchParameterMap idSearch(SearchParameterMap params, String uuid) {
		return params.addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY,
		    new TokenAndListParam().addAnd(new TokenParam(uuid)));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(Episode::getUuid).collect(Collectors.toList());
	}
}

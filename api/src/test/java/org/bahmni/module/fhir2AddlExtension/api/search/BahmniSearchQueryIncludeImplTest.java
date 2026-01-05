package org.bahmni.module.fhir2AddlExtension.api.search;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareEncounterService;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniSearchQueryIncludeImplTest {
	
	@Mock
	private BahmniFhirImagingStudyService imagingStudyService;
	
	@Mock
	private BahmniFhirEpisodeOfCareEncounterService episodeOfCareEncounterService;
	
	@Mock
	private IBundleProvider mockBundleProvider;
	
	@InjectMocks
	private BahmniSearchQueryIncludeImpl<IBaseResource> searchQueryInclude;
	
	private ReferenceAndListParam serviceRequestReferences;
	
	private ReferenceAndListParam episodeOfCareReferences;
	
	@Before
	public void setUp() {
		serviceRequestReferences = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("ServiceRequest", null, "SR-1"));
		orListParam.add(new ReferenceParam("ServiceRequest", null, "SR-2"));
		serviceRequestReferences.addValue(orListParam);
		
		episodeOfCareReferences = new ReferenceAndListParam();
		ReferenceOrListParam eocOrListParam = new ReferenceOrListParam();
		eocOrListParam.add(new ReferenceParam("EpisodeOfCare", null, "EOC-1"));
		episodeOfCareReferences.addValue(eocOrListParam);
	}
	
	@Test
	public void handleRevIncludeParam_shouldHandleImagingStudyBasedOn() {
		Include revInclude = new Include("ImagingStudy:basedon", true);

		when(imagingStudyService.searchImagingStudy(any(BahmniImagingStudySearchParams.class)))
		        .thenReturn(mockBundleProvider);

		IBundleProvider result = searchQueryInclude.handleRevIncludeParam(new HashSet<>(),
		    Collections.singleton(revInclude), serviceRequestReferences, revInclude);

		assertThat(result, notNullValue());

		ArgumentCaptor<BahmniImagingStudySearchParams> paramsCaptor = ArgumentCaptor
		        .forClass(BahmniImagingStudySearchParams.class);
		verify(imagingStudyService).searchImagingStudy(paramsCaptor.capture());

		BahmniImagingStudySearchParams capturedParams = paramsCaptor.getValue();
		assertThat(capturedParams.getBasedOnReference(), equalTo(serviceRequestReferences));
		assertThat(capturedParams.getPatientReference(), nullValue());
	}
	
	@Test
	public void handleRevIncludeParam_shouldHandleImagingStudyBasedOnWhenThereIsNoImagingStudy() {
		Include revInclude = new Include("ImagingStudy:basedon", true);

		when(imagingStudyService.searchImagingStudy(any(BahmniImagingStudySearchParams.class)))
		        .thenReturn(mockBundleProvider);

		IBundleProvider result = searchQueryInclude.handleRevIncludeParam(new HashSet<>(),
		    Collections.singleton(revInclude), serviceRequestReferences, revInclude);

		assertThat(result, notNullValue());
		verify(imagingStudyService).searchImagingStudy(any(BahmniImagingStudySearchParams.class));
	}
	
	@Test
	public void handleRevIncludeParam_shouldHandleEncounterEpisodeOfCare() {
		Include revInclude = new Include("Encounter:episode-of-care", true);

		when(episodeOfCareEncounterService.encountersForEpisodes(any(ReferenceAndListParam.class)))
		        .thenReturn(mockBundleProvider);

		IBundleProvider result = searchQueryInclude.handleRevIncludeParam(new HashSet<>(),
		    Collections.singleton(revInclude), episodeOfCareReferences, revInclude);

		assertThat(result, notNullValue());

		ArgumentCaptor<ReferenceAndListParam> paramsCaptor = ArgumentCaptor.forClass(ReferenceAndListParam.class);
		verify(episodeOfCareEncounterService).encountersForEpisodes(paramsCaptor.capture());
		assertThat(paramsCaptor.getValue(), equalTo(episodeOfCareReferences));
	}
	
	@Test
	public void handleRevIncludeParam_shouldNotCallImagingStudyServiceForUnsupportedParameter() {
		Include revInclude = new Include("ImagingStudy:patient", true);

		searchQueryInclude.handleRevIncludeParam(new HashSet<>(), Collections.singleton(revInclude),
				serviceRequestReferences, revInclude);

		verify(imagingStudyService, never()).searchImagingStudy(any());
	}
	
	@Test
	public void handleRevIncludeParam_shouldNotCallEncounterServiceForUnsupportedParameter() {
		Include revInclude = new Include("Encounter:subject", true);

		searchQueryInclude.handleRevIncludeParam(new HashSet<>(), Collections.singleton(revInclude),
				episodeOfCareReferences, revInclude);

		verify(episodeOfCareEncounterService, never()).encountersForEpisodes(any());
	}
	
	@Test
	public void handleRevIncludeParam_shouldNotCallServicesForUnsupportedResourceType() {
		Include revInclude = new Include("Observation:subject", true);

		searchQueryInclude.handleRevIncludeParam(new HashSet<>(), Collections.singleton(revInclude),
				serviceRequestReferences, revInclude);

		verify(imagingStudyService, never()).searchImagingStudy(any());
		verify(episodeOfCareEncounterService, never()).encountersForEpisodes(any());
	}
}

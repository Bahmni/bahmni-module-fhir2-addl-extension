package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirImagingStudyService;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniImagingStudyR4ResourceProviderTest {
	
	private static final String IMAGING_STUDY_UUID = "test-imaging-study-uuid";
	
	@Mock
	private BahmniFhirImagingStudyService fhirImagingStudyService;
	
	@Mock
	private IBundleProvider bundleProvider;
	
	@Mock
	private RequestDetails requestDetails;
	
	@Captor
	private ArgumentCaptor<BahmniImagingStudySearchParams> searchParamsCaptor;
	
	private BahmniImagingStudyR4ResourceProvider resourceProvider;
	
	@Before
	public void setUp() {
		resourceProvider = new BahmniImagingStudyR4ResourceProvider(fhirImagingStudyService);
	}
	
	@Test
	public void testGetResourceType() {
		assertEquals(ImagingStudy.class, resourceProvider.getResourceType());
	}
	
	@Test
	public void testCreateImagingStudy() {
		ImagingStudy imagingStudy = createTestImagingStudy(null);
		ImagingStudy createdStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		
		when(fhirImagingStudyService.create(imagingStudy)).thenReturn(createdStudy);
		
		MethodOutcome result = resourceProvider.createImagingStudy(imagingStudy);
		
		assertNotNull(result);
		assertNotNull(result.getResource());
		assertEquals(IMAGING_STUDY_UUID, ((ImagingStudy) result.getResource()).getId());
		verify(fhirImagingStudyService).create(imagingStudy);
	}
	
	@Test
	public void testUpdateImagingStudy() {
		ImagingStudy imagingStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		ImagingStudy updatedStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		updatedStudy.setDescription("Updated description");
		
		when(fhirImagingStudyService.update(IMAGING_STUDY_UUID, imagingStudy)).thenReturn(updatedStudy);
		
		MethodOutcome result = resourceProvider.updateImagingStudy(new IdType(IMAGING_STUDY_UUID), imagingStudy);
		
		assertNotNull(result);
		assertNotNull(result.getResource());
		assertEquals("Updated description", ((ImagingStudy) result.getResource()).getDescription());
		verify(fhirImagingStudyService).update(IMAGING_STUDY_UUID, imagingStudy);
	}
	
	@Test
	public void testUpdateImagingStudyWithoutId_shouldThrowException() {
		ImagingStudy imagingStudy = createTestImagingStudy(null);
		
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.updateImagingStudy(null, imagingStudy);
		});
	}
	
	@Test
	public void testPatchImagingStudy() {
		ImagingStudy patchedStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		String patchBody = "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"available\"}]";
		
		when(
		    fhirImagingStudyService.patch(eq(IMAGING_STUDY_UUID), eq(PatchTypeEnum.JSON_PATCH), eq(patchBody),
		        eq(requestDetails))).thenReturn(patchedStudy);
		
		MethodOutcome result = resourceProvider.patchImagingStudy(new IdType(IMAGING_STUDY_UUID), PatchTypeEnum.JSON_PATCH,
		    patchBody, requestDetails);
		
		assertNotNull(result);
		assertNotNull(result.getResource());
		verify(fhirImagingStudyService).patch(IMAGING_STUDY_UUID, PatchTypeEnum.JSON_PATCH, patchBody, requestDetails);
	}
	
	@Test
	public void testPatchImagingStudyWithoutId_shouldThrowException() {
		String patchBody = "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"available\"}]";
		
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.patchImagingStudy(null, PatchTypeEnum.JSON_PATCH, patchBody, requestDetails);
		});
	}
	
	@Test
	public void testGetImagingStudyByUuidWithValidId() {
		ImagingStudy expectedStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		
		when(fhirImagingStudyService.get(IMAGING_STUDY_UUID)).thenReturn(expectedStudy);
		
		ImagingStudy result = resourceProvider.getImagingStudyByUuid(new IdType(IMAGING_STUDY_UUID));
		
		assertNotNull(result);
		assertEquals(IMAGING_STUDY_UUID, result.getId());
		verify(fhirImagingStudyService).get(IMAGING_STUDY_UUID);
	}
	
	@Test
	public void testGetImagingStudyByUuidWithInvalidId_shouldThrowException() {
		when(fhirImagingStudyService.get(IMAGING_STUDY_UUID)).thenReturn(null);
		
		assertThrows(ResourceNotFoundException.class, () -> {
			resourceProvider.getImagingStudyByUuid(new IdType(IMAGING_STUDY_UUID));
		});
	}
	
	@Test
	public void testSearchImagingStudyWithPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam();
		
		when(fhirImagingStudyService.searchImagingStudy(any(BahmniImagingStudySearchParams.class))).thenReturn(
		    bundleProvider);
		
		IBundleProvider result = resourceProvider.searchImagingStudy(patientReference, null, null, null, null);
		
		assertNotNull(result);
		assertEquals(bundleProvider, result);
		verify(fhirImagingStudyService).searchImagingStudy(searchParamsCaptor.capture());
		
		BahmniImagingStudySearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchImagingStudyWithBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam();
		
		when(fhirImagingStudyService.searchImagingStudy(any(BahmniImagingStudySearchParams.class))).thenReturn(
		    bundleProvider);
		
		IBundleProvider result = resourceProvider.searchImagingStudy(null, basedOnReference, null, null, null);
		
		assertNotNull(result);
		verify(fhirImagingStudyService).searchImagingStudy(searchParamsCaptor.capture());
		
		BahmniImagingStudySearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchImagingStudyWithId() {
		TokenAndListParam id = new TokenAndListParam();
		
		when(fhirImagingStudyService.searchImagingStudy(any(BahmniImagingStudySearchParams.class))).thenReturn(
		    bundleProvider);
		
		IBundleProvider result = resourceProvider.searchImagingStudy(null, null, id, null, null);
		
		assertNotNull(result);
		verify(fhirImagingStudyService).searchImagingStudy(searchParamsCaptor.capture());
		
		BahmniImagingStudySearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchImagingStudyWithAllParameters() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam();
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam();
		TokenAndListParam id = new TokenAndListParam();
		DateRangeParam lastUpdated = new DateRangeParam();
		SortSpec sort = new SortSpec("_lastUpdated");
		
		when(fhirImagingStudyService.searchImagingStudy(any(BahmniImagingStudySearchParams.class))).thenReturn(
		    bundleProvider);
		
		IBundleProvider result = resourceProvider.searchImagingStudy(patientReference, basedOnReference, id, lastUpdated,
		    sort);
		
		assertNotNull(result);
		verify(fhirImagingStudyService).searchImagingStudy(searchParamsCaptor.capture());
		
		BahmniImagingStudySearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSubmitQualityAssessment() {
		ImagingStudy imagingStudy = createTestImagingStudy(null);
		ImagingStudy resultStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		
		when(fhirImagingStudyService.submitQualityAssessment(imagingStudy)).thenReturn(resultStudy);
		
		ImagingStudy result = resourceProvider.submitQualityAssessment(new IdType(IMAGING_STUDY_UUID), imagingStudy);
		
		assertNotNull(result);
		assertEquals(IMAGING_STUDY_UUID, result.getId());
		verify(fhirImagingStudyService).submitQualityAssessment(imagingStudy);
	}
	
	@Test
	public void testSubmitQualityAssessmentWithoutId_shouldThrowException() {
		ImagingStudy imagingStudy = createTestImagingStudy(null);
		
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.submitQualityAssessment(null, imagingStudy);
		});
	}
	
	@Test
	public void testFetchQualityAssessment() {
		ImagingStudy expectedStudy = createTestImagingStudy(IMAGING_STUDY_UUID);
		
		when(fhirImagingStudyService.fetchWithQualityAssessment(IMAGING_STUDY_UUID)).thenReturn(expectedStudy);
		
		ImagingStudy result = resourceProvider.fetchQualityAssessment(new IdType(IMAGING_STUDY_UUID));
		
		assertNotNull(result);
		assertEquals(IMAGING_STUDY_UUID, result.getId());
		verify(fhirImagingStudyService).fetchWithQualityAssessment(IMAGING_STUDY_UUID);
	}
	
	@Test
	public void testFetchQualityAssessmentWithoutId_shouldThrowException() {
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.fetchQualityAssessment(null);
		});
	}
	
	private ImagingStudy createTestImagingStudy(String id) {
		ImagingStudy study = new ImagingStudy();
		if (id != null) {
			study.setId(id);
		}
		study.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		study.setDescription("Test imaging study");
		return study;
	}
	
	@Test
	public void testSubmitQualityAssessmentWithNullIdPart_shouldThrowException() {
		ImagingStudy imagingStudy = createTestImagingStudy(null);
		IdType idType = new IdType();
		idType.setValue(null);
		
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.submitQualityAssessment(idType, imagingStudy);
		});
	}
	
	@Test
	public void testFetchQualityAssessmentWithNullIdPart_shouldThrowException() {
		IdType idType = new IdType();
		idType.setValue(null);
		
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.fetchQualityAssessment(idType);
		});
	}
}

package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference.FhirDocumentReferenceDocStatus;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference.FhirDocumentReferenceStatus;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceContent;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DocumentReferenceDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/DocumentReferenceDaoImplTest_data.xml";
	
	private static final String DOC_1 = "doc-9001-0000-0000-0000-000000000001";
	
	private static final String DOC_2 = "doc-9002-0000-0000-0000-000000000002";
	
	private static final String DOC_3 = "doc-9003-0000-0000-0000-000000000003";
	
	private static final String DOC_4 = "doc-9004-0000-0000-0000-000000000004";
	
	private static final String PATIENT_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PATIENT_6 = "a7e04421-525f-442f-8138-05b619d16def";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String ENCOUNTER_3 = "6519d653-393b-4118-9c83-a3715b82d4ac";
	
	private static final String ENCOUNTER_4 = "eec646cb-c847-45a7-98bc-91c8c4f70add";
	
	private static final String ENCOUNTER_6 = "y403fafb-e5e4-42d0-9d11-4f52e89d123r";
	
	private static final String SSTRM = "urn:test:sstrm";
	
	private static final String SNOMED = "http://snomed.info/sct";
	
	@Autowired
	private DocumentReferenceDao dao;
	
	@Autowired
	private DocumentReferenceAttributeTypeDao attributeTypeDao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void search_withoutParameters_returnsAllNonVoidedDocumentReferences() {
		assertThat(uuids(map()), containsInAnyOrder(DOC_1, DOC_2, DOC_3, DOC_4));
	}
	
	@Test
	public void search_byPatient_returnsOnlyThatPatientsDocuments() {
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_2))),
		    containsInAnyOrder(DOC_1, DOC_2));
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_7))),
		    containsInAnyOrder(DOC_3, DOC_4));
	}
	
	@Test
	public void search_byPatient_doesNotReturnVoidedDocument() {
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_6))), empty());
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(
		    uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		        reference("00000000-0000-0000-0000-000000000000"))), empty());
	}
	
	@Test
	public void search_byEncounter_returnsOnlyDocumentsOfThatEncounter() {
		assertThat(uuids(map().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, reference(ENCOUNTER_6))),
		    containsInAnyOrder(DOC_1, DOC_2));
		assertThat(uuids(map().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, reference(ENCOUNTER_3))),
		    contains(DOC_3));
	}
	
	@Test
	public void search_byEncounterOrList_returnsUnion() {
		ReferenceOrListParam or = new ReferenceOrListParam().add(new ReferenceParam(ENCOUNTER_3)).add(
		    new ReferenceParam(ENCOUNTER_4));
		assertThat(
		    uuids(map().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		        new ReferenceAndListParam().addAnd(or))), containsInAnyOrder(DOC_3, DOC_4));
	}
	
	@Test
	public void search_byPatientAndEncounter_combinesWithAnd() {
		SearchParameterMap matching = map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    reference(PATIENT_7)).addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, reference(ENCOUNTER_4));
		assertThat(uuids(matching), contains(DOC_4));
		
		SearchParameterMap contradicting = map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    reference(PATIENT_2)).addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, reference(ENCOUNTER_4));
		assertThat(uuids(contradicting), empty());
	}
	
	@Test
	public void search_byDocTypeSystemAndCode_returnsDocumentsWhoseTypeHasThatMapping() {
		assertThat(uuids(code(new TokenParam(SSTRM, "WGT234"))), containsInAnyOrder(DOC_1, DOC_3));
		assertThat(uuids(code(new TokenParam(SSTRM, "CD41003"))), containsInAnyOrder(DOC_2, DOC_4));
	}
	
	@Test
	public void search_byDocTypeCodeWithOtherSystem_usesTheSystemToDisambiguate() {
		assertThat(uuids(code(new TokenParam(SNOMED, "2332523"))), containsInAnyOrder(DOC_1, DOC_3));
		assertThat(uuids(code(new TokenParam(SNOMED, "WGT234"))), empty());
	}
	
	@Test
	public void search_byDocTypeUnknownCode_returnsNothing() {
		assertThat(uuids(code(new TokenParam(SSTRM, "NOPE"))), empty());
	}
	
	@Test
	public void search_byDocTypeConceptUuid_matchesConceptDirectly() {
		assertThat(uuids(code(new TokenParam(null, "c607c80f-1ea9-4da3-bb88-6276ce8868dd"))),
		    containsInAnyOrder(DOC_1, DOC_3));
	}
	
	@Test
	public void search_byDocTypeOrList_returnsUnion() {
		TokenAndListParam params = new TokenAndListParam().addAnd(new TokenOrListParam()
		        .add(new TokenParam(SSTRM, "WGT234")).add(new TokenParam(SSTRM, "CD41003")));
		assertThat(uuids(map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, params)),
		    containsInAnyOrder(DOC_1, DOC_2, DOC_3, DOC_4));
	}
	
	@Test
	public void search_byDocTypeAndList_requiresEveryCodeToMatchTheSameDocType() {
		TokenAndListParam sameConcept = new TokenAndListParam().addAnd(new TokenParam(SSTRM, "WGT234")).addAnd(
		    new TokenParam(SSTRM, "WGT234"));
		assertThat(uuids(map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, sameConcept)),
		    containsInAnyOrder(DOC_1, DOC_3));
		
		TokenAndListParam differentConcepts = new TokenAndListParam().addAnd(new TokenParam(SSTRM, "WGT234")).addAnd(
		    new TokenParam(SSTRM, "CD41003"));
		assertThat(uuids(map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, differentConcepts)), empty());
	}
	
	@Test
	public void search_withUnsupportedSearchHandler_ignoresItAndReturnsUnfilteredResults() {
		SearchParameterMap params = map().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenParam("F")));
		assertThat(uuids(params), containsInAnyOrder(DOC_1, DOC_2, DOC_3, DOC_4));
	}
	
	@Test
	public void search_byDocTypeAndPatient_combinesBothFilters() {
		SearchParameterMap params = code(new TokenParam(SSTRM, "WGT234")).addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_7));
		assertThat(uuids(params), contains(DOC_3));
	}
	
	@Test
	public void search_byIdProperty_returnsTheDocumentWithThatUuid() {
		SearchParameterMap params = map().addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY,
		    new TokenAndListParam().addAnd(new TokenParam(DOC_2)));
		assertThat(uuids(params), contains(DOC_2));
	}
	
	@Test
	public void search_byLastUpdated_filtersOnCreationDateWhenNeverChanged() {
		DateRangeParam range = new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2024-03-01"),
		        new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2024-03-31"));
		SearchParameterMap params = map().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.LAST_UPDATED_PROPERTY, range);
		assertThat(uuids(params), contains(DOC_3));
	}
	
	@Test
	public void search_pagination_returnsRequestedWindowAndCountIgnoresIt() {
		SearchParameterMap params = map();
		params.setFromIndex(1);
		params.setToIndex(3);
		assertThat(dao.getSearchResults(params), hasSize(2));
		assertThat(dao.getSearchResultsCount(map()), equalTo(4));
		assertThat(dao.getSearchResultsCount(code(new TokenParam(SSTRM, "WGT234"))), equalTo(2));
	}
	
	@Test
	public void get_byUuid_returnsFullyMappedDocumentWithChildren() {
		FhirDocumentReference doc = dao.get(DOC_1);

		assertThat(doc, notNullValue());
		assertThat(doc.getSubject().getUuid(), equalTo(PATIENT_2));
		assertThat(doc.getEncounter().getUuid(), equalTo(ENCOUNTER_6));
		assertThat(doc.getOrder().getUuid(), equalTo("dfca4077-493c-496b-8312-856ee5d1cc26"));
		assertThat(doc.getDocType().getConceptId(), equalTo(5089));
		assertThat(doc.getStatus(), equalTo(FhirDocumentReferenceStatus.CURRENT));
		assertThat(doc.getDocStatus(), equalTo(FhirDocumentReferenceDocStatus.FINAL));
		assertThat(doc.getMasterIdentifier(), equalTo("MI-1"));
		assertThat(doc.getDescription(), equalTo("weight chart"));
		assertThat(doc.getProvider().getProviderId(), equalTo(1));
		assertThat(doc.getLocation().getLocationId(), equalTo(2));
		assertThat(doc.getContents().stream().map(FhirDocumentReferenceContent::getContentUrl).collect(Collectors.toList()),
		    containsInAnyOrder("/doc/1.pdf", "/doc/1.png"));
		assertThat(doc.getAttributes().stream().map(FhirDocumentReferenceAttribute::getValueReference)
		        .collect(Collectors.toList()), containsInAnyOrder("ref-1", "ref-2"));
	}
	
	@Test
	public void get_unknownUuid_returnsNull() {
		assertThat(dao.get("does-not-exist"), nullValue());
	}
	
	@Test
	public void get_byUuidCollection_returnsOnlyMatchingDocuments() {
		List<FhirDocumentReference> docs = dao.get(Arrays.asList(DOC_1, DOC_4, "does-not-exist"));
		assertThat(docs.stream().map(FhirDocumentReference::getUuid).collect(Collectors.toList()),
		    containsInAnyOrder(DOC_1, DOC_4));
	}
	
	@Test
	public void createOrUpdate_persistsScalarsEnumsContentsAndAttributes() {
		FhirDocumentReference doc = new FhirDocumentReference();
		doc.setSubject(Context.getPatientService().getPatient(6));
		doc.setEncounter(Context.getEncounterService().getEncounter(5));
		doc.setDocType(Context.getConceptService().getConcept(5497));
		doc.setSecurityLabel(Context.getConceptService().getConcept(5089));
		doc.setStatus(FhirDocumentReferenceStatus.SUPERSEDED);
		doc.setDocStatus(FhirDocumentReferenceDocStatus.AMENDED);
		doc.setDescription("new doc");
		doc.setMasterIdentifier("MI-NEW");
		doc.setDateStarted(new Date(1_000_000_000_000L));
		doc.setDateEnded(new Date(1_100_000_000_000L));
		FhirDocumentReferenceContent content = new FhirDocumentReferenceContent();
		content.setContentType("image/jpeg");
		content.setContentUrl("/new.jpg");
		content.setContentFormat("jpeg");
		doc.addContent(content);
		FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
		attribute.setAttributeType(attributeTypeDao.getAttributeTypes(false).stream()
		        .filter(t -> "Active type A".equals(t.getName())).findFirst().get());
		attribute.setValueReferenceInternal("new-ref");
		doc.addAttribute(attribute);

		String uuid = dao.createOrUpdate(doc).getUuid();
		flushAndClear();

		FhirDocumentReference reloaded = dao.get(uuid);
		assertThat(reloaded.getDocumentReferenceId(), notNullValue());
		assertThat(reloaded.getCreator(), notNullValue());
		assertThat(reloaded.getSubject().getPatientId(), equalTo(6));
		assertThat(reloaded.getEncounter().getEncounterId(), equalTo(5));
		assertThat(reloaded.getDocType().getConceptId(), equalTo(5497));
		assertThat(reloaded.getSecurityLabel().getConceptId(), equalTo(5089));
		assertThat(reloaded.getStatus(), equalTo(FhirDocumentReferenceStatus.SUPERSEDED));
		assertThat(reloaded.getDocStatus(), equalTo(FhirDocumentReferenceDocStatus.AMENDED));
		assertThat(reloaded.getDescription(), equalTo("new doc"));
		assertThat(reloaded.getMasterIdentifier(), equalTo("MI-NEW"));
		assertThat(reloaded.getDateStarted().getTime(), equalTo(1_000_000_000_000L));
		assertThat(reloaded.getDateEnded().getTime(), equalTo(1_100_000_000_000L));
		assertThat(reloaded.getContents(), hasSize(1));
		FhirDocumentReferenceContent reloadedContent = reloaded.getContents().iterator().next();
		assertThat(reloadedContent.getContentUrl(), equalTo("/new.jpg"));
		assertThat(reloadedContent.getContentFormat(), equalTo("jpeg"));
		assertThat(reloadedContent.getDocumentReference().getUuid(), equalTo(uuid));
		assertThat(reloaded.getAttributes(), hasSize(1));
		assertThat(reloaded.getAttributes().iterator().next().getAttributeType().getUuid(),
		    equalTo("drat-9001-0000-0000-0000-000000000001"));
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_6))),
		    contains(uuid));
	}
	
	@Test
	public void createOrUpdate_onLoadedEntity_updatesFieldsAndKeepsChildren() {
		FhirDocumentReference doc = dao.get(DOC_1);
		doc.setDescription("changed");
		doc.setDocStatus(FhirDocumentReferenceDocStatus.AMENDED);
		dao.createOrUpdate(doc);
		flushAndClear();
		
		FhirDocumentReference reloaded = dao.get(DOC_1);
		assertThat(reloaded.getDescription(), equalTo("changed"));
		assertThat(reloaded.getDocStatus(), equalTo(FhirDocumentReferenceDocStatus.AMENDED));
		assertThat(reloaded.getChangedBy(), notNullValue());
		assertThat(reloaded.getContents(), hasSize(2));
		assertThat(reloaded.getAttributes(), hasSize(2));
	}
	
	@Test
	public void delete_isSoftDelete_documentDisappearsFromSearch() {
		assertThat(dao.delete(DOC_4), notNullValue());
		flushAndClear();
		
		assertThat(uuids(map()), not(hasItem(DOC_4)));
		assertThat(dao.get(DOC_4).getVoided(), equalTo(true));
	}
	
	@Test
	public void delete_unknownUuid_returnsNull() {
		assertThat(dao.delete("does-not-exist"), nullValue());
	}
	
	@Test
	public void voidDocumentReference_voidsDocumentSetsEnteredInErrorAndVoidsAllChildren() {
		FhirDocumentReference doc = dao.get(DOC_1);

		dao.voidDocumentReference(doc, "wrong patient");
		flushAndClear();

		FhirDocumentReference reloaded = dao.get(DOC_1);
		assertThat(reloaded.getVoided(), equalTo(true));
		assertThat(reloaded.getDocStatus(), equalTo(FhirDocumentReferenceDocStatus.ENTEREDINERROR));
		assertThat(reloaded.getVoidReason(), equalTo("wrong patient"));
		assertThat(reloaded.getDateVoided(), notNullValue());
		assertThat(reloaded.getVoidedBy().getUserId(), equalTo(Context.getAuthenticatedUser().getUserId()));
		assertThat(reloaded.getContents(), hasSize(2));
		reloaded.getContents().forEach(c -> {
			assertThat(c.getVoided(), equalTo(true));
			assertThat(c.getVoidReason(), equalTo("wrong patient"));
			assertThat(c.getVoidedBy(), notNullValue());
			assertThat(c.getDateVoided(), notNullValue());
		});
		assertThat(reloaded.getAttributes(), hasSize(2));
		reloaded.getAttributes().forEach(a -> {
			assertThat(a.getVoided(), equalTo(true));
			assertThat(a.getVoidReason(), equalTo("wrong patient"));
			assertThat(a.getVoidedBy(), notNullValue());
		});
		assertThat(uuids(map()), not(hasItem(DOC_1)));
	}
	
	@Test
	public void voidDocumentReference_keepsOriginalVoidDetailsOfAlreadyVoidedChildren() {
		FhirDocumentReference doc = dao.get(DOC_1);
		FhirDocumentReferenceContent alreadyVoidedContent = doc.getContents().stream()
		        .filter(c -> "/doc/1.png".equals(c.getContentUrl())).findFirst().get();
		alreadyVoidedContent.setVoided(true);
		alreadyVoidedContent.setVoidReason("old content reason");
		FhirDocumentReferenceAttribute alreadyVoidedAttribute = doc.getAttributes().stream()
		        .filter(a -> "ref-2".equals(a.getValueReference())).findFirst().get();
		alreadyVoidedAttribute.setVoided(true);
		alreadyVoidedAttribute.setVoidReason("old attribute reason");

		dao.voidDocumentReference(doc, "new reason");
		flushAndClear();

		FhirDocumentReference reloaded = dao.get(DOC_1);
		reloaded.getContents().forEach(c -> assertThat(c.getVoidReason(),
		    equalTo("/doc/1.png".equals(c.getContentUrl()) ? "old content reason" : "new reason")));
		reloaded.getAttributes().forEach(a -> assertThat(a.getVoidReason(),
		    equalTo("ref-2".equals(a.getValueReference()) ? "old attribute reason" : "new reason")));
	}
	
	@Test
	public void voidDocumentReference_onNewDocumentWithNullContentsAndAttributes_voidsOnlyTheDocument() {
		FhirDocumentReference doc = new FhirDocumentReference();
		doc.setDocType(Context.getConceptService().getConcept(5497));
		doc.setStatus(FhirDocumentReferenceStatus.CURRENT);
		doc.setDocStatus(FhirDocumentReferenceDocStatus.FINAL);
		doc.setContents(null);
		doc.setAttributes(null);
		
		dao.voidDocumentReference(doc, "no children");
		flushAndClear();
		
		FhirDocumentReference reloaded = dao.get(doc.getUuid());
		assertThat(reloaded.getVoided(), equalTo(true));
		assertThat(reloaded.getVoidReason(), equalTo("no children"));
		assertThat(reloaded.getDocStatus(), equalTo(FhirDocumentReferenceDocStatus.ENTEREDINERROR));
	}
	
	@Test
	public void unauthenticatedUser_cannotSearchGetOrModify() {
		Context.logout();

		assertThrows(APIAuthenticationException.class, () -> dao.getSearchResults(map()));
		assertThrows(APIAuthenticationException.class, () -> dao.get(DOC_1));
		assertThrows(APIAuthenticationException.class, () -> dao.createOrUpdate(new FhirDocumentReference()));
		assertThrows(APIAuthenticationException.class, () -> dao.delete(DOC_1));
	}
	
	private SearchParameterMap map() {
		return new SearchParameterMap();
	}
	
	private SearchParameterMap code(TokenParam token) {
		return map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, new TokenAndListParam().addAnd(token));
	}
	
	private ReferenceAndListParam reference(String uuid) {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(uuid)));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(FhirDocumentReference::getUuid).collect(Collectors.toList());
	}
	
	private void flushAndClear() {
		Context.flushSession();
		Context.clearSession();
	}
}

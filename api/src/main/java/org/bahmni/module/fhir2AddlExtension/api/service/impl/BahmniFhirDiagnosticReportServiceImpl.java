package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportValidator;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.DiagnosticReportSearchParams;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("bahmniFhirDiagnosticReportServiceImpl")
@Primary
public class BahmniFhirDiagnosticReportServiceImpl extends BaseFhirService<DiagnosticReport, FhirDiagnosticReportExt> implements BahmniFhirDiagnosticReportService {
	
	private BahmniFhirDiagnosticReportDao diagnosticReportDao;
	
	private DiagnosticReportValidator validator;
	
	private BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator;
	
	private SearchQuery<FhirDiagnosticReportExt, DiagnosticReport, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> searchQuery;
	
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@Autowired
	public BahmniFhirDiagnosticReportServiceImpl(
	    BahmniFhirDiagnosticReportDao diagnosticReportDao,
	    DiagnosticReportValidator validator,
	    BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator,
	    SearchQuery<FhirDiagnosticReportExt, DiagnosticReport, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> searchQuery,
	    SearchQueryInclude<DiagnosticReport> searchQueryInclude) {
		this.diagnosticReportDao = diagnosticReportDao;
		this.validator = validator;
		this.diagnosticReportTranslator = diagnosticReportTranslator;
		this.searchQuery = searchQuery;
		this.searchQueryInclude = searchQueryInclude;
	}
	
	@Override
	public IBundleProvider searchForDiagnosticReports(DiagnosticReportSearchParams diagnosticReportSearchParams) {
		return searchQuery.getQueryResults(diagnosticReportSearchParams.toSearchParameterMap(), diagnosticReportDao,
		    diagnosticReportTranslator, searchQueryInclude);
	}
	
	@Override
	protected FhirDao<FhirDiagnosticReportExt> getDao() {
		return diagnosticReportDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDiagnosticReportExt, DiagnosticReport> getTranslator() {
		return diagnosticReportTranslator;
	}
	
	@Override
	public DiagnosticReport create(@Nonnull DiagnosticReport newResource) {
		validator.validate(newResource);
		if (newResource == null) {
			throw new InvalidRequestException("A resource of type " + resourceClass.getSimpleName() + " must be supplied");
		}
		FhirDiagnosticReportExt openmrsReport = getTranslator().toOpenmrsType(newResource);
		validateObject(openmrsReport);
		
		if (openmrsReport.getUuid() == null) {
			openmrsReport.setUuid(FhirUtils.newUuid());
		}
		return getTranslator().toFhirResource(getDao().createOrUpdate(openmrsReport));
	}
	
	@Override
	public DiagnosticReport findByOrder(@Nonnull Order order) {
		FhirDiagnosticReportExt diagnosticReportExt = diagnosticReportDao.findByOrder(order);
		if (diagnosticReportExt == null) {
			return null;
		}
		return diagnosticReportTranslator.toFhirResource(diagnosticReportExt);
	}
	
}

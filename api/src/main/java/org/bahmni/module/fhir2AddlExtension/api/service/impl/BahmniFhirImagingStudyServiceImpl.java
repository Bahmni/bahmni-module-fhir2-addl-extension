package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Slf4j
public class BahmniFhirImagingStudyServiceImpl extends BaseFhirService<ImagingStudy, FhirImagingStudy> implements BahmniFhirImagingStudyService {
	
	private final BahmniFhirImagingStudyDao imagingStudyDao;
	
	private final BahmniFhirImagingStudyTranslator imagingStudyTranslator;
	
	@Autowired
	public BahmniFhirImagingStudyServiceImpl(BahmniFhirImagingStudyDao imagingStudyDao,
	    BahmniFhirImagingStudyTranslator imagingStudyTranslator) {
		this.imagingStudyDao = imagingStudyDao;
		this.imagingStudyTranslator = imagingStudyTranslator;
	}
	
	@Override
	protected FhirDao<FhirImagingStudy> getDao() {
		return imagingStudyDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirImagingStudy, ImagingStudy> getTranslator() {
		return imagingStudyTranslator;
	}
}

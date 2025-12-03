package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.springframework.stereotype.Component;

@Component
public class BahmniFhirImagingStudyDaoImpl extends BaseFhirDao<FhirImagingStudy> implements BahmniFhirImagingStudyDao {
}

package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentReferenceAttributeTypeDaoImpl implements DocumentReferenceAttributeTypeDao {
	
	private static final String SELECT_ALL_ATTRIBUTETYPES = "FROM FhirDocumentReferenceAttributeType";
	
	private static final String SELECT_ALL_ACTIVE_ATTRIBUTETYPES = "FROM FhirDocumentReferenceAttributeType dat WHERE dat.retired = false";
	
	@Getter(AccessLevel.PUBLIC)
	@Setter(value = AccessLevel.PROTECTED, onMethod = @__({ @Autowired, @Qualifier("sessionFactory") }))
	private SessionFactory sessionFactory;
	
	@Override
	@Cacheable(value = "fhir2addlextensionDocAttributeType")
	public List<FhirDocumentReferenceAttributeType> getAttributeTypes(boolean includeRetired) {
		if (includeRetired) {
			return sessionFactory.getCurrentSession()
			        .createQuery(SELECT_ALL_ATTRIBUTETYPES, FhirDocumentReferenceAttributeType.class).list();
		}
		return sessionFactory.getCurrentSession()
		        .createQuery(SELECT_ALL_ACTIVE_ATTRIBUTETYPES, FhirDocumentReferenceAttributeType.class).list();
	}
	
}

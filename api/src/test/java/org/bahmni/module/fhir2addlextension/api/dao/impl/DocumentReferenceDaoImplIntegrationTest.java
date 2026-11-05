package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

public class DocumentReferenceDaoImplIntegrationTest {
	
	@Test
	public void daoIsInstantiable() {
		DocumentReferenceDaoImpl dao = new DocumentReferenceDaoImpl();
		assertThat(dao, notNullValue());
	}
}

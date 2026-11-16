package org.bahmni.module.fhir2addlextension.api.dao;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemOnlyCodeQueryTest {
	
	@Test
	public void isSystemOnly_trueForSystemWithNullOrEmptyCode() {
		assertTrue(SystemOnlyCodeQuery.isSystemOnly(new TokenParam("sys", "")));
		assertTrue(SystemOnlyCodeQuery.isSystemOnly(new TokenParam("sys", null)));
	}
	
	@Test
	public void isSystemOnly_falseWhenCodeOrSystemMissing() {
		assertFalse(SystemOnlyCodeQuery.isSystemOnly(new TokenParam("sys", "code")));
		assertFalse(SystemOnlyCodeQuery.isSystemOnly(new TokenParam(null, "code")));
		assertFalse(SystemOnlyCodeQuery.isSystemOnly(new TokenParam("", "")));
	}
	
	@Test
	public void hasSystemOnlyToken_detectsAnyAlternativeInAnyAndGroup() {
		TokenAndListParam param = new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam(null, "c")))
		        .addAnd(new TokenOrListParam().add(new TokenParam("a", "b")).add(new TokenParam("sys", "")));
		assertTrue(SystemOnlyCodeQuery.hasSystemOnlyToken(param));
		assertFalse(SystemOnlyCodeQuery.hasSystemOnlyToken(new TokenAndListParam().addAnd(new TokenParam("a", "b"))));
		assertFalse(SystemOnlyCodeQuery.hasSystemOnlyToken(null));
	}
}

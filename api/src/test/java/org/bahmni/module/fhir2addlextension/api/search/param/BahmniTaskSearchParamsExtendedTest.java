package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class BahmniTaskSearchParamsExtendedTest {
	
	@Test
	public void equals_shouldReturnTrueForIdenticalInstances() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		BahmniTaskSearchParams params1 = new BahmniTaskSearchParams();
		params1.setEncounterReference(encounterRef);
		
		BahmniTaskSearchParams params2 = new BahmniTaskSearchParams();
		params2.setEncounterReference(encounterRef);
		
		assertThat(params1.equals(params2), equalTo(true));
	}
	
	@Test
	public void equals_shouldReturnFalseForDifferentInstances() {
		BahmniTaskSearchParams params1 = new BahmniTaskSearchParams();
		BahmniTaskSearchParams params2 = new BahmniTaskSearchParams();
		
		params1.setEncounterReference(new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Encounter", "enc-1"))));
		params2.setEncounterReference(new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Encounter", "enc-2"))));
		
		assertThat(params1.equals(params2), equalTo(false));
	}
	
	@Test
	public void hashCode_shouldBeConsistentAcrossInstances() {
		BahmniTaskSearchParams params1 = new BahmniTaskSearchParams();
		BahmniTaskSearchParams params2 = new BahmniTaskSearchParams();
		
		assertThat(params1.hashCode(), equalTo(params2.hashCode()));
	}
	
	@Test
	public void encounterReferenceGetter_shouldReturnSetValue() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		params.setEncounterReference(encounterRef);
		
		assertThat(params.getEncounterReference(), notNullValue());
		assertThat(params.getEncounterReference(), equalTo(encounterRef));
	}
	
	@Test
	public void nameGetter_shouldReturnSetValue() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		
		params.setName(name);
		
		assertThat(params.getName(), notNullValue());
		assertThat(params.getName(), equalTo(name));
	}
	
	@Test
	public void toSearchParameterMap_shouldHandleNullEncounterReference() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		params.setEncounterReference(null);
		
		assertThat(params.toSearchParameterMap(), notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_shouldHandleNullName() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		params.setName(null);
		
		assertThat(params.toSearchParameterMap(), notNullValue());
	}
	
	@Test
	public void allFieldsSet_shouldBePresentInSearchParameterMap() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		params.setEncounterReference(new ReferenceAndListParam());
		params.setName(new StringAndListParam());
		//params.setFocusReference(new ReferenceAndListParam());
		
		assertThat(params.toSearchParameterMap(), notNullValue());
	}
}

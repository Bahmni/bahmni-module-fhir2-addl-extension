package org.bahmni.module.fhir2addlextension.api.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.bahmni.module.fhir2addlextension.api.service.BahmniPatientMatchService;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.GenderTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("defaultPatientMatchService")
public class BahmniPatientMatchServiceImpl implements BahmniPatientMatchService {
	
	private final GenderTranslator genderTranslator;
	
	@Autowired
	public BahmniPatientMatchServiceImpl(GenderTranslator genderTranslator) {
		this.genderTranslator = genderTranslator;
	}
	
	@Override
	public List<Patient> findMatches(org.hl7.fhir.r4.model.Patient fhirPatient) {
		HumanName humanName = fhirPatient.getNameFirstRep();
		List<String> givenNames = humanName.getGiven().stream().map(StringType::getValue).collect(Collectors.toList());
		String name = joinNames(givenNames, humanName.getFamily());
		Integer birthYear = fhirPatient.hasBirthDate()
		        ? fhirPatient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).getYear() : null;

		Set<Person> similarPeople = Context.getPersonService().getSimilarPeople(name, birthYear,
		    genderTranslator.toOpenmrsType(fhirPatient.getGender()));
		
		List<Patient> patients = new ArrayList<>();
		for (Person person : similarPeople) {
			if (!person.getIsPatient()) {
				continue;
			}
			Patient patient = Context.getPatientService().getPatient(person.getPersonId());
			if (patient != null) {
				patients.add(patient);
			}
		}
		return patients;
	}
	
	private String joinNames(List<String> givenNames, String familyName) {
		return Stream.concat(givenNames.stream(), Stream.of(familyName)).filter(StringUtils::isNotBlank)
		        .map(String::trim).collect(Collectors.joining(" "));
	}
}

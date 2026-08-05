package org.bahmni.module.fhir2addlextension.api.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class MedicationRequestDateUtilsTest {
	
	@Test
	public void isSameDay_givenSameDayDifferentTime_shouldReturnTrue() {
		Calendar cal = Calendar.getInstance();
		cal.set(2025, Calendar.JUNE, 10, 8, 0, 0);
		Date morning = cal.getTime();
		cal.set(2025, Calendar.JUNE, 10, 23, 59, 59);
		Date evening = cal.getTime();
		
		assertThat(MedicationRequestDateUtils.isSameDay(morning, evening), equalTo(true));
	}
	
	@Test
	public void isSameDay_givenDifferentDays_shouldReturnFalse() {
		Calendar cal = Calendar.getInstance();
		cal.set(2025, Calendar.JUNE, 10, 12, 0, 0);
		Date day1 = cal.getTime();
		cal.set(2025, Calendar.JUNE, 11, 12, 0, 0);
		Date day2 = cal.getTime();
		
		assertThat(MedicationRequestDateUtils.isSameDay(day1, day2), equalTo(false));
	}
	
	@Test
	public void isFutureDate_givenTomorrow_shouldReturnTrue() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 1);
		Date tomorrow = cal.getTime();
		
		assertThat(MedicationRequestDateUtils.isFutureDate(tomorrow, now), equalTo(true));
	}
	
	@Test
	public void isFutureDate_givenToday_shouldReturnFalse() {
		Date now = new Date();
		
		assertThat(MedicationRequestDateUtils.isFutureDate(now, now), equalTo(false));
	}
	
	@Test
	public void isFutureDate_givenYesterday_shouldReturnFalse() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterday = cal.getTime();
		
		assertThat(MedicationRequestDateUtils.isFutureDate(yesterday, now), equalTo(false));
	}
	
	@Test
	public void isFutureDate_givenLaterTodaySameDay_shouldReturnFalse() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 8);
		Date morning = cal.getTime();
		cal.set(Calendar.HOUR_OF_DAY, 23);
		Date evening = cal.getTime();
		
		// evening is after morning, but same day — not a future date
		assertThat(MedicationRequestDateUtils.isFutureDate(evening, morning), equalTo(false));
	}
}

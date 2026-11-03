package org.bahmni.module.fhir2addlextension.api;

import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class TestUtils {
	
	/**
	 * Creates a real {@link OpenmrsFhirCriteriaContext} backed by Mockito mocks of the JPA
	 * infrastructure. Joins, predicates, and subqueries are all handled without error, making this
	 * suitable for unit-testing {@code setupSearchParams} dispatch logic.
	 */
	@SuppressWarnings("unchecked")
	public static <T> OpenmrsFhirCriteriaContext<T, T> mockCriteriaContext(Class<T> entityClass) {
		// predicate must be created before cb so the custom Answer lambda can close over it
		Predicate predicate = mock(Predicate.class);
		// CriteriaBuilder.In is a Predicate subtype returned by cb.in(); needs its own mock
		// so Mockito doesn't complain about returning a Predicate where In is expected.
		// Raw type used to avoid wildcard capture issues in thenReturn.
		@SuppressWarnings("rawtypes")
		CriteriaBuilder.In inPredicate = mock(CriteriaBuilder.In.class);

		// Use a custom default Answer for CriteriaBuilder: intercept at JVM reflection level,
		// bypassing Java compile-time overload resolution (e.g. equal(Expression,Expression) vs
		// equal(Expression,Object)).  Methods returning exactly Predicate get the shared predicate
		// mock; methods returning CriteriaBuilder.In get the inPredicate mock.
		CriteriaBuilder cb = mock(CriteriaBuilder.class, invocation -> {
			Class<?> rt = invocation.getMethod().getReturnType();
			if (CriteriaBuilder.In.class.isAssignableFrom(rt)) {
				return inPredicate;
			}
			if (rt == Predicate.class) {
				return predicate;
			}
			return null;
		});

		Root<T> root = mock(Root.class);
		CriteriaQuery<T> cq = mock(CriteriaQuery.class);
		EntityManager em = mock(EntityManager.class);

		Join<?, ?> join = mock(Join.class);
		Path<?> path = mock(Path.class);

		javax.persistence.criteria.Subquery<?> subquery = mock(javax.persistence.criteria.Subquery.class);
		Root<?> subRoot = mock(Root.class);
		Path<?> subPath = mock(Path.class);

		// root and join navigation
		lenient().when(root.join(any(String.class))).thenReturn((Join) join);
		lenient().when(root.join(any(String.class), any())).thenReturn((Join) join);
		lenient().when(root.get(any(String.class))).thenReturn((Path) path);
		lenient().when(join.join(any(String.class))).thenReturn((Join) join);
		lenient().when(join.join(any(String.class), any())).thenReturn((Join) join);
		lenient().when(join.get(any(String.class))).thenReturn((Path) path);

		// path chaining (e.g. root.get("personA").get("personId"))
		lenient().when(path.get(any(String.class))).thenReturn((Path) path);
		lenient().when(path.in(any(javax.persistence.criteria.Subquery.class))).thenReturn(predicate);
		lenient().when(path.in(any(Object.class))).thenReturn(predicate);

		// in().value() chains — stub so callers can call .value(x).value(y) without NPE
		lenient().when(inPredicate.value(any())).thenReturn(inPredicate);

		lenient().when(cq.distinct(any(Boolean.class))).thenReturn(cq);

		// subquery chain (for RelatedPerson subquery pattern)
		lenient().when(cq.subquery(any())).thenReturn((javax.persistence.criteria.Subquery) subquery);
		lenient().when(subquery.from(any(Class.class))).thenReturn((Root) subRoot);
		lenient().when(subquery.select(any())).thenReturn((javax.persistence.criteria.Subquery) subquery);
		lenient().when(subquery.where(any(Predicate.class))).thenReturn((javax.persistence.criteria.Subquery) subquery);
		lenient().when(subRoot.get(any(String.class))).thenReturn((Path) subPath);
		lenient().when(subPath.get(any(String.class))).thenReturn((Path) subPath);
		lenient().when(subPath.in(any(javax.persistence.criteria.Subquery.class))).thenReturn(predicate);

		return new OpenmrsFhirCriteriaContext<>(em, cb, cq, root);
	}
	
	public static void setPropertyOnObject(Object target, String attributeName, Object value) throws NoSuchFieldException,
	        IllegalAccessException {
		//TBD: unfortunately, the setters are package private
		Class<?> clazz = target.getClass();
		Field field = clazz.getDeclaredField(attributeName);
		field.setAccessible(true);
		field.set(target, value);
	}
	
	public static @NotNull
	Date minusDays(@NotNull Date currentDate, int days) {
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.minus(days, ChronoUnit.DAYS);
		return Date.from(resultInstant);
	}
	
	public static @NotNull
	Date plusDays(@NotNull Date currentDate, int days) {
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.plus(days, ChronoUnit.DAYS);
		return Date.from(resultInstant);
	}
}

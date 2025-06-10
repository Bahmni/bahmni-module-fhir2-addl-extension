package org.bahmni.module.fhir2AddlExtension.api.providers;

import java.util.Objects;
import javax.annotation.Nonnull;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.lang3.StringUtils;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirValueSetService;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.ValueSetFhirResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extended ValueSet FHIR Resource Provider for Bahmni with support for hierarchical $expand
 * operation. This provider extends the standard FHIR ValueSet resource provider to add support for
 * hierarchical expansion of value sets, allowing clients to retrieve concept relationships and
 * parent-child structures.
 * <p>
 * Supported operations:
 * <ul>
 * <li>$expand - Expands a ValueSet with optional hierarchical relationships</li>
 * </ul>
 * 
 * @author [Author Name]
 * @since [Version]
 */
@Component
@R4Provider
public class BahmniValueSetFhirR4ResourceProvider extends ValueSetFhirResourceProvider {
	
	private static final Logger log = LoggerFactory.getLogger(BahmniValueSetFhirR4ResourceProvider.class);
	
	private final BahmniFhirValueSetService bahmniFhirValueSetService;
	
	/**
	 * Constructor for dependency injection.
	 * 
	 * @param bahmniFhirValueSetService the service for ValueSet operations
	 */
	public BahmniValueSetFhirR4ResourceProvider(BahmniFhirValueSetService bahmniFhirValueSetService) {
		this.bahmniFhirValueSetService = Objects.requireNonNull(bahmniFhirValueSetService,
		    "BahmniFhirValueSetService cannot be null");
	}
	
	/**
	 * Expands a ValueSet by ID with hierarchical output. This operation retrieves a ValueSet and
	 * expands it to include all concepts that are part of the set with hierarchical relationships
	 * between concepts preserved.
	 * 
	 * @param id the ID of the ValueSet to expand (required)
	 * @return expanded ValueSet with concepts in hierarchical structure
	 * @throws InvalidRequestException if parameters are invalid
	 * @throws ResourceNotFoundException if the ValueSet cannot be found
	 * @throws InternalErrorException if an unexpected error occurs
	 */
	@Operation(name = "$expand", idempotent = true)
	public ValueSet expandedValueSet(@IdParam @Nonnull IdType id) {
		
		log.debug("Expanding ValueSet with ID: {}", id.getIdPart());
		
		validateExpandParameters(id);
		
		try {
			ValueSet result = bahmniFhirValueSetService.expandedValueSet(id.getIdPart());
			
			log.debug("Successfully expanded ValueSet with ID: {}, returned {} concepts", id.getIdPart(),
			    getConceptCount(result));
			
			return result;
			
		}
		catch (IllegalArgumentException e) {
			log.warn("Invalid parameters for ValueSet expansion: {}", e.getMessage());
			throw new InvalidRequestException("Invalid parameters for ValueSet expansion: " + e.getMessage(), e);
		}
		catch (ResourceNotFoundException e) {
			log.warn("ValueSet not found: {}", id.getIdPart());
			throw e; // Re-throw as-is
		}
		catch (Exception e) {
			log.error("Unexpected error expanding ValueSet with ID: {}", id.getIdPart(), e);
			throw new InternalErrorException("Internal server error occurred while expanding ValueSet", e);
		}
	}
	
	/**
	 * Validates the parameters for the expand operation.
	 * 
	 * @param id the ValueSet ID to validate
	 * @throws InvalidRequestException if any parameter is invalid
	 */
	private void validateExpandParameters(IdType id) {
		if (id == null || StringUtils.isBlank(id.getIdPart())) {
			throw new InvalidRequestException("ValueSet ID must be provided");
		}
	}
	
	/**
	 * Gets the count of concepts in an expanded ValueSet.
	 * 
	 * @param valueSet the ValueSet to count concepts in
	 * @return the number of concepts, or 0 if expansion is null
	 */
	private int getConceptCount(ValueSet valueSet) {
		if (valueSet == null || valueSet.getExpansion() == null || valueSet.getExpansion().getContains() == null) {
			return 0;
		}
		return valueSet.getExpansion().getContains().size();
	}
}

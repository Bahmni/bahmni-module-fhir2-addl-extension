package org.bahmni.module.fhir2AddlExtension.api.service;

import org.openmrs.Order;
import org.openmrs.api.OrderContext;

/*
 * TODO: Remove this extension interface once this JIRA changes are backported into 2.5.x and 2.6.x on OpenMRS Core
 * JIRA: https://openmrs.atlassian.net/browse/TRUNK-6534
 * Core PR: https://github.com/openmrs/openmrs-core/pull/5736
 * This has been done to support creation of linked orders
 * This is primarily extended to remove the validation check for orders linked with previous_order_id to have the same order concept.
 */

public interface OpenMRSOrderServiceExtension {
	
	Order validateAndSetMissingFields(Order order, OrderContext orderContext);
}

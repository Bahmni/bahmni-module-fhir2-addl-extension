# ServiceRequest Extensions in Bahmni FHIR2 Additional Extension Module

## Introduction

The Bahmni FHIR2 Additional Extension module enhances the standard OpenMRS FHIR2 module's ServiceRequest resource with additional capabilities and extensions. This document outlines the extensions added to the FHIR ServiceRequestProfile, including property extensions, additional search methods, and other enhancements that extend the functionality provided by the base OpenMRS FHIR2 module.

These extensions enable more comprehensive representation of orders in Bahmni and provide additional search capabilities that are useful in clinical workflows.

## Schema-Level Changes

Unlike the standard OpenMRS FHIR2 module which maps ServiceRequest resources to the `test_order` table, this extension module maps ServiceRequest resources to the more generic `orders` table in the OpenMRS database. This change provides several benefits:

- Support for a broader range of order types beyond just test orders
- Consistent representation of all non-drug orders as ServiceRequest resources
- Simplified data model that aligns better with FHIR's ServiceRequest concept
- Adds support for any orders that are placed other than drug-orders.

Note: While we moved the ServiceRequest profile to depend on `orders` table, a filter has been added in the DAO layer to exclude all operation on DRUG_ORDER order type. Since drug orders will be part of MedicationRequest resource.

## Property Extensions

### Order Type Category

Order type in OpenMRS are represented as category in the ServiceRequest resource. This allows clients to filter ServiceRequests by order type.

**System URI**: `http://fhir.bahmni.org/code-system/order-type`

**Example JSON representation**:

```json
{
  "resourceType": "ServiceRequest",
  "id": "5f4d2688-8d7a-4e23-b987-566e752f5d11",
  "category": [
    {
      "coding": [
        {
          "system": "http://fhir.bahmni.org/code-system/order-type",
          "code": "e7ee8e76-e8b1-4c26-9b40-ca4bc3939715",
          "display": "Lab Order"
        }
      ],
      "text": "Lab Order"
    }
  ],
  // other ServiceRequest properties
}
```

### Urgency to Priority Mapping

OpenMRS order urgency is mapped to FHIR ServiceRequest priority. This mapping provides standardized representation of order priority in FHIR.

**Supported mappings**:
- OpenMRS `ROUTINE` → FHIR `ROUTINE`
- OpenMRS `STAT` → FHIR `STAT`
- OpenMRS `ON_SCHEDULED_DATE` → FHIR `ROUTINE`

**Example JSON representation**:

```json
{
  "resourceType": "ServiceRequest",
  "id": "5f4d2688-8d7a-4e23-b987-566e752f5d11",
  "priority": "stat",
  // other ServiceRequest properties
}
```

### Lab Order Concept Type Extension

This extension distinguishes between single lab tests and panels (groups of tests) based on the concept class of the order. This extension will be sent only when the ordered concept is either of LabTest, Test or LabSet concept classes. 

**Extension URL**: `http://fhir.bahmni.org/lab-order-concept-type-extension`

**Values**:
- `Test`: For orders with concept class "LabTest" or "Test"
- `Panel`: For orders with concept class "LabSet"

**Example JSON representation**:

```json
{
  "resourceType": "ServiceRequest",
  "id": "5f4d2688-8d7a-4e23-b987-566e752f5d11",
  "extension": [
    {
      "url": "http://fhir.bahmni.org/lab-order-concept-type-extension",
      "valueString": "Panel"
    }
  ],
  // other ServiceRequest properties
}
```

## Additional Search Methods

### Search by Category (Order Type)

This extension allows filtering ServiceRequests by order type category.

**URL Parameter**: `category`

**Example Request**:
```
GET /openmrs/ws/fhir2/R4/ServiceRequest?category=e7ee8e76-e8b1-4c26-9b40-ca4bc3939715
```

This request returns all ServiceRequests with the specified order type UUID.

### Search by Number of Visits

This unique feature allows retrieving ServiceRequests from a specified number of recent patient visits. This is particularly useful in clinical workflows where providers need to review orders from n recent visits.

**URL Parameters**:
- `patient`: (Required) The patient reference
- `numberOfVisits`: (Required) The number of recent visits to include

**Example Request**:
```
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&numberOfVisits=3
```

This request returns all ServiceRequests from the 3 most recent visits for the specified patient.

You can also combine this with the category parameter to filter by order type:

```
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&numberOfVisits=3&category=e7ee8e76-e8b1-4c26-9b40-ca4bc3939715

```
## Other Enhancements

### Drug Order Filtering

Drug orders are explicitly excluded from ServiceRequest resources, as they are represented by the MedicationRequest resource in FHIR. This ensures a clean separation between medication orders and other types of orders.

Implementation details:
- The DAO layer adds a filter to exclude orders with order type UUID matching `DRUG_ORDER_TYPE_UUID`
- Attempts to create a ServiceRequest with a drug order type will result in an error

### Enhanced Date Range Handling

The date range search has been enhanced to include more date fields from the OpenMRS Order model:

- `scheduledDate`
- `dateActivated`
- `dateStopped`
- `autoExpireDate`

This provides more comprehensive date-based searching capabilities compared to the standard implementation.

**Example Request**:
```
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=Patient/aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&occurrence=ge2023-01-01&occurrence=le2023-12-31
```

**Example curl command**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/fhir2/R4/ServiceRequest?patient=Patient/aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&occurrence=ge2023-01-01&occurrence=le2023-12-31' \
  -H 'Authorization: Basic YWRtaW46QWRtaW4xMjM=' \
  -H 'Accept: application/json'
```

## Integration with OpenMRS FHIR2 Module

The Bahmni FHIR2 Additional Extension module extends the base OpenMRS FHIR2 module by:

1. Providing custom implementations of key interfaces:
   - `ServiceRequestTranslator`
   - `FhirServiceRequestDao`
   - `ServiceRequestFhirResourceProvider`

2. Using Spring's `@Primary` annotation to ensure these implementations take precedence over the standard ones

3. Maintaining compatibility with the base module's API while adding extensions

## API Examples for QA Testing

### 1. Retrieve a specific ServiceRequest by ID

```bash
GET /openmrs/ws/fhir2/R4/ServiceRequest/5f4d2688-8d7a-4e23-b987-566e752f5d11
```

### 2. Search for ServiceRequests by patient

```bash
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0
```

### 3. Search for ServiceRequests by patient and order type category

```bash
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&category=e7ee8e76-e8b1-4c26-9b40-ca4bc3939715
```

### 4. Search for ServiceRequests from recent visits

```bash
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&numberOfVisits=3
```

### 4. Search for ServiceRequests from recent visits and category

```bash
GET /openmrs/ws/fhir2/R4/ServiceRequest?patient=aeb1b41e-3f24-4ca3-945b-71f3ff51c4b0&category=e7ee8e76-e8b1-4c26-9b40-ca4bc3939715&numberOfVisits=3
```

Note: There are additional search capabilities possible which can be referred from  [BahmniServiceRequestFhirR4ResourceProvider.java](../api/src/main/java/org/bahmni/module/fhir2AddlExtension/api/providers/BahmniServiceRequestFhirR4ResourceProvider.java)
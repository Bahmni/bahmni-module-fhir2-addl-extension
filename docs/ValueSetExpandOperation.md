# ValueSet $expand Operation Usage Guide

This document explains how to use the hierarchical ValueSet $expand operation implemented in the Bahmni FHIR2 extension module.

## Overview

The ValueSet $expand operation allows you to expand OpenMRS concepts into FHIR ValueSet resources with hierarchical relationships. The implementation uses OpenMRS concept `setMembers` to build parent-child concept hierarchies and always returns results in hierarchical structure with all concepts included.

## Endpoints

The $expand operation is available at the following endpoint:

### Expand by ValueSet ID

```url
GET /openmrs/ws/fhir2/R4/ValueSet/{id}/$expand
```

## Parameters

| Parameter | Type   | Required | Description                                |
| --------- | ------ | -------- | ------------------------------------------ |
| `id`      | string | Yes      | The UUID of the ValueSet/Concept to expand |

## Technical Implementation

### Concept Relationship Mapping

- **OpenMRS Concepts** → **FHIR ValueSet expansion**
- **Concept setMembers** → **Hierarchical parent-child relationships**
- **Concept UUID** → **FHIR code**
- **Concept displayString** → **FHIR display**

### Behavior

- **Always Hierarchical**: Returns concepts in hierarchical structure preserving parent-child relationships
- **Excludes Parent Concept**: Only returns the setMembers of the requested concept, not the concept itself
- **All Concepts**: Returns all setMembers without pagination or filtering
- **Complete Expansion**: No limits on the number of concepts returned

### Inactive Concept Detection

The implementation automatically detects inactive concepts and sets the `inactive` field to `true` for:

- Retired concepts (`concept.isRetired() == true`)
- Concepts with retired ConceptClass (`conceptClass.isRetired() == true`)
- Concepts with null ConceptClass (`conceptClass == null`)

### Infinite Loop Prevention

The service includes built-in protection against infinite loops through concept relationship cycles using a `processed` set to track visited concepts.

## Usage Examples

### Basic Hierarchical Expansion

**Request:**

```http
GET /openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/$expand
```

**Response:**

```json
{
  "resourceType": "ValueSet",
  "id": "12345678-1234-1234-1234-123456789012",
  "url": "http://bahmni.org/openmrs/concepts/12345678-1234-1234-1234-123456789012",
  "name": "DiagnosisValueSet",
  "expansion": {
    "timestamp": "2025-06-09T17:41:00.000Z",
    "total": 2,
    "contains": [
      {
        "code": "child-concept-uuid",
        "display": "Hypertension"
      },
      {
        "code": "retired-concept-uuid",
        "display": "Obsolete Heart Condition",
        "inactive": true
      }
    ]
  }
}
```

## cURL Examples

### Basic hierarchical expansion

```bash
curl -X GET \
  "http://localhost:8080/openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/\$expand" \
  -H "Authorization: Basic YWRtaW46QWRtaW4xMjM=" \
  -H "Accept: application/fhir+json"
```

## Prerequisites

1. **Bahmni FHIR2 Extension Module**: Ensure the module is installed and running
2. **Authentication**: Valid OpenMRS credentials with FHIR access privileges
3. **Concept Setup**: The ValueSet ID should correspond to an existing OpenMRS Concept with **setMembers** (not ConceptAnswers) for hierarchical relationships

### OpenMRS Concept Configuration

For hierarchical expansion to work properly:

- Parent concepts must have child concepts configured as **setMembers**
- The concept relationship is built using `concept.getSetMembers()`
- ConceptAnswers are NOT used for hierarchical relationships
- Ensure concepts have proper ConceptClass assignments to avoid inactive flagging

## Response Format

The response follows the FHIR R4 ValueSet resource format with an `expansion` element containing:

- `timestamp`: When the expansion was performed (ISO 8601 format)
- `total`: Total number of concepts in the expansion
- `contains`: Array of concept definitions in hierarchical structure

### Hierarchical Structure

The expansion directly contains the setMembers of the requested concept (the parent concept itself is excluded). If any of these setMembers have their own setMembers, they will include a nested `contains` array with their child concepts. The nesting preserves the OpenMRS concept setMember relationships.

**Example structure:**

- Request: Expand concept "Medical Specialties"
- Response contains: "Cardiology", "Neurology", "Pediatrics" (not "Medical Specialties" itself)
- If "Cardiology" has setMembers like "Interventional Cardiology", they appear in Cardiology's `contains` array

### Concept Code Mapping

- **FHIR `code`** = OpenMRS Concept UUID
- **FHIR `display`** = OpenMRS Concept displayString
- **FHIR `inactive`** = true for retired/invalid concepts

## Error Handling

The operation provides comprehensive error handling with specific HTTP status codes and FHIR OperationOutcome responses:

### Client Errors (4XX)

#### 400 Bad Request - InvalidRequestException

```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "invalid",
      "details": {
        "text": "ValueSet ID must be provided"
      }
    }
  ]
}
```

**Common causes:**

- Empty or null ValueSet ID
- Malformed request parameters

#### 404 Not Found - ResourceNotFoundException

```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "not-found",
      "details": {
        "text": "ValueSet not found with ID: 12345678-1234-1234-1234-123456789012"
      }
    }
  ]
}
```

**Common causes:**

- ValueSet/Concept with the given UUID doesn't exist in OpenMRS
- Concept exists but is not accessible due to permissions

### Server Errors (5XX)

#### 500 Internal Server Error - InternalErrorException

```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "exception",
      "details": {
        "text": "Internal server error occurred while expanding ValueSet"
      }
    }
  ]
}
```

**Common causes:**

- Database connectivity issues
- OpenMRS service failures
- Unexpected runtime exceptions

## Performance Considerations

### Optimization Strategies

1. **Caching**: Consider implementing caching for frequently accessed ValueSets
2. **Memory Usage**: Large concept hierarchies may use significant memory due to nested structures

### Best Practices

```bash
# Basic expansion request
curl ".../$expand"

# Client-side processing if needed
curl ".../$expand" | jq '.expansion.contains[] | select(.display | contains("diabetes"))'
```

## Integration Examples

### JavaScript/TypeScript

```typescript
async function expandValueSet(valueSetId: string): Promise<ValueSet> {
  const response = await fetch(
    `/openmrs/ws/fhir2/R4/ValueSet/${valueSetId}/$expand`,
    {
      headers: {
        Authorization: "Basic " + btoa("admin:Admin123"),
        Accept: "application/fhir+json",
      },
    },
  );

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  return response.json();
}

// Usage example
const expansion = await expandValueSet("concept-uuid");
```

### Java Client

```java
// Using HAPI FHIR Client
IGenericClient client = ctx.newRestfulGenericClient(serverBase);

// Basic expansion
ValueSet expanded = client
    .operation()
    .onInstance(new IdType("ValueSet", "concept-uuid"))
    .named("$expand")
    .returnResourceType(ValueSet.class)
    .execute();
```

## Troubleshooting

### Common Issues

1. **Empty Expansion Results**

   - Verify the concept has setMembers configured
   - Check if the concept exists and is not retired
   - Ensure proper authentication and permissions

2. **Missing Child Concepts in Hierarchy**

   - Verify parent concepts have child concepts as setMembers (not ConceptAnswers)
   - Check for circular references that might be preventing expansion

3. **Performance Issues**

   - Large concept hierarchies may consume significant memory
   - Consider client-side filtering if needed

4. **Authentication Errors**
   - Verify OpenMRS credentials are correct
   - Ensure user has FHIR access privileges
   - Check session timeout settings

### Debug Information

Enable debug logging by setting the following in OpenMRS:

```properties
log4j.logger.org.bahmni.module.fhir2AddlExtension.api.providers.BahmniValueSetFhirR4ResourceProvider=DEBUG
log4j.logger.org.bahmni.module.fhir2AddlExtension.api.service.impl.BahmniFhirValueSetServiceImpl=DEBUG
```

This will provide detailed information about:

- Parameter validation
- Concept retrieval process
- Expansion algorithm execution
- Performance metrics

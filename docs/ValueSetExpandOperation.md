# ValueSet $expand Operation Usage Guide

This document explains how to use the hierarchical ValueSet $expand operation implemented in the Bahmni FHIR2 extension module.

## Overview

The ValueSet $expand operation allows you to expand OpenMRS concepts into FHIR ValueSet resources with support for hierarchical relationships. The implementation uses OpenMRS concept `setMembers` to build parent-child concept hierarchies and provides both hierarchical and flat expansion modes.

## Endpoints

The $expand operation is available at the following endpoint:

### Expand by ValueSet ID

```url
GET /openmrs/ws/fhir2/R4/ValueSet/{id}/$expand
```

## Parameters

| Parameter          | Type    | Required | Default | Limits | Description                                                                             |
| ------------------ | ------- | -------- | ------- | ------ | --------------------------------------------------------------------------------------- |
| `id`               | string  | Yes      | -       | -      | The UUID of the ValueSet/Concept to expand                                              |
| `includeHierarchy` | boolean | No       | false   | -      | If true, returns hierarchical structure with parent-child relationships via nested `contains` |
| `filter`           | string  | No       | null    | -      | Text filter to apply to concept codes and displays (case-insensitive partial match)     |
| `count`            | integer | No       | 100     | 1-1000 | Maximum number of concepts to return (applied after filtering)                           |
| `offset`           | integer | No       | 0       | ≥0     | Number of concepts to skip for pagination (applied after filtering)                      |

## Technical Implementation

### Concept Relationship Mapping

- **OpenMRS Concepts** → **FHIR ValueSet expansion**
- **Concept setMembers** → **Hierarchical parent-child relationships**
- **Concept UUID** → **FHIR code**
- **Concept displayString** → **FHIR display**

### Inactive Concept Detection

The implementation automatically detects inactive concepts and sets the `inactive` field to `true` for:

- Retired concepts (`concept.isRetired() == true`)
- Concepts with retired ConceptClass (`conceptClass.isRetired() == true`)
- Concepts with null ConceptClass (`conceptClass == null`)

### Infinite Loop Prevention

The service includes built-in protection against infinite loops through concept relationship cycles using a `processed` set to track visited concepts.

## Usage Examples

### Example 1: Basic Hierarchical Expansion

**Request:**

```http
GET /openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/$expand?includeHierarchy=true
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
    "total": 1,
    "contains": [
      {
        "code": "parent-concept-uuid",
        "display": "Cardiovascular Diseases",
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
    ]
  }
}
```

### Example 2: Flat Expansion

**Request:**

```http
GET /openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/$expand?includeHierarchy=false
```

**Response:**

```json
{
  "resourceType": "ValueSet",
  "id": "12345678-1234-1234-1234-123456789012",
  "expansion": {
    "timestamp": "2025-06-09T17:41:00.000Z",
    "total": 3,
    "contains": [
      {
        "code": "parent-concept-uuid",
        "display": "Cardiovascular Diseases"
      },
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

### Example 3: Expansion with Filter

**Request:**

```http
GET /openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/$expand?filter=hyper&includeHierarchy=false
```

**Response:**

```json
{
  "resourceType": "ValueSet",
  "id": "12345678-1234-1234-1234-123456789012",
  "expansion": {
    "timestamp": "2025-06-09T17:41:00.000Z",
    "total": 1,
    "contains": [
      {
        "code": "child-concept-uuid",
        "display": "Hypertension"
      }
    ]
  }
}
```

### Example 4: Paginated Expansion

**Request:**

```http
GET /openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/$expand?count=10&offset=5&includeHierarchy=false
```

**Response:**

```json
{
  "resourceType": "ValueSet",
  "id": "12345678-1234-1234-1234-123456789012",
  "expansion": {
    "timestamp": "2025-06-09T17:41:00.000Z",
    "total": 10,
    "contains": [
      {
        "code": "concept-6-uuid",
        "display": "Sixth Concept"
      },
      {
        "code": "concept-7-uuid", 
        "display": "Seventh Concept"
      }
    ]
  }
}
```

## cURL Examples

### Basic hierarchical expansion

```bash
curl -X GET \
  "http://localhost:8080/openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/\$expand?includeHierarchy=true" \
  -H "Authorization: Basic YWRtaW46QWRtaW4xMjM=" \
  -H "Accept: application/fhir+json"
```

### Expansion with filter and count limit

```bash
curl -X GET \
  "http://localhost:8080/openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/\$expand?filter=diabetes&includeHierarchy=false&count=20" \
  -H "Authorization: Basic YWRtaW46QWRtaW4xMjM=" \
  -H "Accept: application/fhir+json"
```

### Paginated expansion

```bash
curl -X GET \
  "http://localhost:8080/openmrs/ws/fhir2/R4/ValueSet/12345678-1234-1234-1234-123456789012/\$expand?count=50&offset=100" \
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
- `total`: Total number of concepts in the expansion (after filtering and pagination)
- `contains`: Array of concept definitions

### Hierarchical Structure (`includeHierarchy=true`)

Parent concepts include a nested `contains` array with their child concepts. The nesting preserves the OpenMRS concept setMember relationships.

### Flat Structure (`includeHierarchy=false`)

All concepts are returned at the same level without nesting. Parent and child concepts appear as siblings in the contains array.

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
- Invalid parameter values (e.g., negative offset, count > 1000)
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

1. **Use Pagination**: Always use `count` parameter for large ValueSets to avoid memory issues
2. **Server-side Filtering**: Use the `filter` parameter instead of client-side filtering for better performance
3. **Hierarchical vs Flat**: Flat expansion (`includeHierarchy=false`) is generally faster for large concept sets
4. **Caching**: Consider implementing caching for frequently accessed ValueSets

### Resource Limits

- **Maximum Count**: 1000 concepts per request
- **Default Count**: 100 concepts if not specified
- **Filtering**: Applied before pagination for optimal performance
- **Memory Usage**: Hierarchical expansion uses more memory due to nested structures

### Best Practices

```bash
# Good: Paginated request for large ValueSets
curl ".../$expand?count=50&offset=0"

# Good: Filtered request to reduce data transfer
curl ".../$expand?filter=diabetes&count=20"

# Avoid: Requesting all concepts without pagination
curl ".../$expand?count=1000"

# Avoid: Client-side filtering of large datasets
curl ".../$expand" | jq '.expansion.contains[] | select(.display | contains("diabetes"))'
```

## Integration Examples

### JavaScript/TypeScript

```typescript
interface ValueSetExpansionOptions {
  includeHierarchy?: boolean;
  filter?: string;
  count?: number;
  offset?: number;
}

async function expandValueSet(
  valueSetId: string, 
  options: ValueSetExpansionOptions = {}
): Promise<ValueSet> {
  const params = new URLSearchParams();
  
  if (options.includeHierarchy) params.set('includeHierarchy', 'true');
  if (options.filter) params.set('filter', options.filter);
  if (options.count) params.set('count', options.count.toString());
  if (options.offset) params.set('offset', options.offset.toString());
  
  const response = await fetch(
    `/openmrs/ws/fhir2/R4/ValueSet/${valueSetId}/$expand?${params}`,
    {
      headers: {
        'Authorization': 'Basic ' + btoa('admin:Admin123'),
        'Accept': 'application/fhir+json'
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  return response.json();
}

// Usage examples
const hierarchicalExpansion = await expandValueSet('concept-uuid', { 
  includeHierarchy: true 
});

const filteredExpansion = await expandValueSet('concept-uuid', {
  filter: 'diabetes',
  count: 20,
  offset: 0
});
```

### Java Client

```java
// Using HAPI FHIR Client
IGenericClient client = ctx.newRestfulGenericClient(serverBase);

// Hierarchical expansion
Parameters params = new Parameters()
    .addParameter("includeHierarchy", new BooleanType(true));
    
ValueSet expanded = client
    .operation()
    .onInstance(new IdType("ValueSet", "concept-uuid"))
    .named("$expand")
    .withParameters(params)
    .returnResourceType(ValueSet.class)
    .execute();

// Filtered expansion  
Parameters filteredParams = new Parameters()
    .addParameter("filter", new StringType("diabetes"))
    .addParameter("count", new IntegerType(50));
    
ValueSet filtered = client
    .operation()
    .onInstance(new IdType("ValueSet", "concept-uuid")) 
    .named("$expand")
    .withParameters(filteredParams)
    .returnResourceType(ValueSet.class)
    .execute();
```

## Troubleshooting

### Common Issues

1. **Empty Expansion Results**
   - Verify the concept has setMembers configured
   - Check if the concept exists and is not retired
   - Ensure proper authentication and permissions

2. **Hierarchical Structure Not Showing**
   - Confirm `includeHierarchy=true` parameter is set
   - Verify parent concepts have child concepts as setMembers (not ConceptAnswers)
   - Check for circular references that might be preventing expansion

3. **Performance Issues**
   - Reduce `count` parameter for large expansions
   - Use filtering to narrow down results
   - Consider flat expansion for better performance

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

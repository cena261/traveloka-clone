# Traveloka Search Engine API Documentation

## Overview

The Traveloka Search Engine provides comprehensive search capabilities for properties, including full-text search, geospatial search, autocomplete suggestions, analytics, and administrative functions. This implementation follows TDD principles and includes robust caching, error handling, and performance optimization.

## Architecture

### Components

1. **Controllers** (REST API Layer)
   - `SearchController` - Main search operations
   - `SuggestionController` - Autocomplete and location search
   - `IndexingController` - Search index management
   - `AnalyticsController` - Search analytics and reporting

2. **Services** (Business Logic Layer)
   - `SearchService` - Core search functionality
   - `AutoCompleteService` - Suggestion generation
   - `FilterService` - Filter validation and management
   - `IndexingService` - Elasticsearch index operations
   - `SearchAnalyticsService` - Analytics and metrics
   - `LocationSearchService` - Geospatial search operations

3. **Repositories** (Data Access Layer)
   - `PropertyElasticsearchRepository` - Elasticsearch operations
   - `SearchHistoryRepository` - Search analytics storage
   - `PopularDestinationRepository` - Destination data management

4. **Infrastructure**
   - Redis caching for performance optimization
   - Elasticsearch for full-text and geospatial search
   - PostgreSQL with PostGIS for analytics and location data

## API Endpoints

### Search Operations

#### POST /api/search/properties
Search for properties with advanced filtering and pagination.

**Request Body:**
```json
{
  "query": "luxury hotel",
  "language": "vi",
  "price": {
    "minPrice": 1000000,
    "maxPrice": 5000000,
    "currency": "VND",
    "perNight": true
  },
  "location": {
    "city": "Hà Nội",
    "countryCode": "VN",
    "latitude": 21.0285,
    "longitude": 105.8542,
    "radiusKm": 10
  },
  "property": {
    "starRating": [4, 5],
    "propertyTypes": ["hotel", "villa"],
    "amenities": ["wifi", "pool", "spa"],
    "guestRating": 4.0,
    "instantBook": true,
    "freeCancellation": true
  },
  "guests": {
    "adults": 2,
    "children": 1,
    "rooms": 1
  }
}
```

**Query Parameters:**
- `page` (default: 0) - Page number
- `size` (default: 20, max: 100) - Results per page
- `sort` (default: "score,desc") - Sort criteria

**Response:**
```json
{
  "results": [
    {
      "propertyId": "uuid",
      "name": "Hotel Name",
      "description": "Hotel description",
      "kind": "HOTEL",
      "starRating": 5,
      "location": {
        "city": "Hà Nội",
        "countryCode": "VN",
        "address": "123 Street Name",
        "coordinates": {
          "lat": 21.0285,
          "lon": 105.8542
        }
      },
      "pricing": {
        "startingPrice": 2500000,
        "currency": "VND"
      },
      "rating": {
        "average": 4.5,
        "count": 120
      },
      "amenities": ["WiFi", "Pool", "Spa"],
      "images": [
        {
          "url": "https://example.com/image.jpg",
          "alt": "Hotel image",
          "isPrimary": true
        }
      ]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  },
  "filters": {
    "applied": {
      "priceRange": "1M-5M VND",
      "location": "Hà Nội",
      "starRating": "4-5 stars"
    },
    "available": {
      "priceRanges": [...],
      "starRatings": [...],
      "propertyTypes": [...],
      "amenities": [...]
    }
  },
  "metadata": {
    "responseTimeMs": 245,
    "totalMatches": 150,
    "searchId": "uuid",
    "language": "vi",
    "cacheHit": false
  }
}
```

#### GET /api/search/filters
Get available filter options for search refinement.

**Query Parameters:**
- `query` (optional) - Base search query
- `language` (default: "vi") - Response language

**Response:**
```json
{
  "priceRanges": {
    "ranges": [
      {"label": "Under 500K VND", "min": 0, "max": 500000},
      {"label": "500K - 1M VND", "min": 500000, "max": 1000000}
    ],
    "currency": "VND"
  },
  "starRatings": {
    "options": [
      {"label": "5 Stars", "value": 5},
      {"label": "4 Stars & Up", "value": 4}
    ]
  },
  "propertyTypes": {
    "types": [
      {"label": "Hotels", "value": "hotel"},
      {"label": "Villas", "value": "villa"}
    ]
  },
  "amenities": {
    "categories": {
      "connectivity": ["WiFi", "Business Center"],
      "leisure": ["Swimming Pool", "Spa", "Fitness Center"],
      "dining": ["Restaurant", "Bar", "Room Service"]
    }
  }
}
```

### Autocomplete and Suggestions

#### GET /api/search/suggestions/autocomplete
Get autocomplete suggestions for search queries.

**Query Parameters:**
- `query` (required) - Search query to autocomplete
- `language` (default: "vi") - Response language
- `latitude` (optional) - User location latitude
- `longitude` (optional) - User location longitude
- `limit` (default: 10, max: 50) - Maximum suggestions

**Response:**
```json
{
  "suggestions": [
    {
      "text": "Hà Nội",
      "displayText": "Hà Nội, VN",
      "type": "LOCATION",
      "score": 95.0,
      "isPopular": true,
      "details": {
        "location": {
          "city": "Hà Nội",
          "countryCode": "VN",
          "locationType": "CITY",
          "coordinates": {
            "latitude": 21.0285,
            "longitude": 105.8542
          },
          "propertyCount": 285
        }
      }
    },
    {
      "text": "Hanoi Hotel",
      "displayText": "Hanoi Hotel - Hà Nội",
      "type": "PROPERTY",
      "score": 90.0,
      "details": {
        "property": {
          "id": "uuid",
          "propertyType": "hotel",
          "starRating": 4,
          "ratingAverage": 4.2,
          "reviewCount": 89,
          "startingPrice": 1200000,
          "currency": "VND",
          "isFeatured": true
        }
      }
    }
  ],
  "responseMetadata": {
    "responseTimeMs": 45,
    "cacheHit": false,
    "language": "vi",
    "suggestionCount": 2,
    "queryNormalized": "ha noi",
    "autocompleteSessionId": "uuid"
  }
}
```

### Location-Based Search

#### GET /api/search/suggestions/location
Search properties near specific coordinates.

**Query Parameters:**
- `latitude` (required) - Search center latitude
- `longitude` (required) - Search center longitude
- `radiusKm` (default: 10.0, max: 100) - Search radius
- `query` (optional) - Additional search query
- `page` (default: 0) - Page number
- `size` (default: 20, max: 100) - Results per page

#### GET /api/search/suggestions/city/{cityName}
Search properties in a specific city.

**Path Parameters:**
- `cityName` (required) - City name to search in

**Query Parameters:**
- `query` (optional) - Additional search query
- `page` (default: 0) - Page number
- `size` (default: 20, max: 100) - Results per page

### Administrative Operations

#### POST /api/search/admin/indexing/property/{propertyId}
Index a specific property in the search engine.

**Path Parameters:**
- `propertyId` (required) - UUID of property to index

#### POST /api/search/admin/indexing/property/{propertyId}/reindex
Reindex a specific property with updated data.

#### DELETE /api/search/admin/indexing/property/{propertyId}
Remove a property from the search index.

#### GET /api/search/admin/indexing/status
Get current indexing system status and statistics.

**Response:**
```json
{
  "indexedPropertiesCount": 15420,
  "indexingService": "active",
  "lastUpdated": 1634567890123
}
```

### Analytics and Reporting

#### GET /api/search/admin/analytics/performance
Get search performance metrics for a date range.

**Query Parameters:**
- `startDate` (required) - Start date (ISO 8601)
- `endDate` (required) - End date (ISO 8601)

**Response:**
```json
{
  "totalSearches": 45230,
  "conversionRate": 0.125,
  "zeroResultsRate": 0.08,
  "responseTimeBySearchType": [
    ["property_search", 245.5],
    ["location_search", 189.2]
  ],
  "deviceAnalytics": [
    ["mobile", 32145],
    ["desktop", 13085]
  ],
  "searchFrequencyByHour": [
    [0, 892], [1, 654], [2, 445]
  ]
}
```

#### GET /api/search/admin/analytics/popular-queries
Get most popular search queries.

**Query Parameters:**
- `days` (default: 30, max: 365) - Time period in days
- `minCount` (default: 5) - Minimum search count
- `limit` (default: 50, max: 500) - Maximum results

#### GET /api/search/admin/analytics/trending-queries
Get trending search queries comparing recent to historical periods.

**Query Parameters:**
- `recentDays` (default: 7) - Recent period in days
- `historicalDays` (default: 30) - Historical comparison period
- `minRecentCount` (default: 3) - Minimum recent searches
- `limit` (default: 20, max: 200) - Maximum results

## Performance Characteristics

### Response Times (95th percentile)
- Property search: < 500ms
- Autocomplete: < 100ms
- Location search: < 300ms
- Analytics queries: < 2s

### Caching Strategy
- Search results: 5 minutes TTL
- Autocomplete suggestions: 15 minutes TTL
- Filter options: 30 minutes TTL
- Analytics data: 1 hour TTL

### Rate Limits
- General API: 1000 requests/minute per client
- Autocomplete: 100 requests/minute per client
- Admin operations: 10 requests/minute per client

## Error Handling

### HTTP Status Codes
- `200 OK` - Successful operation
- `400 Bad Request` - Invalid parameters or request format
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error with graceful degradation

### Error Response Format
```json
{
  "error": {
    "code": "INVALID_PARAMETERS",
    "message": "The provided search parameters are invalid",
    "details": {
      "field": "price.minPrice",
      "violation": "must be non-negative"
    },
    "timestamp": "2023-10-15T10:30:00Z",
    "requestId": "uuid"
  }
}
```

## Security Considerations

### Input Validation
- Query length limits (1000 characters)
- Parameter sanitization against injection attacks
- Coordinate boundary validation
- Price range validation

### Access Control
- Admin endpoints require authentication
- Rate limiting per client/IP
- Request size limits (1MB max)

## Deployment and Configuration

### Environment Variables
```bash
# Elasticsearch Configuration
ELASTICSEARCH_HOSTS=localhost:9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=changeme

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=changeme

# Database Configuration
DATABASE_URL=postgresql://user:pass@localhost:5432/traveloka
DATABASE_MAX_POOL_SIZE=20

# Search Configuration
SEARCH_DEFAULT_PAGE_SIZE=20
SEARCH_MAX_PAGE_SIZE=100
SEARCH_CACHE_TTL_MINUTES=5
AUTOCOMPLETE_CACHE_TTL_MINUTES=15
```

### Health Checks
- `GET /actuator/health` - Overall system health
- `GET /actuator/health/elasticsearch` - Elasticsearch connectivity
- `GET /actuator/health/redis` - Redis connectivity
- `GET /actuator/health/db` - Database connectivity

## Monitoring and Observability

### Metrics
- Search request rate and latency
- Cache hit rates
- Elasticsearch query performance
- Error rates by endpoint
- Business metrics (conversion rates, popular queries)

### Logging
- Structured JSON logging
- Request/response correlation IDs
- Performance metrics logging
- Error tracking with stack traces
- Audit logs for admin operations

## Testing

### Test Coverage
- Unit tests: 95%+ coverage
- Integration tests: End-to-end API testing
- Performance tests: Load and stress testing
- Error handling tests: Resilience and fault tolerance

### Test Categories
- T034: Integration testing and validation
- T035: Performance optimization and caching verification
- T036: Error handling and resilience testing

## Future Enhancements

### Planned Features
- Machine learning-based personalization
- Real-time analytics dashboard
- Advanced geospatial filtering
- Multi-language search optimization
- Voice search integration

### Scalability Considerations
- Horizontal scaling with load balancers
- Elasticsearch cluster optimization
- Database sharding strategies
- CDN integration for global performance
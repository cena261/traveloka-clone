# Claude Code Context for Traveloka Backend

## Project Overview
Enterprise-scale hotel booking platform backend built with Spring Boot 3.x, PostgreSQL + PostGIS, Redis, and Elasticsearch. Focus on BROWNFIELD development extending existing architecture.

## Current Technologies
- **Backend**: Spring Boot 3.x, Maven, Java 17
- **Database**: PostgreSQL 16 + PostGIS 3.4, Flyway migrations
- **Caching**: Redis 7 for session management and data caching
- **Storage**: MinIO for S3-compatible object storage
- **Search**: Elasticsearch 9.1.3 for property search functionality
- **Auth**: Keycloak 25.0 OAuth2 Resource Server integration
- **Testing**: Spring Boot Test, Testcontainers for integration tests
- **Mapping**: MapStruct for entity-DTO transformations

## Architecture Principles
1. **Brownfield Extension**: EXTEND existing com.cena.traveloka.* structure, never recreate
2. **Module-First Development**: Follow existing domain modules (iam, catalog, search, availability, pricing, booking, payment)
3. **Database Schema Integrity**: Use Flyway migrations, maintain referential integrity across domains
4. **Test-Driven Implementation**: TDD mandatory for service layer, integration tests with Testcontainers
5. **Spring Boot Conventions**: @Service, @Repository, @Controller patterns with proper exception handling

## Current Implementation: Hotel Property & Partner Management

### Package Structure
```
com.cena.traveloka.catalog.inventory/
├── entity/              # JPA entities with relationships
├── repository/          # Spring Data JPA repositories
├── service/             # Business logic layer
├── controller/          # REST API endpoints
├── dto/                 # Data transfer objects
└── mapper/              # MapStruct entity-DTO mappers
```

### Key Entities Implemented
- **Partner**: Hotel business entity with contracts, commission tracking, performance metrics
- **Property**: Accommodation with PostGIS location, multi-language descriptions, verification workflow
- **PropertyType**: Hotel/resort/apartment classification
- **RoomType**: Room configurations with capacity, bed types, pricing
- **RoomUnit**: Individual bookable rooms with status management
- **Amenity**: Categorized facilities with filtering capabilities
- **PropertyAmenity**: Property-amenity associations with additional attributes
- **PropertyPolicy**: Check-in/out policies, cancellation rules, restrictions

### Database Schema
- **inventory schema**: Core business entities with spatial indexes
- **PostGIS geography points**: Location-based search optimization
- **Foreign key relationships**: Referential integrity across entities
- **Audit fields**: created_at, updated_at, created_by, updated_by for all entities

### API Endpoints
- **Partner Management**: Registration, contract management, performance tracking
- **Property Management**: CRUD operations, spatial search, bulk import, image upload
- **Room Management**: Room type configuration, unit management, status tracking

### Performance Optimizations
- **Redis caching**: 1-hour TTL for property data, 15-minute TTL for search results
- **PostGIS spatial indexes**: GIST indexes for efficient location queries
- **Full-text search**: tsvector indexes for property name/description search
- **Connection pooling**: Optimized database connections for high concurrency

### Integration Points
- **MinIO Storage**: Property image upload with presigned URLs
- **Redis Caching**: Distributed caching with configurable TTL policies
- **PostGIS Spatial**: Geography-based search within radius
- **Elasticsearch Prep**: Document structure ready for search indexing

## Current Implementation: Property Search Engine (002-context-brownfield-development)

### Search Module Architecture
Comprehensive property search engine extending `com.cena.traveloka.search.*` module as PRIMARY REVENUE DRIVER for Traveloka platform.

### Search Technologies
- **Elasticsearch 9.1.3**: Full-text search with Vietnamese text analyzers
- **PostGIS Spatial**: Geographic queries for "hotels near me" functionality
- **Redis Caching**: 5-minute TTL for search results, 30-minute for suggestions
- **Search Analytics**: PostgreSQL-based search pattern tracking

### Search Package Structure
```
com.cena.traveloka.search/
├── entity/              # SearchIndex, SearchHistory, PopularDestination, SearchSession
├── repository/          # Elasticsearch, PostgreSQL, Redis repositories
├── service/             # SearchService, LocationSearchService, AutoCompleteService, SearchAnalyticsService
├── controller/          # SearchController, SuggestionController
├── dto/                 # Search request/response DTOs
├── config/              # Elasticsearch, Redis configuration
└── util/                # Search utilities, ranking algorithms
```

### Key Search Entities
- **SearchIndex** (Elasticsearch): Optimized property documents with multi-language support
- **SearchHistory** (PostgreSQL): User search patterns, analytics, personalization data
- **PopularDestination** (PostgreSQL): Trending locations, search volume analytics
- **SearchSession** (PostgreSQL): Session-based search behavior tracking
- **AutoCompleteSuggestion** (Elasticsearch): Real-time suggestion index with completion suggester

### Search API Endpoints
- **POST /api/search/properties**: Full-text + location-based property search with advanced filtering
- **GET /api/search/suggestions**: Auto-complete with fuzzy matching, multi-language support
- **GET /api/search/locations**: Location-based search by coordinates with radius filtering
- **GET /api/search/popular**: Popular destinations based on analytics
- **POST /api/search/analytics**: Search interaction tracking for business intelligence
- **GET /api/search/filters**: Dynamic filter options with faceted search counts

### Search Performance Optimizations
- **Sub-500ms Response Time**: Elasticsearch + Redis caching + optimized queries
- **10,000+ Concurrent Users**: Connection pooling, Redis Cluster, async processing
- **Multi-language Support**: Vietnamese text analyzers, diacritic normalization
- **Intelligent Ranking**: Price, rating, popularity, distance-based scoring algorithms
- **Cache Strategy**: Hierarchical caching with geographic and temporal patterns

### Search Data Flow
1. **Property Updates** → Elasticsearch re-indexing → Cache invalidation
2. **User Search** → Cache check → Elasticsearch query → PostGIS spatial filtering → Result ranking
3. **Analytics Tracking** → PostgreSQL logging → Popular destination updates → Personalization

### Elasticsearch Configuration
- **Vietnamese Analyzer**: Custom tokenizer with diacritic folding, stopwords, stemming
- **Geo-point Mapping**: Spatial search with distance calculations
- **Completion Suggester**: Weighted auto-complete with contextual filtering
- **Faceted Search**: Aggregations for price, amenities, ratings, property types

### Redis Caching Strategy
- **Search Results**: `search:results:{query_hash}` (5-minute TTL)
- **Auto-complete**: `search:autocomplete:{prefix}` (30-minute TTL)
- **Popular Destinations**: `search:popular:{country_code}` (1-hour TTL)
- **Filter Counts**: `search:filters:{property_type}` (15-minute TTL)

### Integration Points
- **Inventory Module**: Real-time property data synchronization
- **Analytics Dashboard**: Search pattern visualization, conversion tracking
- **Mobile API**: Location-based search, personalization
- **SEO Optimization**: Search-friendly URLs, structured data

## Recent Changes
- 001-context-this-is: Added Hotel Property & Partner Management system with PostGIS + MinIO + Redis integration
- 002-context-brownfield-development: Implemented comprehensive property search engine with Elasticsearch 9.1.3, advanced filtering, analytics

## Development Workflow
1. **Implementation Priority**: Entity → Repository → Service → Controller → Integration
2. **Testing Strategy**: Contract tests → Integration tests → Unit tests following TDD
3. **Quality Gates**: 80% code coverage, security scans, performance benchmarks
4. **Constitutional Compliance**: All implementations follow `.specify/memory/constitution.md`

## Key Commands
- `mvn spring-boot:run` - Start application
- `mvn test` - Run test suite
- `mvn flyway:migrate` - Run database migrations
- `docker-compose up -d` - Start infrastructure (PostgreSQL, Redis, MinIO)

## Performance Standards
- Property search: < 500ms response time
- Redis cache hits: Sub-millisecond access
- Spatial queries: Logarithmic performance with GIST indexes
- Image uploads: < 2s per file to MinIO
- Database connections: Optimized pooling configuration

This context enables efficient development within the established Traveloka architecture while maintaining consistency with constitutional principles and performance requirements.
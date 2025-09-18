# Traveloka Constitution

## Core Principles

### I. Brownfield Extension (NON-NEGOTIABLE)
EXTEND existing architecture, never recreate; All implementations must work within established com.cena.traveloka.* package structure; Respect existing Spring Boot 3.x, PostgreSQL + PostGIS, Redis, Elasticsearch tech stack; Fill empty classes with business logic, do not create new files unless explicitly required

### II. Module-First Development 
Every feature implementation follows existing domain modules (iam, catalog, search, availability, pricing, booking, payment, etc.); Implement one module at a time with complete business logic; Each module must integrate seamlessly with existing cross-cutting concerns (common/, security, caching)

### III. Database Schema Integrity
Use only Flyway migrations following V1-V20 pattern; Extend existing schemas: iam, inventory, availability, pricing, booking, payment, review, notify; All spatial queries must use PostGIS extensions; Maintain referential integrity across domain boundaries

### IV. Test-Driven Implementation (NON-NEGOTIABLE)
TDD mandatory for all service layer business logic; Integration tests with Testcontainers for PostgreSQL, Redis, Elasticsearch; Unit tests for all public methods in service classes; Performance tests for search and booking critical paths

### V. Spring Boot Convention Compliance
Follow established patterns: @Service for business logic, @Repository for data access, @Controller for REST endpoints; Use MapStruct for entity-DTO mapping; Implement proper exception handling with GlobalExceptionHandler; Maintain audit logging with correlation IDs

### VI. Performance & Scalability Standards
Sub-500ms response time for search operations; Redis caching for frequently accessed data with proper TTL; Elasticsearch optimization for property search and auto-complete; Database query optimization with proper indexing; Connection pooling and resource management

### VII. Security & Compliance Requirements
OAuth2 Resource Server integration with Keycloak JWT validation; Role-based access control for partner vs customer operations; API rate limiting with Redis token bucket; PCI compliance for payment processing; Audit trails for all financial transactions

## Technology Stack Constraints

### Required Dependencies (DO NOT CHANGE)
- Spring Boot 3.x with Maven build system
- PostgreSQL 16 with PostGIS 3.4 for spatial data
- Redis 7 for caching and session management  
- Elasticsearch 9.1.3 for search functionality
- Keycloak 25.0 for authentication and authorization
- MinIO for object storage (S3-compatible)
- Docker Compose for development environment orchestration

### Integration Requirements
- MailHog for development email testing
- VNPay and MoMo payment gateway integration
- MapStruct for object mapping between layers
- Flyway for database version control
- Testcontainers for integration testing
- Jackson for JSON serialization with custom serializers

## Development Workflow

### Implementation Priority Order
1. Entity layer: Complete JPA mappings and relationships
2. Repository layer: Implement custom queries and caching
3. Service layer: Fill business logic and orchestration
4. Controller layer: REST endpoints with proper validation
5. Integration layer: External system APIs and callbacks
6. Testing layer: Comprehensive test coverage

### Quality Gates
- All PRs must pass comprehensive test suite
- Code coverage minimum 80% for service layer business logic
- Security scans for authentication and payment flows
- Performance benchmarks for critical user journeys
- Database migration reviews for schema changes

### Module Implementation Sequence
1. catalog/inventory (Property and partner management)
2. search/ (Property search with Elasticsearch)
3. availability/ (Real-time inventory management)
4. pricing/ (Dynamic pricing engine)
5. booking/ (Booking workflow and state management)
6. payment/ (Multi-gateway payment processing)
7. communication/ (Notifications and content management)
8. review/ (Rating and review system)
9. analytics/ (Tracking and reporting)
10. integration/ (External system synchronization)

## Architectural Governance

### Non-Negotiable Rules
- Constitution supersedes all other practices
- No new package structure creation without documented justification  
- All database changes via Flyway migrations only
- Redis integration required for all caching strategies
- Elasticsearch required for all search functionality
- All payment processing must be PCI compliant

### Documentation Requirements
- JavaDoc for all public service methods
- API documentation with OpenAPI 3.0
- Database schema documentation with entity relationships
- Integration guides for external systems
- Deployment guides for different environments

### Change Management
- Architecture changes require constitution amendment
- Breaking changes need migration documentation
- Performance degradation triggers architectural review
- Security vulnerabilities mandate immediate hotfix process

**Version**: 1.0.0 | **Ratified**: 2025-09-15 | **Last Amended**: 2025-09-15
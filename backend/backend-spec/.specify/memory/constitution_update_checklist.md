# Traveloka Constitution Update Checklist

When amending the constitution (`/.specify/memory/constitution.md`), ensure all dependent documents are updated to maintain consistency with the existing Traveloka brownfield architecture.

## Templates to Update

### When adding/modifying ANY article:
- [ ] `/templates/plan-template.md` - Update Brownfield Architecture Check section
- [ ] `/templates/spec-template.md` - Update if module requirements/scope affected  
- [ ] `/templates/tasks-template.md` - Update if new implementation patterns needed
- [ ] `/.claude/commands/plan.md` - Update if planning process changes
- [ ] `/.claude/commands/tasks.md` - Update if task generation affected
- [ ] `/CLAUDE.md` - Update runtime development guidelines for existing codebase

### Article-specific updates:

#### Article I (Brownfield Extension):
- [ ] Ensure templates emphasize extending existing classes only
- [ ] Update package structure references to com.cena.traveloka.*
- [ ] Add existing tech stack validation requirements
- [ ] Include empty class detection and filling procedures

#### Article II (Module-First Development):
- [ ] Update implementation order in templates  
- [ ] Add module integration checkpoints
- [ ] Emphasize domain boundary respect
- [ ] Include cross-cutting concern integration steps

#### Article III (Database Schema Integrity):
- [ ] Update Flyway migration pattern requirements
- [ ] Add PostGIS spatial query examples
- [ ] Include existing schema extension guidelines
- [ ] Reference V1-V20 migration numbering convention

#### Article IV (Test-Driven Implementation):
- [ ] Add Testcontainers setup for existing stack
- [ ] Update test priority for service layer business logic
- [ ] Include performance test requirements for critical paths
- [ ] Add integration test patterns for existing modules

#### Article V (Spring Boot Convention Compliance):
- [ ] Reference established annotation patterns
- [ ] Add MapStruct mapping requirements
- [ ] Include GlobalExceptionHandler integration
- [ ] Update audit logging with correlation ID patterns

#### Article VI (Performance & Scalability Standards):
- [ ] Add Redis caching strategy templates
- [ ] Include Elasticsearch optimization guidelines  
- [ ] Reference database indexing requirements
- [ ] Add response time monitoring templates

#### Article VII (Security & Compliance Requirements):
- [ ] Update Keycloak OAuth2 integration patterns
- [ ] Add RBAC implementation guidelines
- [ ] Include payment processing security requirements
- [ ] Reference audit trail implementation patterns

## Validation Steps

1. **Before committing constitution changes:**
   - [ ] All templates reference existing package structure
   - [ ] Examples updated to use established tech stack
   - [ ] No contradictions with brownfield principles
   - [ ] Existing class structure preservation verified

2. **After updating templates:**
   - [ ] Run through sample implementation with empty classes
   - [ ] Verify all constitution requirements addressed in existing modules
   - [ ] Check templates work with established Spring Boot patterns
   - [ ] Validate database schema extension compatibility

3. **Version tracking:**
   - [ ] Update constitution version number
   - [ ] Note version in template footers
   - [ ] Add amendment to constitution history
   - [ ] Update module implementation sequence if needed

## Common Misses for Brownfield Projects

Watch for these often-forgotten updates:
- Existing class reference documentation (`/docs/existing-structure.md`)
- Empty class identification checklists in templates
- Package structure validation examples
- Tech stack version constraints (Spring Boot 3.x, PostgreSQL 16, etc.)
- Integration patterns with established infrastructure
- Migration path documentation for schema changes

## Module-Specific Checklist Items

### catalog/inventory module:
- [ ] Property and Partner entity completion guidelines
- [ ] JPA relationship mapping with existing schemas
- [ ] MinIO integration for property images

### search/ module:
- [ ] Elasticsearch 9.1.3 integration patterns
- [ ] PostGIS spatial query templates
- [ ] Redis caching for search results

### availability/ module:
- [ ] Real-time inventory checking with partitioned tables
- [ ] Redis-based inventory locking mechanisms
- [ ] Calendar optimization strategies

### booking/ module:
- [ ] State machine implementation in existing workflow classes
- [ ] Payment gateway orchestration patterns
- [ ] Notification trigger integration

### payment/ module:
- [ ] VNPay and MoMo integration compliance
- [ ] PCI security requirement validation
- [ ] Transaction audit trail requirements

## Template Sync Status

Last sync check: 2025-09-15
- Constitution version: 1.0.0  
- Templates aligned: ✅ (initial brownfield setup)
- Existing codebase compatibility: ✅ (validated against com.cena.traveloka structure)

## Integration with Existing Infrastructure

### Docker Compose Validation:
- [ ] All services running (PostgreSQL, Redis, Elasticsearch, Keycloak, MinIO)
- [ ] Port configurations match application-dev.yml
- [ ] Health checks passing for all dependencies

### Database Schema Validation:
- [ ] Flyway migrations V1-V20 structure maintained
- [ ] Existing schemas (iam, inventory, etc.) preserved
- [ ] PostGIS extensions properly configured

### Spring Boot Configuration Validation:
- [ ] OAuth2 Resource Server with Keycloak integration
- [ ] Redis configuration for caching and sessions
- [ ] Elasticsearch client configuration
- [ ] MinIO S3-compatible storage setup

---
*This checklist ensures the constitution's principles are consistently applied across the existing Traveloka brownfield architecture while maintaining system integrity and established patterns.*
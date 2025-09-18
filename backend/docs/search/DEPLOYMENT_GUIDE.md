# Search Engine Deployment Guide

## Prerequisites

### System Requirements
- Java 17 or later
- Maven 3.8+
- Docker and Docker Compose
- Minimum 8GB RAM
- 50GB available disk space

### External Dependencies
- Elasticsearch 8.x cluster
- Redis 6.x or later
- PostgreSQL 12+ with PostGIS extension
- Load balancer (nginx/HAProxy)

## Environment Setup

### 1. Database Setup
```sql
-- Create database
CREATE DATABASE traveloka_search;

-- Enable PostGIS extension
\c traveloka_search;
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_topology;

-- Create search analytics schema
CREATE SCHEMA search_analytics;

-- Create tables (run migration scripts)
\i src/main/resources/db/migration/V001__create_search_tables.sql
```

### 2. Elasticsearch Configuration
```yaml
# elasticsearch.yml
cluster.name: traveloka-search
node.name: search-node-1

network.host: 0.0.0.0
http.port: 9200

discovery.seed_hosts: ["elasticsearch-1", "elasticsearch-2", "elasticsearch-3"]
cluster.initial_master_nodes: ["search-node-1", "search-node-2", "search-node-3"]

# Enable security
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
```

### 3. Redis Configuration
```conf
# redis.conf
bind 0.0.0.0
port 6379
requirepass changeme

# Memory settings
maxmemory 2gb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000
```

## Application Configuration

### 1. Production Configuration
```yaml
# application-production.yml
spring:
  profiles:
    active: production

  elasticsearch:
    hosts:
      - elasticsearch-1:9200
      - elasticsearch-2:9200
      - elasticsearch-3:9200
    username: elastic
    password: ${ELASTICSEARCH_PASSWORD}
    connection-timeout: 30s
    socket-timeout: 60s

  redis:
    cluster:
      nodes:
        - redis-1:6379
        - redis-2:6379
        - redis-3:6379
    password: ${REDIS_PASSWORD}
    timeout: 5s
    lettuce:
      pool:
        max-active: 20
        max-wait: -1ms

  datasource:
    url: jdbc:postgresql://postgres-cluster:5432/traveloka_search
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

# Search Engine Configuration
search:
  elasticsearch:
    index-prefix: traveloka
    refresh-interval: 30s
    number-of-shards: 3
    number-of-replicas: 2

  cache:
    search-results-ttl: 300
    suggestions-ttl: 900
    filters-ttl: 1800
    analytics-ttl: 3600

  performance:
    max-page-size: 100
    default-page-size: 20
    query-timeout: 30000
    bulk-size: 1000

  analytics:
    retention-days: 365
    cleanup-enabled: true
    cleanup-schedule: "0 2 * * *"

# Logging Configuration
logging:
  level:
    com.cena.traveloka.search: INFO
    org.elasticsearch: WARN
    org.springframework.data.elasticsearch: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{requestId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{requestId}] %logger{36} - %msg%n"
  file:
    name: logs/search-engine.log
    max-size: 100MB
    max-history: 30

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2. Docker Compose Deployment
```yaml
# docker-compose.yml
version: '3.8'

services:
  search-engine:
    image: traveloka/search-engine:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: production
      ELASTICSEARCH_HOSTS: elasticsearch-1:9200,elasticsearch-2:9200,elasticsearch-3:9200
      ELASTICSEARCH_PASSWORD: ${ELASTICSEARCH_PASSWORD}
      REDIS_CLUSTER_NODES: redis-1:6379,redis-2:6379,redis-3:6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      DATABASE_URL: jdbc:postgresql://postgres:5432/traveloka_search
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
    depends_on:
      - elasticsearch-1
      - redis-1
      - postgres
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  elasticsearch-1:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.9.0
    environment:
      - cluster.name=traveloka-search
      - node.name=elasticsearch-1
      - discovery.seed_hosts=elasticsearch-2,elasticsearch-3
      - cluster.initial_master_nodes=elasticsearch-1,elasticsearch-2,elasticsearch-3
      - xpack.security.enabled=true
      - ELASTIC_PASSWORD=${ELASTICSEARCH_PASSWORD}
    volumes:
      - es1_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    mem_limit: 2g

  elasticsearch-2:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.9.0
    environment:
      - cluster.name=traveloka-search
      - node.name=elasticsearch-2
      - discovery.seed_hosts=elasticsearch-1,elasticsearch-3
      - cluster.initial_master_nodes=elasticsearch-1,elasticsearch-2,elasticsearch-3
      - xpack.security.enabled=true
      - ELASTIC_PASSWORD=${ELASTICSEARCH_PASSWORD}
    volumes:
      - es2_data:/usr/share/elasticsearch/data
    ulimits:
      memlock:
        soft: -1
        hard: -1
    mem_limit: 2g

  elasticsearch-3:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.9.0
    environment:
      - cluster.name=traveloka-search
      - node.name=elasticsearch-3
      - discovery.seed_hosts=elasticsearch-1,elasticsearch-2
      - cluster.initial_master_nodes=elasticsearch-1,elasticsearch-2,elasticsearch-3
      - xpack.security.enabled=true
      - ELASTIC_PASSWORD=${ELASTICSEARCH_PASSWORD}
    volumes:
      - es3_data:/usr/share/elasticsearch/data
    ulimits:
      memlock:
        soft: -1
        hard: -1
    mem_limit: 2g

  redis-1:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis1_data:/data
    ports:
      - "6379:6379"

  redis-2:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis2_data:/data
    ports:
      - "6380:6379"

  redis-3:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis3_data:/data
    ports:
      - "6381:6379"

  postgres:
    image: postgis/postgis:14-3.2
    environment:
      POSTGRES_DB: traveloka_search
      POSTGRES_USER: ${DATABASE_USERNAME}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

volumes:
  es1_data:
  es2_data:
  es3_data:
  redis1_data:
  redis2_data:
  redis3_data:
  postgres_data:
```

## Deployment Steps

### 1. Build Application
```bash
# Clone repository
git clone https://github.com/traveloka/search-engine.git
cd search-engine

# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t traveloka/search-engine:latest .
```

### 2. Deploy Infrastructure
```bash
# Set environment variables
export ELASTICSEARCH_PASSWORD=your_secure_password
export REDIS_PASSWORD=your_secure_password
export DATABASE_USERNAME=traveloka
export DATABASE_PASSWORD=your_secure_password

# Deploy with Docker Compose
docker-compose up -d

# Wait for services to be healthy
docker-compose ps
```

### 3. Initialize Search Indices
```bash
# Wait for Elasticsearch to be ready
curl -u elastic:$ELASTICSEARCH_PASSWORD http://localhost:9200/_cluster/health

# Create search indices (automatic on first startup)
curl -X POST http://localhost:8080/api/search/admin/indexing/refresh-all
```

### 4. Verify Deployment
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Test search functionality
curl -X POST http://localhost:8080/api/search/properties \
  -H "Content-Type: application/json" \
  -d '{"query": "hotel", "language": "vi"}'

# Test autocomplete
curl "http://localhost:8080/api/search/suggestions/autocomplete?query=hotel&language=vi"
```

## Kubernetes Deployment

### 1. Namespace and Secrets
```yaml
# namespace.yml
apiVersion: v1
kind: Namespace
metadata:
  name: search-engine

---
# secrets.yml
apiVersion: v1
kind: Secret
metadata:
  name: search-engine-secrets
  namespace: search-engine
type: Opaque
data:
  elasticsearch-password: <base64-encoded-password>
  redis-password: <base64-encoded-password>
  database-username: <base64-encoded-username>
  database-password: <base64-encoded-password>
```

### 2. Application Deployment
```yaml
# deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: search-engine
  namespace: search-engine
spec:
  replicas: 3
  selector:
    matchLabels:
      app: search-engine
  template:
    metadata:
      labels:
        app: search-engine
    spec:
      containers:
      - name: search-engine
        image: traveloka/search-engine:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: ELASTICSEARCH_HOSTS
          value: "elasticsearch-service:9200"
        - name: ELASTICSEARCH_PASSWORD
          valueFrom:
            secretKeyRef:
              name: search-engine-secrets
              key: elasticsearch-password
        - name: REDIS_HOST
          value: "redis-service"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: search-engine-secrets
              key: redis-password
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres-service:5432/traveloka_search"
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: search-engine-secrets
              key: database-username
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: search-engine-secrets
              key: database-password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30

---
# service.yml
apiVersion: v1
kind: Service
metadata:
  name: search-engine-service
  namespace: search-engine
spec:
  selector:
    app: search-engine
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP

---
# ingress.yml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: search-engine-ingress
  namespace: search-engine
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/rate-limit: "1000"
spec:
  rules:
  - host: search-api.traveloka.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: search-engine-service
            port:
              number: 80
```

## Monitoring and Observability

### 1. Prometheus Configuration
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'search-engine'
    static_configs:
      - targets: ['search-engine:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
```

### 2. Grafana Dashboards
Import the provided Grafana dashboard JSON files:
- `grafana/search-performance-dashboard.json`
- `grafana/search-analytics-dashboard.json`
- `grafana/system-metrics-dashboard.json`

### 3. Log Aggregation
```yaml
# fluent-bit.conf
[INPUT]
    Name tail
    Path /var/log/search-engine/*.log
    Parser json
    Tag search.engine

[FILTER]
    Name grep
    Match search.engine
    Regex log_level (ERROR|WARN|INFO)

[OUTPUT]
    Name elasticsearch
    Match search.engine
    Host elasticsearch
    Port 9200
    Index search-logs
    Type _doc
```

## Performance Tuning

### 1. JVM Configuration
```bash
# JVM options
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+PrintGC \
  -XX:+PrintGCDetails \
  -XX:+UseStringDeduplication"
```

### 2. Elasticsearch Tuning
```yaml
# elasticsearch.yml additions
indices.memory.index_buffer_size: 20%
indices.queries.cache.size: 20%
indices.fielddata.cache.size: 30%
thread_pool.search.size: 30
thread_pool.search.queue_size: 1000
```

### 3. Redis Tuning
```conf
# redis.conf additions
tcp-keepalive 300
timeout 0
tcp-backlog 511
databases 16
```

## Security Configuration

### 1. SSL/TLS Setup
```yaml
# application-production.yml additions
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: search-engine
```

### 2. API Security
```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/search/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/search/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);

        return http.build();
    }
}
```

## Backup and Recovery

### 1. Database Backup
```bash
#!/bin/bash
# backup-database.sh
pg_dump -h postgres -U traveloka traveloka_search | \
  gzip > backups/db-$(date +%Y%m%d-%H%M%S).sql.gz
```

### 2. Elasticsearch Snapshot
```bash
#!/bin/bash
# backup-elasticsearch.sh
curl -X PUT "elasticsearch:9200/_snapshot/backup_repo/snapshot_$(date +%Y%m%d)" \
  -H "Content-Type: application/json" \
  -d '{"indices": "traveloka_*"}'
```

## Troubleshooting

### Common Issues

1. **High Memory Usage**
   - Check JVM heap size and GC logs
   - Monitor Elasticsearch field data cache
   - Verify Redis memory usage

2. **Slow Search Performance**
   - Check Elasticsearch cluster health
   - Verify index mapping and queries
   - Monitor cache hit rates

3. **Connection Timeouts**
   - Verify network connectivity
   - Check connection pool settings
   - Monitor thread pool usage

### Debug Commands
```bash
# Application logs
docker-compose logs -f search-engine

# Elasticsearch cluster status
curl -u elastic:password http://localhost:9200/_cluster/health?pretty

# Redis memory usage
docker-compose exec redis-1 redis-cli --raw info memory

# Database connections
docker-compose exec postgres psql -U traveloka -d traveloka_search -c "SELECT * FROM pg_stat_activity;"
```

## Maintenance Tasks

### Daily
- Monitor application and infrastructure health
- Check error rates and performance metrics
- Verify backup completion

### Weekly
- Review and analyze search analytics
- Update trending destination scores
- Check and clean up old log files

### Monthly
- Update dependencies and security patches
- Review and optimize Elasticsearch indices
- Analyze capacity and scaling requirements
- Clean up old analytics data (automated)
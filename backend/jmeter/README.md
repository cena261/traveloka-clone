# JMeter Performance Testing - Traveloka IAM

## Overview

Performance testing suite for the IAM (Identity & Access Management) module using Apache JMeter.

## Test Configuration

- **Concurrent Users**: 1000
- **Ramp-up Time**: 60 seconds
- **Test Duration**: 300 seconds (5 minutes)
- **Tested Endpoints**:
  - POST `/api/v1/auth/login`
  - GET `/api/v1/users/me`

## Prerequisites

1. **Apache JMeter 5.6+**
   - Download from: https://jmeter.apache.org/download_jmeter.cgi
   - Extract to a directory (e.g., `C:\jmeter` or `/opt/jmeter`)

2. **Running Backend**
   - Ensure the Traveloka backend is running on `localhost:8080`
   - Database, Redis, and Keycloak must be available

3. **Test Data**
   - The test creates unique user emails using `test${threadNum}@example.com`
   - Password: `TestPassword123!`
   - Ensure test users exist or modify the test to register users first

## Running the Tests

### GUI Mode (for test development)

```bash
# Windows
cd C:\path\to\apache-jmeter\bin
jmeter.bat

# Linux/Mac
cd /opt/apache-jmeter/bin
./jmeter.sh
```

Then:
1. Open JMeter GUI
2. File → Open → Select `IAM-Performance-Test.jmx`
3. Configure variables if needed
4. Click the green "Start" button

### CLI Mode (for actual performance testing)

```bash
# Windows
jmeter.bat -n -t IAM-Performance-Test.jmx -l results.jtl -e -o report-output

# Linux/Mac
./jmeter.sh -n -t IAM-Performance-Test.jmx -l results.jtl -e -o report-output
```

Parameters:
- `-n`: Non-GUI mode
- `-t`: Test file path
- `-l`: Results file (JTL format)
- `-e`: Generate HTML report
- `-o`: Output directory for HTML report

### Custom Configuration

Override variables from command line:

```bash
jmeter.sh -n -t IAM-Performance-Test.jmx \\
  -JBASE_URL=staging.traveloka.com \\
  -JPORT=443 \\
  -JTHREADS=2000 \\
  -JRAMP_TIME=120 \\
  -JDURATION=600 \\
  -l results-2000-users.jtl \\
  -e -o report-2000-users
```

## Test Scenarios

### Scenario 1: Login Flow
1. User sends POST request to `/api/v1/auth/login`
2. Extract `accessToken` from JSON response
3. Assert HTTP 200 response
4. Wait 1 second (think time)

### Scenario 2: Authenticated Request
1. User sends GET request to `/api/v1/users/me`
2. Include `Authorization: Bearer {token}` header
3. Verify successful response

## Performance Metrics

### Key Performance Indicators (KPIs)

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Average Response Time | < 200ms | 200-500ms | > 500ms |
| 95th Percentile | < 500ms | 500ms-1s | > 1s |
| 99th Percentile | < 1s | 1-2s | > 2s |
| Error Rate | < 0.1% | 0.1-1% | > 1% |
| Throughput | > 500 req/s | 200-500 req/s | < 200 req/s |

### Expected Results (1000 concurrent users)

Based on requirements:
- **Max Response Time**: < 1 second (NFR-003)
- **Throughput**: 10,000+ concurrent users support
- **Error Rate**: < 0.1%
- **CPU Usage**: < 70%
- **Memory Usage**: < 80%

## Analyzing Results

### Summary Report
The `Summary Report` listener shows:
- **Samples**: Total requests executed
- **Average**: Mean response time
- **Min/Max**: Response time range
- **Std. Dev**: Response time standard deviation
- **Error %**: Percentage of failed requests
- **Throughput**: Requests per second
- **KB/sec**: Data transferred per second

### HTML Report
Generated HTML report includes:
- **Dashboard**: Overview with graphs
- **Statistics**: Detailed metrics per request
- **Errors**: Failed requests with details
- **Response Times Over Time**: Trend graph
- **Active Threads Over Time**: Load profile

### Interpreting Results

**Good Performance**:
```
Login Endpoint:
- Average: 150ms
- 95th %ile: 300ms
- Error Rate: 0%
- Throughput: 800 req/s
```

**Poor Performance**:
```
Login Endpoint:
- Average: 1200ms
- 95th %ile: 3000ms
- Error Rate: 5%
- Throughput: 150 req/s
```

## Troubleshooting

### High Error Rate

Check:
1. Backend logs for exceptions
2. Database connection pool exhaustion
3. Redis connection issues
4. Rate limiting configuration
5. Keycloak availability

### High Response Times

Potential causes:
1. Database query optimization needed
2. Insufficient connection pool size
3. Redis latency
4. CPU/Memory saturation
5. Network latency

### Connection Refused

Ensure:
1. Backend is running: `curl http://localhost:8080/actuator/health`
2. Firewall allows connections
3. Correct host/port in test variables

## Load Profile Recommendations

### Development/Testing
- Users: 100
- Ramp-up: 10s
- Duration: 60s

### Staging Validation
- Users: 500
- Ramp-up: 30s
- Duration: 180s

### Production Readiness
- Users: 1000
- Ramp-up: 60s
- Duration: 300s

### Stress Testing
- Users: 2000+
- Ramp-up: 120s
- Duration: 600s

## Monitoring During Tests

### Backend Monitoring

Monitor these metrics:
```bash
# CPU and Memory
docker stats

# Logs
tail -f logs/application.log

# Database connections
SELECT count(*) FROM pg_stat_activity;

# Redis connections
redis-cli INFO clients
```

### System Resources

Use monitoring tools:
- **Prometheus + Grafana**: Real-time dashboards
- **JConsole/VisualVM**: JVM metrics
- **htop/top**: System resource usage

## Best Practices

1. **Warm-up Period**: Run a smaller load first to warm up caches
2. **Realistic Data**: Use production-like test data
3. **Think Time**: Add realistic pauses between requests
4. **Correlation**: Extract dynamic values (tokens, IDs)
5. **Assertions**: Validate response correctness, not just status codes
6. **Gradual Load**: Use ramp-up to simulate realistic user arrival
7. **Test Isolation**: Run on dedicated environment to avoid interference

## Results Archive

Save test results for comparison:
```
results/
├── 2025-01-05_baseline_1000users.jtl
├── 2025-01-05_baseline_report/
├── 2025-01-12_optimization_1000users.jtl
└── 2025-01-12_optimization_report/
```

## Next Steps

After running baseline tests:
1. Identify bottlenecks
2. Optimize code/queries
3. Tune configuration (pools, caching)
4. Re-run tests to measure improvement
5. Document performance baseline
6. Set up continuous performance testing

## References

- [JMeter User Manual](https://jmeter.apache.org/usermanual/index.html)
- [JMeter Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)
- [IAM Module Documentation](../src/main/java/com/cena/traveloka/iam/README.md)

## Support

For questions or issues:
- Performance Team: performance@traveloka.com
- IAM Team: iam-team@traveloka.com

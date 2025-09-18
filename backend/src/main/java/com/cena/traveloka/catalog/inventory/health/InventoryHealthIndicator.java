package com.cena.traveloka.catalog.inventory.health;

import com.cena.traveloka.catalog.inventory.repository.PartnerRepository;
import com.cena.traveloka.catalog.inventory.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryHealthIndicator implements HealthIndicator {

    private final PartnerRepository partnerRepository;
    private final PropertyRepository propertyRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // Check database connectivity
            long partnerCount = partnerRepository.count();
            long propertyCount = propertyRepository.count();

            // Check Redis connectivity
            redisTemplate.opsForValue().set("health-check", "ok");
            String redisCheck = (String) redisTemplate.opsForValue().get("health-check");

            if (!"ok".equals(redisCheck)) {
                return Health.down()
                        .withDetail("redis", "Connection failed")
                        .build();
            }

            return Health.up()
                    .withDetail("database", "Connected")
                    .withDetail("redis", "Connected")
                    .withDetail("partners", partnerCount)
                    .withDetail("properties", propertyCount)
                    .build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
package com.cena.traveloka.search.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {

    public <T> T get(String key, Class<T> type) {
        // Mock cache implementation
        log.debug("Getting cache key: {}", key);
        return null;
    }

    public void put(String key, Object value) {
        // Mock cache implementation
        log.debug("Putting cache key: {}", key);
    }

    public void evict(String key) {
        // Mock cache implementation
        log.debug("Evicting cache key: {}", key);
    }
}
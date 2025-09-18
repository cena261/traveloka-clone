package com.cena.traveloka.search.repository;

import com.cena.traveloka.search.entity.SearchIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyElasticsearchRepository extends ElasticsearchRepository<SearchIndex, UUID> {
    // Custom Elasticsearch queries can be added here
}
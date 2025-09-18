package com.cena.traveloka.search.repository;

import com.cena.traveloka.search.entity.SearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyElasticsearchRepository extends ElasticsearchRepository<SearchIndex, UUID> {

    @Query("""
        {
          "bool": {
            "should": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": [
                    "name^3",
                    "name.suggest^2",
                    "city^2",
                    "city.suggest^1.5",
                    "description.*^1"
                  ],
                  "type": "best_fields",
                  "fuzziness": "AUTO",
                  "operator": "or"
                }
              },
              {
                "match": {
                  "name.keyword": {
                    "query": "?0",
                    "boost": 4
                  }
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    SearchHits<SearchIndex> searchByQuery(String query, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": [
                    "name^3",
                    "name.suggest^2",
                    "city^2",
                    "city.suggest^1.5",
                    "description.*^1"
                  ],
                  "type": "best_fields",
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              {
                "geo_distance": {
                  "distance": "?3km",
                  "location.coordinates": {
                    "lat": ?1,
                    "lon": ?2
                  }
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> searchByQueryAndLocation(String query, Double latitude, Double longitude, Double radiusKm, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "city^2", "description.*^1"],
                  "type": "best_fields"
                }
              }
            ],
            "filter": [
              {
                "terms": {
                  "star_rating": ?1
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> searchByQueryAndStarRating(String query, List<Integer> starRatings, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "city^2", "description.*^1"],
                  "type": "best_fields"
                }
              }
            ],
            "filter": [
              {
                "range": {
                  "room_types.base_price": {
                    "gte": ?1,
                    "lte": ?2
                  }
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> searchByQueryAndPriceRange(String query, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "city^2", "description.*^1"],
                  "type": "best_fields"
                }
              }
            ],
            "filter": [
              {
                "nested": {
                  "path": "amenities",
                  "query": {
                    "terms": {
                      "amenities.name.keyword": ?1
                    }
                  }
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> searchByQueryAndAmenities(String query, List<String> amenities, Pageable pageable);

    @Query("""
        {
          "bool": {
            "filter": [
              {
                "geo_distance": {
                  "distance": "?2km",
                  "location.coordinates": {
                    "lat": ?0,
                    "lon": ?1
                  }
                }
              }
            ]
          },
          "sort": [
            {
              "_geo_distance": {
                "location.coordinates": {
                  "lat": ?0,
                  "lon": ?1
                },
                "order": "asc",
                "unit": "km"
              }
            }
          ]
        }
        """)
    SearchHits<SearchIndex> findByLocationWithinRadius(Double latitude, Double longitude, Double radiusKm, Pageable pageable);

    @Query("""
        {
          "bool": {
            "filter": [
              {
                "terms": {
                  "city.keyword": ?0
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> findByCity(List<String> cities, Pageable pageable);

    @Query("""
        {
          "bool": {
            "filter": [
              {
                "range": {
                  "rating_avg": {
                    "gte": ?0
                  }
                }
              },
              {
                "range": {
                  "rating_count": {
                    "gte": ?1
                  }
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> findByMinRatingAndMinReviews(BigDecimal minRating, Integer minReviews, Pageable pageable);

    @Query("""
        {
          "bool": {
            "filter": [
              {
                "terms": {
                  "kind": ?0
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> findByPropertyTypes(List<String> propertyTypes, Pageable pageable);

    @Query("""
        {
          "match": {
            "name.suggest": {
              "query": "?0",
              "analyzer": "vietnamese_analyzer"
            }
          }
        }
        """)
    List<SearchIndex> findSuggestionsByName(String query);

    @Query("""
        {
          "match": {
            "city.suggest": {
              "query": "?0",
              "analyzer": "vietnamese_analyzer"
            }
          }
        }
        """)
    List<SearchIndex> findSuggestionsByCity(String query);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "match_all": {}
              }
            ],
            "filter": [
              {
                "term": {
                  "search_boost.is_promoted": true
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> findPromotedProperties(Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "match_all": {}
              }
            ],
            "sort": [
              {
                "search_boost.popularity_score": {
                  "order": "desc"
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> findMostPopular(Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name", "city", "description.*"],
                  "type": "best_fields",
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              {
                "geo_distance": {
                  "distance": "?3km",
                  "location.coordinates": {
                    "lat": ?1,
                    "lon": ?2
                  }
                }
              },
              {
                "range": {
                  "room_types.base_price": {
                    "gte": ?4,
                    "lte": ?5
                  }
                }
              },
              {
                "terms": {
                  "star_rating": ?6
                }
              }
            ]
          }
        }
        """)
    SearchHits<SearchIndex> searchWithComplexFilters(
            String query,
            Double latitude,
            Double longitude,
            Double radiusKm,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<Integer> starRatings,
            Pageable pageable
    );

    // Aggregation queries for faceted search
    @Query("""
        {
          "size": 0,
          "aggs": {
            "price_ranges": {
              "range": {
                "field": "room_types.base_price",
                "ranges": [
                  {"to": 500000},
                  {"from": 500000, "to": 1000000},
                  {"from": 1000000, "to": 2000000},
                  {"from": 2000000}
                ]
              }
            },
            "star_ratings": {
              "terms": {
                "field": "star_rating"
              }
            },
            "cities": {
              "terms": {
                "field": "city.keyword",
                "size": 20
              }
            },
            "amenities": {
              "nested": {
                "path": "amenities"
              },
              "aggs": {
                "amenity_names": {
                  "terms": {
                    "field": "amenities.name.keyword",
                    "size": 50
                  }
                }
              }
            }
          }
        }
        """)
    SearchHits<SearchIndex> getSearchAggregations();
}

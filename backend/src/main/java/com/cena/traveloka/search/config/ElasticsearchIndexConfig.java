package com.cena.traveloka.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.StringReader;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexConfig {

    private final ElasticsearchClient elasticsearchClient;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndicesOnStartup() {
        try {
            createPropertySearchIndex();
            createSuggestionIndex();
            log.info("Elasticsearch indices created successfully");
        } catch (Exception e) {
            log.error("Failed to create Elasticsearch indices", e);
        }
    }

    private void createPropertySearchIndex() throws Exception {
        String indexName = "traveloka-properties";

        if (!indexExists(indexName)) {
            String indexMapping = """
                {
                  "settings": {
                    "number_of_shards": 2,
                    "number_of_replicas": 1,
                    "analysis": {
                      "analyzer": {
                        "vietnamese_analyzer": {
                          "type": "custom",
                          "tokenizer": "standard",
                          "filter": [
                            "lowercase",
                            "asciifolding",
                            "vietnamese_stop",
                            "vietnamese_stemmer"
                          ]
                        },
                        "vietnamese_search_analyzer": {
                          "type": "custom",
                          "tokenizer": "standard",
                          "filter": [
                            "lowercase",
                            "asciifolding",
                            "vietnamese_stop"
                          ]
                        }
                      },
                      "filter": {
                        "vietnamese_stop": {
                          "type": "stop",
                          "stopwords": ["và", "của", "các", "trong", "với", "từ", "cho", "đến", "một", "có", "là", "được", "không", "này", "đó", "những", "để", "như", "sẽ", "về", "tại", "đã", "hay", "hoặc", "nếu", "khi", "mà", "nhưng", "vì", "do", "theo", "trên", "dưới", "giữa", "sau", "trước"]
                        },
                        "vietnamese_stemmer": {
                          "type": "stemmer",
                          "language": "light_minimal"
                        }
                      }
                    }
                  },
                  "mappings": {
                    "properties": {
                      "propertyId": {
                        "type": "keyword"
                      },
                      "name": {
                        "type": "text",
                        "analyzer": "vietnamese_analyzer",
                        "search_analyzer": "vietnamese_search_analyzer",
                        "fields": {
                          "raw": {
                            "type": "keyword"
                          },
                          "suggest": {
                            "type": "completion",
                            "analyzer": "vietnamese_analyzer"
                          }
                        }
                      },
                      "description": {
                        "type": "object",
                        "properties": {
                          "vi": {
                            "type": "text",
                            "analyzer": "vietnamese_analyzer",
                            "search_analyzer": "vietnamese_search_analyzer"
                          },
                          "en": {
                            "type": "text",
                            "analyzer": "english"
                          }
                        }
                      },
                      "location": {
                        "type": "object",
                        "properties": {
                          "coordinates": {
                            "type": "geo_point"
                          },
                          "city": {
                            "type": "text",
                            "analyzer": "vietnamese_analyzer",
                            "search_analyzer": "vietnamese_search_analyzer",
                            "fields": {
                              "raw": {
                                "type": "keyword"
                              }
                            }
                          },
                          "countryCode": {
                            "type": "keyword"
                          },
                          "address": {
                            "type": "text",
                            "analyzer": "vietnamese_analyzer",
                            "search_analyzer": "vietnamese_search_analyzer"
                          }
                        }
                      },
                      "amenities": {
                        "type": "nested",
                        "properties": {
                          "id": {
                            "type": "keyword"
                          },
                          "name": {
                            "type": "text",
                            "analyzer": "vietnamese_analyzer",
                            "search_analyzer": "vietnamese_search_analyzer"
                          },
                          "category": {
                            "type": "keyword"
                          }
                        }
                      },
                      "propertyType": {
                        "type": "keyword"
                      },
                      "starRating": {
                        "type": "integer"
                      },
                      "priceRange": {
                        "type": "object",
                        "properties": {
                          "minPrice": {
                            "type": "double"
                          },
                          "maxPrice": {
                            "type": "double"
                          },
                          "currency": {
                            "type": "keyword"
                          }
                        }
                      },
                      "ratings": {
                        "type": "object",
                        "properties": {
                          "average": {
                            "type": "double"
                          },
                          "count": {
                            "type": "integer"
                          }
                        }
                      },
                      "availability": {
                        "type": "object",
                        "properties": {
                          "totalRooms": {
                            "type": "integer"
                          },
                          "isActive": {
                            "type": "boolean"
                          }
                        }
                      },
                      "searchMetrics": {
                        "type": "object",
                        "properties": {
                          "popularityScore": {
                            "type": "double"
                          },
                          "bookingConversionRate": {
                            "type": "double"
                          },
                          "lastUpdated": {
                            "type": "date"
                          }
                        }
                      }
                    }
                  }
                }
                """;

            CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                    .index(indexName)
                    .withJson(new StringReader(indexMapping))
            );

            elasticsearchClient.indices().create(request);
            log.info("Created property search index: {}", indexName);
        }
    }

    private void createSuggestionIndex() throws Exception {
        String indexName = "traveloka-suggestions";

        if (!indexExists(indexName)) {
            String indexMapping = """
                {
                  "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 1,
                    "analysis": {
                      "analyzer": {
                        "suggestion_analyzer": {
                          "type": "custom",
                          "tokenizer": "standard",
                          "filter": [
                            "lowercase",
                            "asciifolding"
                          ]
                        }
                      }
                    }
                  },
                  "mappings": {
                    "properties": {
                      "suggestionId": {
                        "type": "keyword"
                      },
                      "suggestionText": {
                        "type": "text",
                        "analyzer": "vietnamese_analyzer"
                      },
                      "suggestionType": {
                        "type": "keyword"
                      },
                      "completionData": {
                        "type": "completion",
                        "analyzer": "suggestion_analyzer",
                        "contexts": [
                          {
                            "name": "location",
                            "type": "geo"
                          },
                          {
                            "name": "type",
                            "type": "category"
                          }
                        ]
                      },
                      "locationData": {
                        "type": "object",
                        "properties": {
                          "coordinates": {
                            "type": "geo_point"
                          },
                          "countryCode": {
                            "type": "keyword"
                          }
                        }
                      },
                      "popularityMetrics": {
                        "type": "object",
                        "properties": {
                          "searchFrequency": {
                            "type": "integer"
                          },
                          "conversionRate": {
                            "type": "double"
                          },
                          "lastUsed": {
                            "type": "date"
                          }
                        }
                      },
                      "isActive": {
                        "type": "boolean"
                      }
                    }
                  }
                }
                """;

            CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                    .index(indexName)
                    .withJson(new StringReader(indexMapping))
            );

            elasticsearchClient.indices().create(request);
            log.info("Created suggestion index: {}", indexName);
        }
    }

    private boolean indexExists(String indexName) throws Exception {
        ExistsRequest request = ExistsRequest.of(builder -> builder.index(indexName));
        return elasticsearchClient.indices().exists(request).value();
    }
}

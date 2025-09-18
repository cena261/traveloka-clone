package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class SuggestionResponse {
    private List<String> suggestions;
    private String query;
    private Integer totalCount;
    private String suggestionType;
}
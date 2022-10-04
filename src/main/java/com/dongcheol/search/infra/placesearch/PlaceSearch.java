package com.dongcheol.search.infra.placesearch;

import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface PlaceSearch {

    Mono<PlaceSearchResp> search(String query, int page, int size,
        MultiValueMap<String, String> params);
}

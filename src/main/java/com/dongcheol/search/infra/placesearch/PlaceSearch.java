package com.dongcheol.search.infra.placesearch;

import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import reactor.core.publisher.Mono;

public interface PlaceSearch {

    Mono<PlaceSearchResp> search(String query);
}

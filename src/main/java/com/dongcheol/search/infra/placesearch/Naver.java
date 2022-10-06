package com.dongcheol.search.infra.placesearch;

import com.dongcheol.search.infra.placesearch.dto.PlaceSearchItem;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import com.dongcheol.search.infra.placesearch.type.ApiType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Qualifier("naverApi")
public class Naver implements PlaceSearch {


    private static final String BASE_URL = "https://openapi.naver.com/v1/search/local.json";
    private static final int DEFAULT_START = 1;
    private final WebClient webClient;

    public Naver(
        @Value("${naverapi.place.client-id}") String clientId,
        @Value("${naverapi.place.client-secret}") String clientSecret
    ) {
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("X-Naver-Client-Id", clientId)
            .defaultHeader("X-Naver-Client-Secret", clientSecret)
            .build();
    }

    public Mono<PlaceSearchResp> search(
        String query,
        int page,
        int size,
        MultiValueMap<String, String> params
    ) {
        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("query", query)
                .queryParam("display", size)
                .queryParam("start", page)
                .queryParams(params)
                .build()
            )
            .retrieve()
            .onStatus(HttpStatus::isError, response ->
                Mono.error(
                    new WebClientResponseException(
                        response.rawStatusCode(),
                        new StringBuilder("네이버 지역 검색 API 요청 오류 ")
                            .append(response.bodyToMono(String.class))
                            .toString(),
                        response.headers().asHttpHeaders(), null, null
                    )
                )
            )
            .bodyToMono(String.class)
            .flatMap(this::bodyToPlaceResp)
            .doOnError(throwable -> log.error("검색 에러", throwable));
    }

    private Mono<PlaceSearchResp> bodyToPlaceResp(String body) {
        log.debug("네이버 API 응답 바디: " + body);
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> data = mapper.readValue(body, Map.class);
            List<Map<String, String>> documents = (List) data.get("items");
            List<PlaceSearchItem> placeSearchItems = documents.stream()
                .map(map -> {
                    map.put(
                        "title",
                        map.get("title")
                            .replaceAll("<b>", "")
                            .replaceAll("</b>", "")
                    );
                    return map;
                })
                .map(map ->
                    PlaceSearchItem.builder()
                        .title(map.get("title"))
                        .address(map.get("address"))
                        .roadAddress(map.get("roadAddress"))
                        .build())
                .collect(Collectors.toList());

            PlaceSearchResp resp = PlaceSearchResp.builder()
                .apiType(ApiType.NAVER)
                .itemCount(placeSearchItems.size())
                .items(placeSearchItems)
                .success(true)
                .build();

            return Mono.just(resp);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

    }
}

package com.dongcheol.search.infra.place;

import com.dongcheol.search.infra.place.dto.PlaceInfo;
import com.dongcheol.search.infra.place.dto.PlaceResp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class Naver {

    private static final Logger LOGGER = LoggerFactory.getLogger(Naver.class);

    private static final String BASE_URL = "https://openapi.naver.com/v1/search/local.json";
    private static final int SEARCH_MAX_SIZE = 5;
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

    public Mono<PlaceResp> search(String query) {
        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("query", query)
                .queryParam("display", SEARCH_MAX_SIZE)
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
            .doOnError(throwable -> LOGGER.error("검색 에러", throwable))
            .onErrorResume(e -> Mono.empty());
    }

    private Mono<PlaceResp> bodyToPlaceResp(String body) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> data = mapper.readValue(body, Map.class);
            List<Map<String, String>> documents = (List) data.get("items");
            List<PlaceInfo> placeInfos = documents.stream()
                .map(map ->
                    PlaceInfo.builder()
                        .name(map.get("title"))
                        .address(map.get("address"))
                        .roadAddress(map.get("roadAddress"))
                        .build()
                )
                .collect(Collectors.toList());

            PlaceResp resp = PlaceResp.builder()
                .apiType("naver")
                .result(placeInfos)
                .build();

            return Mono.just(resp);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

    }
}

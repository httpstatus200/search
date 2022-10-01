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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class Kakao {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kakao.class);
    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword.JSON";
    private static final int SEARCH_MAX_SIZE = 5;

    private final WebClient webClient;

    public Kakao(@Value("${kakaoapi.place.key}") String apiKey) {
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader(
                HttpHeaders.AUTHORIZATION,
                new StringBuilder("KakaoAK ").append(apiKey).toString()
            )
            .build();
    }

    public Mono<PlaceResp> search(String query) {
        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("query", query)
                .queryParam("size", SEARCH_MAX_SIZE)
                .build()
            )
            .retrieve()
            .onStatus(HttpStatus::isError, response ->
                Mono.error(
                    new WebClientResponseException(
                        response.rawStatusCode(),
                        new StringBuilder("카카오 로컬 API 요청 오류 ")
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
            List<Map<String, String>> documents = (List) data.get("documents");
            List<PlaceInfo> placeInfos = documents.stream()
                .map(map ->
                    PlaceInfo.builder()
                        .name(map.get("place_name"))
                        .address(map.get("address_name"))
                        .roadAddress(map.get("road_address_name"))
                        .build()
                )
                .collect(Collectors.toList());

            PlaceResp resp = PlaceResp.builder()
                .apiType("kakao")
                .result(placeInfos)
                .build();

            return Mono.just(resp);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
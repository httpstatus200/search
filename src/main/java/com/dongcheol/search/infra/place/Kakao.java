package com.dongcheol.search.infra.place;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class Kakao {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kakao.class);
    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword";
    private static final int SEARCH_MAX_SIZE = 5;

    private final WebClient webClient;

    public Kakao(@Value("${kakaoapi.place.key}") String apiKey) {
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, String.format("KakaoAK %s", apiKey))
            .build();
    }

    public Mono<String> search(String query) {
        return this.webClient.get()
            .uri(String.format("?query=%s&size=%d", query, SEARCH_MAX_SIZE))
            .retrieve()
            .bodyToMono(String.class);
    }
}

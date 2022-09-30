package com.dongcheol.search.infra.place;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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

    public Mono<String> search(String query) {
        return this.webClient.get()
            .uri(String.format("?query=%s&display=%d", query, SEARCH_MAX_SIZE))
            .retrieve()
            .bodyToMono(String.class);
    }
}

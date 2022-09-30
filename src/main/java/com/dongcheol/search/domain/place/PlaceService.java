package com.dongcheol.search.domain.place;

import com.dongcheol.search.infra.place.Kakao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceService.class);
    private Kakao kakaoApi;

    public PlaceService(Kakao kakaoApi) {
        this.kakaoApi = kakaoApi;
    }

    public void searchPlace(String query) {
        String kakaoResult = kakaoApi.search(query).block();
        LOGGER.info("kakao: " + kakaoResult);
    }
}

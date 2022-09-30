package com.dongcheol.search.domain.place;

import com.dongcheol.search.infra.place.Kakao;
import com.dongcheol.search.infra.place.Naver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceService.class);
    private Kakao kakaoApi;
    private Naver naverApi;

    public PlaceService(Kakao kakaoApi, Naver naverApi) {
        this.kakaoApi = kakaoApi;
        this.naverApi = naverApi;
    }

    public void searchPlace(String query) {
//        String kakaoResult = kakaoApi.search(query).block();
//        LOGGER.info("kakao: " + kakaoResult);
        String naverResult = naverApi.search(query).block();
        LOGGER.info("naver: " + naverResult);
    }
}

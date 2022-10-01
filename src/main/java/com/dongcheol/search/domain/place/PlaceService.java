package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceInfo;
import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.infra.placesearch.PlaceSearch;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class PlaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceService.class);
    private PlaceSearch kakaoApi;
    private PlaceSearch naverApi;

    public PlaceService(
        @Qualifier("kakaoApi") PlaceSearch kakaoApi,
        @Qualifier("naverApi") PlaceSearch naverApi
    ) {
        this.kakaoApi = kakaoApi;
        this.naverApi = naverApi;
    }

    public PlaceResp searchPlace(String query) {
        CountDownLatch countDownLatch = new CountDownLatch(2);

        Map<String, PlaceSearchResp> placeRespMap = new HashMap<>();
        long start = System.currentTimeMillis();
        Flux.merge(
                naverApi.search(query)
                    .onErrorReturn(PlaceSearchResp.createFailResp("naver")),
                kakaoApi.search(query)
                    .onErrorReturn(PlaceSearchResp.createFailResp("kakao"))
            )
            .parallel()
            .runOn(Schedulers.parallel())
            .subscribe(data -> {
                LOGGER.debug(data.getApiType() + " API result=" + data);
                if (data.isSuccess()) {
                    placeRespMap.put(data.getApiType(), data);
                }
                countDownLatch.countDown();
            });

        try {
            countDownLatch.await(10, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - start;
            LOGGER.debug("place APIs execution time=" + duration + "ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        LOGGER.debug("placeRespMap size=" + placeRespMap.size());
        if (placeRespMap.size() == 0) {
            return PlaceResp.builder()
                .places(new ArrayList<>())
                .build();
        }

        if (placeRespMap.size() == 1) {
            List<PlaceInfo> places = placeRespMap.values()
                .stream()
                .map(value ->
                    value.getItems()
                        .stream()
                        .map(item -> PlaceInfo
                            .builder()
                            .title(item.getTitle())
                            .address(item.getAddress())
                            .roadAddress(item.getRoadAddress())
                            .provider(value.getApiType())
                            .build()
                        )
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList())
                .get(0);

            return PlaceResp.builder().places(places).build();
        }

        return null;
    }
}

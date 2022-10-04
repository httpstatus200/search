package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceInfo;
import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.domain.place.dto.PopularQuery;
import com.dongcheol.search.domain.place.dto.PopularQueryResp;
import com.dongcheol.search.infra.logservice.PlaceQueryLogger;
import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import com.dongcheol.search.infra.placesearch.ApiTypeEnum;
import com.dongcheol.search.infra.placesearch.PlaceSearch;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
    private QueryLogCountRepository queryLogCountRepository;
    private PlaceSearch kakaoApi;
    private PlaceSearch naverApi;
    private PlaceQueryLogger queryLogger;

    public PlaceService(
        QueryLogCountRepository queryLogCountRepository,
        @Qualifier("kakaoApi") PlaceSearch kakaoApi,
        @Qualifier("naverApi") PlaceSearch naverApi,
        PlaceQueryLogger placeQueryLogger
    ) {
        this.queryLogCountRepository = queryLogCountRepository;
        this.kakaoApi = kakaoApi;
        this.naverApi = naverApi;
        this.queryLogger = placeQueryLogger;
    }

    public PlaceResp searchPlace(String query) {
        this.queryLogger.put(new PlaceQueryLog(query));

        CountDownLatch countDownLatch = new CountDownLatch(2);

        Map<String, List<PlaceInfo>> apiResultMap = new HashMap<>();
        long start = System.currentTimeMillis();
        Flux.merge(
                naverApi.search(query)
                    .onErrorReturn(PlaceSearchResp.createFailResp(ApiTypeEnum.NAVER)),
                kakaoApi.search(query)
                    .onErrorReturn(PlaceSearchResp.createFailResp(ApiTypeEnum.KAKAO))
            )
            .parallel()
            .runOn(Schedulers.parallel())
            .subscribe(resp -> {
                LOGGER.debug(resp.getApiType() + " API resp=" + resp);
                if (resp.isSuccess()) {
                    List<PlaceInfo> placeInfos = resp.getItems()
                        .stream()
                        .map(item ->
                            PlaceInfo
                                .builder()
                                .title(item.getTitle())
                                .address(item.getAddress())
                                .roadAddress(item.getRoadAddress())
                                .provider(resp.getApiType().getName())
                                .build()
                        )
                        .collect(Collectors.toCollection(() -> new LinkedList<>()));

                    apiResultMap.put(resp.getApiType().getName(), placeInfos);
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

        LOGGER.debug("api result map size=" + apiResultMap.size());
        if (apiResultMap.size() == 0) {
            return PlaceResp.builder()
                .places(new ArrayList<>())
                .build();
        }

        if (apiResultMap.size() == 1) {
            List<PlaceInfo> places = apiResultMap.values().iterator().next();
            return PlaceResp.builder().places(places).build();
        }

        List<PlaceInfo> kakaoResult = apiResultMap.get(ApiTypeEnum.KAKAO.getName());
        List<PlaceInfo> naverResult = apiResultMap.get(ApiTypeEnum.NAVER.getName());

        List<PlaceInfo> orderedPlaces = new ArrayList();
        Iterator<PlaceInfo> kIter = kakaoResult.iterator();
        while (kIter.hasNext()) {
            PlaceInfo kPlace = kIter.next();

            Iterator<PlaceInfo> nIter = naverResult.iterator();
            while (nIter.hasNext()) {
                PlaceInfo nPlace = nIter.next();
                String kTitle = kPlace.getTitle().replaceAll(" ", "");
                String nTitle = nPlace.getTitle().replaceAll(" ", "");
                if (kTitle.equals(nTitle)) {
                    orderedPlaces.add(kPlace);
                    kIter.remove();
                    nIter.remove();
                    break;
                }
            }
        }

        LOGGER.debug("kakaoResult size=" + kakaoResult.size());
        LOGGER.debug("naverResult size=" + naverResult.size());
        kakaoResult.stream().forEach(orderedPlaces::add);
        naverResult.stream().forEach(orderedPlaces::add);

        return PlaceResp.builder().places(orderedPlaces).build();
    }

    public PopularQueryResp queryTop10() {
        List<QueryLogCount> logCounts = this.queryLogCountRepository.findTop10ByOrderByCountDesc();
        List<PopularQuery> popularQueries = logCounts.stream()
            .map(lc -> new PopularQuery(lc.getQuery(), lc.getCount()))
            .collect(Collectors.toList());

        return new PopularQueryResp(10, popularQueries);
    }
}

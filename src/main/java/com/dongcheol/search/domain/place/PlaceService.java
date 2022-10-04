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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PlaceService {

    private QueryLogCountRepository queryLogCountRepository;
    private PlaceSearch kakaoApi;
    private PlaceSearch naverApi;
    private PlaceQueryLogger queryLogger;

    private static final Map<ApiTypeEnum, ApiTypeEnum> spareApiMapper = new HashMap() {{
        put(ApiTypeEnum.NAVER, ApiTypeEnum.KAKAO);
        put(ApiTypeEnum.KAKAO, ApiTypeEnum.NAVER);
    }};

    private static final ApiTypeEnum[] API_PRIORITY = {ApiTypeEnum.KAKAO, ApiTypeEnum.NAVER};

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

        long start = System.currentTimeMillis();

        List<PlaceSearchResp> respList = Flux.merge(
                createApiCaller(ApiTypeEnum.KAKAO, query),
                createApiCaller(ApiTypeEnum.NAVER, query)
            )
            .collectList()
            .block();

        // 우선순위로 정렬
        List<PlaceInfo> placeInfos = new ArrayList<>();
        Map<ApiTypeEnum, List<PlaceInfo>> apiResultMap = classifyApiData(respList);
        for (ApiTypeEnum type : API_PRIORITY) {
            placeInfos.addAll(apiResultMap.get(type));
        }

        List<PlaceInfo> result = new ArrayList<>();
        Map<String, Boolean> map = new HashMap<>();
        placeInfos.forEach(placeInfo -> {
            String title = placeInfo.getTitle().replaceAll(" ", "");
            if (map.containsKey(title)) {
                return;
            }

            map.put(title, true);
            result.add(placeInfo);
        });

        return PlaceResp.builder().places(result).build();
    }

    private Mono<PlaceSearchResp> createApiCaller(ApiTypeEnum type, String query) {
        switch (type) {
            case NAVER:
                return naverApi.search(query)
                    .onErrorReturn(PlaceSearchResp.createFailResp(type));
            default:
                return kakaoApi.search(query)
                    .onErrorReturn(PlaceSearchResp.createFailResp(type));
        }
    }

    private Map<ApiTypeEnum, List<PlaceInfo>> classifyApiData(List<PlaceSearchResp> respList) {
        Map<ApiTypeEnum, List<PlaceInfo>> apiResultMap = new HashMap<>();
        respList.stream()
            .forEach(resp -> {
                List<PlaceInfo> placeInfos = apiRespToPlaceInfoList(resp);
                apiResultMap.put(resp.getApiType(), placeInfos);
            });

        return apiResultMap;
    }

    private List<PlaceInfo> apiRespToPlaceInfoList(PlaceSearchResp resp) {
        return resp.getItems()
            .stream()
            .map(item ->
                PlaceInfo.builder()
                    .title(item.getTitle())
                    .address(item.getAddress())
                    .roadAddress(item.getRoadAddress())
                    .provider(resp.getApiType().getName())
                    .build()
            )
            .collect(Collectors.toCollection(() -> new LinkedList<>()));
    }

    public PopularQueryResp queryTop10() {
        List<QueryLogCount> logCounts = this.queryLogCountRepository.findTop10ByOrderByCountDesc();
        List<PopularQuery> popularQueries = logCounts.stream()
            .map(lc -> new PopularQuery(lc.getQuery(), lc.getCount()))
            .collect(Collectors.toList());

        return new PopularQueryResp(10, popularQueries);
    }
}

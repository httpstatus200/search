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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PlaceService {

    private final QueryLogCountRepository queryLogCountRepository;
    private final PlaceSearch kakaoApi;
    private final PlaceSearch naverApi;
    private PlaceQueryLogger queryLogger;

    private static final Map<ApiTypeEnum, ApiTypeEnum> spareApiMapper = new HashMap() {{
        put(ApiTypeEnum.NAVER, ApiTypeEnum.KAKAO);
        put(ApiTypeEnum.KAKAO, ApiTypeEnum.NAVER);
    }};

    private static final ApiTypeEnum[] API_PRIORITY = {ApiTypeEnum.KAKAO, ApiTypeEnum.NAVER};

    private static final int DEFAULT_API_PAGE = 1;
    private static final int DEFAULT_API_SIZE = 5;

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

        List<PlaceSearchResp> respList = Flux.merge(
                createApiCaller(ApiTypeEnum.KAKAO, query, DEFAULT_API_PAGE, DEFAULT_API_SIZE, null),
                createApiCaller(ApiTypeEnum.NAVER, query, DEFAULT_API_PAGE, DEFAULT_API_SIZE, null)
            )
            .collectList()
            .block();

        Map<ApiTypeEnum, List<PlaceInfo>> apiResultMap = classifyApiData(respList);

        // 실패 케이스 찾기
        Map<ApiTypeEnum, PlaceSearchResp> failedApis = respList.stream()
            .filter(resp -> !resp.isSuccess())
            .collect(Collectors.toMap(
                resp -> resp.getApiType(),
                resp -> resp
            ));

        // 조회 미달 케이스 찾기
        Map<ApiTypeEnum, PlaceSearchResp> lackedApis = respList.stream()
            .filter(resp -> resp.isSuccess() && resp.getItems().size() < 5)
            .collect(Collectors.toMap(
                resp -> resp.getApiType(),
                resp -> resp
            ));

        // 실패 건 재조회 요청
        failedApis.entrySet()
            .stream()
            .forEach(entry -> {
                ApiTypeEnum type = entry.getKey();
                ApiTypeEnum spareType = spareApiMapper.get(type);
                if (lackedApis.containsKey(spareType) || failedApis.containsKey(spareType)) {
                    return;
                }

                MultiValueMap<String, String> params = null;
                if (spareType == ApiTypeEnum.NAVER) {
                    params = new LinkedMultiValueMap<String, String>() {{
                        put("sort", new ArrayList<>());
                        add("sort", "comment");
                    }};
                }
                PlaceSearchResp resp = createApiCaller(
                    type,
                    query,
                    DEFAULT_API_PAGE + 1,
                    DEFAULT_API_SIZE,
                    params
                ).block();
                if (resp.isSuccess()) {
                    List<PlaceInfo> result = apiRespToPlaceInfoList(resp);
                    apiResultMap.get(type).addAll(result);
                } else {
                    // 실패
                }
            });

        // 부족한 결과 재조회 요청
        lackedApis.entrySet()
            .stream()
            .forEach(entry -> {
                ApiTypeEnum type = entry.getKey();
                ApiTypeEnum spareType = spareApiMapper.get(type);
                if (lackedApis.containsKey(spareType) || failedApis.containsKey(spareType)) {
                    return;
                }

                MultiValueMap<String, String> params = null;
                if (spareType == ApiTypeEnum.NAVER) {
                    params = new LinkedMultiValueMap<String, String>() {{
                        put("sort", new ArrayList<>());
                        add("sort", "comment");
                    }};
                }
                PlaceSearchResp resp = createApiCaller(
                    spareType,
                    query,
                    DEFAULT_API_PAGE + 1,
                    DEFAULT_API_SIZE,
                    params
                ).block();
                if (resp.isSuccess()) {
                    List<PlaceInfo> result = apiRespToPlaceInfoList(resp);

                    int needCnt = DEFAULT_API_SIZE - entry.getValue().getItemCount();

                    Iterator<PlaceInfo> iter = result.iterator();
                    int idx = 0;
                    while (iter.hasNext() && idx < needCnt) {
                        apiResultMap.get(spareType).add(iter.next());
                        idx += 1;
                    }
                } else {
                    // 실패
                }
            });

        List<PlaceInfo> placeInfos = new ArrayList<>();
        for (ApiTypeEnum type : API_PRIORITY) {
            placeInfos.addAll(apiResultMap.get(type));
        }

        Map<String, PlaceInfoCounter> dupCountMap = new LinkedHashMap<>();
        placeInfos.forEach(placeInfo -> {
            String key = placeInfo.getTitle().replaceAll(" ", "");
            if (dupCountMap.containsKey(key)) {
                dupCountMap.get(key).increase();
                return;
            }

            dupCountMap.put(key, new PlaceInfoCounter(placeInfo));
        });

        List<PlaceInfo> result = dupCountMap.values()
            .stream()
            .sorted((v1, v2) -> (Integer.compare(v1.count, v2.count)) * -1)
            .map(v -> v.placeInfo)
            .collect(Collectors.toList());

        return PlaceResp.builder().places(result).build();
    }

    private Mono<PlaceSearchResp> createApiCaller(
        ApiTypeEnum type,
        String query,
        int page,
        int size,
        MultiValueMap<String, String> params
    ) {
        switch (type) {
            case NAVER:
                return naverApi.search(query, page, size, params)
                    .onErrorReturn(PlaceSearchResp.createFailResp(type));
            default:
                return kakaoApi.search(query, page, size, params)
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

    private class PlaceInfoCounter {

        PlaceInfo placeInfo;
        int count;

        public PlaceInfoCounter(PlaceInfo placeInfo) {
            this.placeInfo = placeInfo;
            this.count = 1;
        }

        public void increase() {
            this.count += 1;
        }
    }
}

package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceInfo;
import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.domain.place.dto.PopularQuery;
import com.dongcheol.search.domain.place.dto.PopularQueryResp;
import com.dongcheol.search.global.exception.ExternalApiException;
import com.dongcheol.search.global.type.ErrorCode;
import com.dongcheol.search.infra.logservice.PlaceQueryLogService;
import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import com.dongcheol.search.infra.placesearch.ApiTypeEnum;
import com.dongcheol.search.infra.placesearch.PlaceSearch;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
    private final PlaceQueryLogService logService;
    private final CacheManager cacheManager;

    private static final Map<ApiTypeEnum, ApiTypeEnum> SPARE_API_MAPPER = new HashMap() {{
        put(ApiTypeEnum.NAVER, ApiTypeEnum.KAKAO);
        put(ApiTypeEnum.KAKAO, ApiTypeEnum.NAVER);
    }};

    private static final ApiTypeEnum[] API_PRIORITY = {ApiTypeEnum.KAKAO, ApiTypeEnum.NAVER};

    private static final int DEFAULT_API_PAGE = 1;
    private static final int DEFAULT_API_SIZE = 5;

    private static final String PLACES_CACHE = "places";

    public PlaceService(
        QueryLogCountRepository queryLogCountRepository,
        @Qualifier("kakaoApi") PlaceSearch kakaoApi,
        @Qualifier("naverApi") PlaceSearch naverApi,
        PlaceQueryLogService placeQueryLogService,
        CacheManager cacheManager
    ) {
        this.queryLogCountRepository = queryLogCountRepository;
        this.kakaoApi = kakaoApi;
        this.naverApi = naverApi;
        this.logService = placeQueryLogService;
        this.cacheManager = cacheManager;
    }

    public PlaceResp searchPlace(String query) {
        PlaceQueryLog qLog = new PlaceQueryLog(query);
        try {
            this.logService.put(qLog);
        } catch (InterruptedException e) {
            log.error("쿼리 로그 저장 실패: " + e);
            // TODO: 저장 실패건 직접 저장 스토어 필요
        }

        Cache cache = this.cacheManager.getCache(this.PLACES_CACHE);
        PlaceResp cachedResp = cache.get(query, PlaceResp.class);
        if (cachedResp != null) {
            log.debug("Cache hit. query=" + query);
            return cachedResp;
        }

        Long start = System.currentTimeMillis();
        List<PlaceSearchResp> respList = Flux.merge(
                createApiCaller(ApiTypeEnum.KAKAO, query, DEFAULT_API_PAGE, DEFAULT_API_SIZE, null),
                createApiCaller(ApiTypeEnum.NAVER, query, DEFAULT_API_PAGE, DEFAULT_API_SIZE, null)
            )
            .collectList()
            .block();

        Map<ApiTypeEnum, List<PlaceInfo>> apiResultMap = classifyApiData(respList);
        Map<ApiTypeEnum, PlaceSearchResp> failedApis = respList.stream()
            .filter(resp -> !resp.isSuccess())
            .collect(Collectors.toMap(resp -> resp.getApiType(), resp -> resp));

        if (failedApis.size() == apiResultMap.size()) {
            throw new ExternalApiException("요청한 모든 API가 실패했습니다.", ErrorCode.EXTERNAL_API_ERRROR);
        }

        Map<ApiTypeEnum, PlaceSearchResp> lackedApis = respList.stream()
            .filter(resp -> resp.isSuccess() && resp.getItems().size() < 5)
            .collect(Collectors.toMap(resp -> resp.getApiType(), resp -> resp));

        ensureApiResponses(failedApis, lackedApis, query)
            .entrySet()
            .stream()
            .forEach(entry -> {
                List<PlaceInfo> infoList = apiResultMap.get(entry.getKey());

                // 네이버의 경우 페이징 지원이 안되기 때문에 정렬을 방식을 변경해서 가져온다.
                // 이때 중복된 값이 들어올 수도 있기 때문에 중복값 필터링을 진행한다.
                if (entry.getKey() == ApiTypeEnum.NAVER) {
                    List<PlaceInfo> newList = entry.getValue();
                    Map<String, Boolean> infoMap = infoList.stream()
                        .collect(Collectors.toMap(p -> samePlaceCheckKey(p), p -> true));

                    List<PlaceInfo> naverResult = newList.stream()
                        .filter(info -> !infoMap.containsKey(samePlaceCheckKey(info)))
                        .collect(Collectors.toList());

                    infoList.addAll(naverResult);
                    return;
                }

                infoList.addAll(entry.getValue());
            });

        List<PlaceInfo> placesSortedPriority = new ArrayList<>();
        for (ApiTypeEnum type : API_PRIORITY) {
            placesSortedPriority.addAll(apiResultMap.get(type));
        }

        Map<String, PlaceInfoCounter> dupCountMap = new LinkedHashMap<>();
        placesSortedPriority.forEach(placeInfo -> {
            String key = samePlaceCheckKey(placeInfo);
            if (dupCountMap.containsKey(key)) {
                dupCountMap.get(key).increase();
                return;
            }

            dupCountMap.put(key, new PlaceInfoCounter(placeInfo));
        });

        List<PlaceInfo> sortedList = dupCountMap.values()
            .stream()
            .sorted((v1, v2) -> {
                if (v1.count >= 2 && v2.count >= 2) {
                    // API 추가 시 중복 카운트가 2 이상일 수 있다.
                    // 이때 기존 Kakao 응답 결과가 바뀔 수 있음.
                    // ex A:1, B:2, C:3 (값:카운트) 일떄 A-C-B가 되는걸 예방하기 위한 조건
                    return 0;
                }
                return (Integer.compare(v1.count, v2.count)) * -1;
            })
            .map(v -> v.placeInfo)
            .collect(Collectors.toList());

        Long end = System.currentTimeMillis() - start;
        log.debug("검색 처리 요청 시간: " + end + "ms");

        PlaceResp result = PlaceResp.builder()
            .places(sortedList)
            .itemCount(sortedList.size())
            .build();

        cache.put(query, result);
        return result;
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

    private Map<ApiTypeEnum, List<PlaceInfo>> ensureApiResponses(
        Map<ApiTypeEnum, PlaceSearchResp> failedApis,
        Map<ApiTypeEnum, PlaceSearchResp> lackedApis,
        String query
    ) {
        Map<ApiTypeEnum, List<PlaceInfo>> apiResultMap = new HashMap<>();

        failedApis.entrySet()
            .stream()
            .forEach(entry -> {
                ApiTypeEnum spareType = SPARE_API_MAPPER.get(entry.getKey());
                if (lackedApis.containsKey(spareType) || failedApis.containsKey(spareType)) {
                    return;
                }

                PlaceSearchResp resp = createSpareApiCaller(spareType, query).block();
                if (resp.isSuccess()) {
                    List<PlaceInfo> result = apiRespToPlaceInfoList(resp);
                    apiResultMap.put(spareType, result);
                } else {
                    // 실패
                }
            });

        lackedApis.entrySet()
            .stream()
            .forEach(entry -> {
                ApiTypeEnum spareType = SPARE_API_MAPPER.get(entry.getKey());
                if (lackedApis.containsKey(spareType) || failedApis.containsKey(spareType)) {
                    return;
                }

                PlaceSearchResp resp = createSpareApiCaller(spareType, query).block();
                if (resp.isSuccess()) {
                    List<PlaceInfo> result = apiRespToPlaceInfoList(resp);

                    int needCnt = DEFAULT_API_SIZE - entry.getValue().getItemCount();

                    Iterator<PlaceInfo> iter = result.iterator();
                    int idx = 0;
                    while (iter.hasNext() && idx < needCnt) {
                        apiResultMap.put(spareType, result);
                        idx += 1;
                    }
                } else {
                    // 실패
                }
            });

        return apiResultMap;
    }

    private Mono<PlaceSearchResp> createSpareApiCaller(
        ApiTypeEnum spareType,
        String query
    ) {
        MultiValueMap<String, String> params = null;
        if (spareType == ApiTypeEnum.NAVER) {
            params = new LinkedMultiValueMap<String, String>() {{
                put("sort", new ArrayList<>());
                add("sort", "comment");
            }};
        }
        return createApiCaller(
            spareType,
            query,
            DEFAULT_API_PAGE + 1,
            DEFAULT_API_SIZE,
            params
        );
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
            .collect(Collectors.toCollection(() -> new ArrayList<>()));
    }

    private String samePlaceCheckKey(PlaceInfo info) {
        return info.getTitle().replaceAll(" ", "");
    }

    public PopularQueryResp queryTop10() {
        Cache cache = this.cacheManager.getCache(PLACES_CACHE);
        PopularQueryResp cachedData = cache.get("queryTop10", PopularQueryResp.class);
        if (cachedData != null) {
            return cachedData;
        }

        List<QueryLogCount> logCounts = this.queryLogCountRepository.findTop10ByOrderByCountDesc();
        List<PopularQuery> popularQueries = logCounts.stream()
            .map(lc -> new PopularQuery(lc.getQuery(), lc.getCount()))
            .collect(Collectors.toList());

        return new PopularQueryResp(popularQueries.size(), popularQueries);
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

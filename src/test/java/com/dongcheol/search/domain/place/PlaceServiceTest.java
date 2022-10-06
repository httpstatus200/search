package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.global.exception.ExternalApiException;
import com.dongcheol.search.infra.logservice.PlaceQueryLogService;
import com.dongcheol.search.infra.placesearch.PlaceSearch;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchItem;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import com.dongcheol.search.infra.placesearch.type.ApiType;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@Import({PlaceService.class})
public class PlaceServiceTest {

    @Autowired
    private PlaceService placeService;

    @MockBean
    private QueryLogCountRepository queryLogCountRepository;
    @MockBean
    @Qualifier("kakaoApi")
    private PlaceSearch kakaoApi;

    @MockBean
    @Qualifier("naverApi")
    private PlaceSearch naverApi;

    @MockBean
    private PlaceQueryLogService queryLogger;

    @MockBean
    private CacheManager cacheManager;

    @BeforeEach
    public void beforeEach() {
        Cache mockCache = new Cache() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public Object getNativeCache() {
                return null;
            }

            @Override
            public ValueWrapper get(Object key) {
                return null;
            }

            @Override
            public <T> T get(Object key, Class<T> type) {
                return null;
            }

            @Override
            public <T> T get(Object key, Callable<T> valueLoader) {
                return null;
            }

            @Override
            public void put(Object key, Object value) {
            }

            @Override
            public void evict(Object key) {
            }

            @Override
            public void clear() {
            }
        };

        Mockito.when(this.cacheManager.getCache("places"))
            .thenReturn(mockCache);
    }

    @Test
    public void Search_EmptyResult_When_FailedExternalApis() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp(ApiType.KAKAO)
                )
            );
        Mockito.when(naverApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp(ApiType.NAVER)
                )
            );

        Assertions.assertThrows(ExternalApiException.class, () -> placeService.searchPlace(query));
    }

    @Test
    public void Search_HasResult_When_SuccOneApi() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.KAKAO)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(
                                    PlaceSearchItem.builder()
                                        .title("국민은행")
                                        .address("서울시")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("신한은행")
                                        .address("서울시")
                                        .build()
                                );
                            }}
                        )
                        .build()
                )
            );
        Mockito.when(naverApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp(ApiType.NAVER)
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getItemCount(), 2);
    }

    @Test
    public void Search_SortedResult_When_HasSamePlaces() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.KAKAO)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("A", "A", "A"));
                                add(new PlaceSearchItem("B", "B", "B"));
                                add(new PlaceSearchItem("C", "C", "C"));
                                add(new PlaceSearchItem("D", "D", "D"));
                            }}
                        )
                        .build()
                )
            );
        Mockito.when(naverApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.NAVER)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("A", "A", "A"));
                                add(new PlaceSearchItem("E", "E", "E"));
                                add(new PlaceSearchItem("D", "D", "D"));
                                add(new PlaceSearchItem("C", "C", "C"));
                            }}
                        )
                        .build()
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getItemCount(), 5);
        String result = resp.getPlaces()
            .stream()
            .map(p -> p.getTitle())
            .collect(Collectors.joining("-"));
        Assertions.assertEquals(result, "A-C-D-B-E");
    }

    @Test
    public void Search_Retry_When_ApiFailed() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.KAKAO)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("A", "A", "A"));
                                add(new PlaceSearchItem("B", "B", "B"));
                                add(new PlaceSearchItem("C", "C", "C"));
                                add(new PlaceSearchItem("D", "D", "D"));
                                add(new PlaceSearchItem("E", "E", "E"));
                            }}
                        )
                        .build()
                )
            );
        Mockito.when(kakaoApi.search(query, 2, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.KAKAO)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("F", "F", "F"));
                                add(new PlaceSearchItem("G", "G", "G"));
                            }}
                        )
                        .build()
                )
            );
        Mockito.when(naverApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp(ApiType.NAVER)
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getItemCount(), 7);
        String result = resp.getPlaces()
            .stream()
            .map(p -> p.getTitle())
            .collect(Collectors.joining("-"));
        Assertions.assertEquals(result, "A-B-C-D-E-F-G");
    }

    @Test
    public void Search_Retry_When_HasSamePlace() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.KAKAO)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("A", "A", "A"));
                                add(new PlaceSearchItem("B", "B", "B"));
                                add(new PlaceSearchItem("C", "C", "C"));
                                add(new PlaceSearchItem("D", "D", "D"));
                                add(new PlaceSearchItem("E", "E", "E"));
                            }}
                        )
                        .build()
                )
            );
        Mockito.when(kakaoApi.search(query, 2, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.KAKAO)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("F", "F", "F"));
                                add(new PlaceSearchItem("G", "G", "G"));
                                add(new PlaceSearchItem("H", "H", "H"));
                                add(new PlaceSearchItem("I", "I", "I"));
                                add(new PlaceSearchItem("J", "J", "J"));
                            }}
                        )
                        .build()
                )
            );
        Mockito.when(naverApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiType.NAVER)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(new PlaceSearchItem("H", "H", "H"));
                                add(new PlaceSearchItem("I", "I", "I"));
                            }}
                        )
                        .build()
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getItemCount(), 10);
        String result = resp.getPlaces()
            .stream()
            .map(p -> p.getTitle())
            .collect(Collectors.joining("-"));
        Assertions.assertEquals(result, "H-I-A-B-C-D-E-F-G-J");
    }
}

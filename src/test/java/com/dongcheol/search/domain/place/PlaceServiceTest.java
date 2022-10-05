package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.infra.logservice.PlaceQueryLogger;
import com.dongcheol.search.infra.placesearch.ApiTypeEnum;
import com.dongcheol.search.infra.placesearch.PlaceSearch;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchItem;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    private PlaceQueryLogger queryLogger;

    @Test
    public void Search_EmptyResult_When_FailedExternalApis() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp(ApiTypeEnum.KAKAO)
                )
            );
        Mockito.when(naverApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp(ApiTypeEnum.NAVER)
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getPlaces().size(), 0);
    }

    @Test
    public void Search_HasResult_When_SuccOneApi() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query, 1, 5, null))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiTypeEnum.KAKAO)
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
                    PlaceSearchResp.createFailResp(ApiTypeEnum.NAVER)
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
                        .apiType(ApiTypeEnum.KAKAO)
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
                        .apiType(ApiTypeEnum.NAVER)
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
                        .apiType(ApiTypeEnum.KAKAO)
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
                        .apiType(ApiTypeEnum.NAVER)
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
                    PlaceSearchResp.createFailResp(ApiTypeEnum.NAVER)
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
                        .apiType(ApiTypeEnum.KAKAO)
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
                        .apiType(ApiTypeEnum.NAVER)
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
                        .apiType(ApiTypeEnum.NAVER)
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

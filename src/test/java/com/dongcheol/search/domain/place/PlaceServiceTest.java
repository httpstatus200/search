package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.infra.logservice.PlaceQueryLogger;
import com.dongcheol.search.infra.placesearch.ApiTypeEnum;
import com.dongcheol.search.infra.placesearch.PlaceSearch;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchItem;
import com.dongcheol.search.infra.placesearch.dto.PlaceSearchResp;
import java.util.ArrayList;
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
        Assertions.assertEquals(resp.getPlaces().size(), 2);
    }

    @Test
    public void Search_SortedResult_When_HasSamePlaces() {
        String query = "국민은행";
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
                                        .title("KB국민은행 여의도본점")
                                        .address("서울 영등포구 여의도동 36-3")
                                        .roadAddress("서울 영등포구 국제금융로8길 26")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 서여의도영업부")
                                        .address("서울 영등포구 여의도동 15-22")
                                        .roadAddress("서울 영등포구 의사당대로 13")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도영업부")
                                        .address("서울 영등포구 여의도동 36-3")
                                        .roadAddress("서울 영등포구 국제금융로8길 26")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도전산센터")
                                        .address("서울 영등포구 여의도동 15-22")
                                        .roadAddress("서울 영등포구 의사당대로 13")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도종합금융센터")
                                        .address("서울 영등포구 여의도동 35-3")
                                        .roadAddress("서울 영등포구 여의나루로 50")
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
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiTypeEnum.NAVER)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도영업부")
                                        .address("서울특별시 영등포구 여의도동 36-3")
                                        .roadAddress("서울특별시 영등포구 국제금융로8길 26")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 신관")
                                        .address("서울특별시 영등포구 여의도동 45")
                                        .roadAddress("서울특별시 영등포구 의사당대로 141")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도종합금융센터")
                                        .address("서울특별시 영등포구 여의도동 35-3")
                                        .roadAddress("서울특별시 영등포구 여의나루로 50 한국교직원공제회관")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 증권타운")
                                        .address("서울특별시 영등포구 여의도동 25-6")
                                        .roadAddress("서울특별시 영등포구 여의나루로 67 신송빌딩")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 서여의도영업부")
                                        .address("서울특별시 영등포구 여의도동 15-22")
                                        .roadAddress("서울특별시 영등포구 의사당대로 13")
                                        .build()
                                );
                            }}
                        )
                        .build()
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getPlaces().size(), 7);
    }

    @Test
    public void Search_When_HasSamePlaces() {
        String query = "국민은행";
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
                                        .title("KB국민은행 여의도본점")
                                        .address("서울 영등포구 여의도동 36-3")
                                        .roadAddress("서울 영등포구 국제금융로8길 26")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 서여의도영업부")
                                        .address("서울 영등포구 여의도동 15-22")
                                        .roadAddress("서울 영등포구 의사당대로 13")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도영업부")
                                        .address("서울 영등포구 여의도동 36-3")
                                        .roadAddress("서울 영등포구 국제금융로8길 26")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도전산센터")
                                        .address("서울 영등포구 여의도동 15-22")
                                        .roadAddress("서울 영등포구 의사당대로 13")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도종합금융센터")
                                        .address("서울 영등포구 여의도동 35-3")
                                        .roadAddress("서울 영등포구 여의나루로 50")
                                        .build()
                                );
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
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 신관")
                                        .address("서울특별시 영등포구 여의도동 45")
                                        .roadAddress("서울특별시 영등포구 의사당대로 141")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도종합금융센터")
                                        .address("서울특별시 영등포구 여의도동 35-3")
                                        .roadAddress("서울특별시 영등포구 여의나루로 50 한국교직원공제회관")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 증권타운")
                                        .address("서울특별시 영등포구 여의도동 25-6")
                                        .roadAddress("서울특별시 영등포구 여의나루로 67 신송빌딩")
                                        .build()
                                );
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 서여의도영업부")
                                        .address("서울특별시 영등포구 여의도동 15-22")
                                        .roadAddress("서울특별시 영등포구 의사당대로 13")
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
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType(ApiTypeEnum.NAVER)
                        .items(
                            new ArrayList<PlaceSearchItem>() {{
                                add(
                                    PlaceSearchItem.builder()
                                        .title("KB국민은행 여의도영업부")
                                        .address("서울특별시 영등포구 여의도동 36-3")
                                        .roadAddress("서울특별시 영등포구 국제금융로8길 26")
                                        .build()
                                );
                            }}
                        )
                        .build()
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getPlaces().size(), 7);
    }
}

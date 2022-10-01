package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceResp;
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
    @Qualifier("kakaoApi")
    private PlaceSearch kakaoApi;

    @MockBean
    @Qualifier("naverApi")
    private PlaceSearch naverApi;

    @Test
    public void Search_EmptyResult_When_FailedExternalApis() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp("kakao")
                )
            );
        Mockito.when(naverApi.search(query))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp("naver")
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getPlaces().size(), 0);
    }

    @Test
    public void Search_HasResult_When_SuccOneApi() {
        String query = "은행";
        Mockito.when(kakaoApi.search(query))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.builder()
                        .success(true)
                        .apiType("kakao")
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
        Mockito.when(naverApi.search(query))
            .thenReturn(
                Mono.just(
                    PlaceSearchResp.createFailResp("naver")
                )
            );

        PlaceResp resp = placeService.searchPlace(query);
        Assertions.assertEquals(resp.getPlaces().size(), 2);
    }
}

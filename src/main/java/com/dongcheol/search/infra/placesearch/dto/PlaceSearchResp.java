package com.dongcheol.search.infra.placesearch.dto;

import com.dongcheol.search.infra.placesearch.ApiTypeEnum;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceSearchResp {

    private ApiTypeEnum apiType;
    private boolean success;
    private List<PlaceSearchItem> items;

    public static PlaceSearchResp createFailResp(ApiTypeEnum apiType) {
        return PlaceSearchResp.builder()
            .apiType(apiType)
            .success(false)
            .items(new ArrayList<>())
            .build();
    }
}
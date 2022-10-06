package com.dongcheol.search.infra.placesearch.dto;

import com.dongcheol.search.infra.placesearch.type.ApiType;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceSearchResp {

    private ApiType apiType;
    private boolean success;
    private int itemCount;
    private List<PlaceSearchItem> items;

    public static PlaceSearchResp createFailResp(ApiType apiType) {
        return PlaceSearchResp.builder()
            .apiType(apiType)
            .success(false)
            .itemCount(0)
            .items(new ArrayList<>())
            .build();
    }
}

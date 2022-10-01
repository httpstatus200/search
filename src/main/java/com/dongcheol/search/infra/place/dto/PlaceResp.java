package com.dongcheol.search.infra.place.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceResp {

    private String apiType;
    private List<PlaceInfo> result;
}

package com.dongcheol.search.domain.place.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceResp {

    private List<PlaceInfo> places;
}

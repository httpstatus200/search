package com.dongcheol.search.domain.place.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceResp implements Serializable {

    private int itemCount;
    private List<PlaceInfo> places;
}

package com.dongcheol.search.domain.place.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceInfo {

    private String title;
    private String address;
    private String roadAddress;
    private String provider;
}

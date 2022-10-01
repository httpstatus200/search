package com.dongcheol.search.infra.placesearch.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceSearchItem {

    private String title;
    private String address;
    private String roadAddress;
}

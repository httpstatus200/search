package com.dongcheol.search.domain.place.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class PlaceInfo implements Serializable {

    private String title;
    private String address;
    private String roadAddress;
    private String provider;
}

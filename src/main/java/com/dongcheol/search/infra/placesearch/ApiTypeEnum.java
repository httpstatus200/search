package com.dongcheol.search.infra.placesearch;

public enum ApiTypeEnum {
    KAKAO("kakao"),
    NAVER("naver");

    private final String name;

    ApiTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}

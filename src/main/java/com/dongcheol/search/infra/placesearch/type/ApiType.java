package com.dongcheol.search.infra.placesearch.type;

public enum ApiType {
    KAKAO("kakao"),
    NAVER("naver");

    private final String name;

    ApiType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}

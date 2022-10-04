package com.dongcheol.search.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class PopularQuery {

    private String query;
    private long count;
}

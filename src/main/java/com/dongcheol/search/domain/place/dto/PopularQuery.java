package com.dongcheol.search.domain.place.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class PopularQuery implements Serializable {

    private String query;
    private long count;
}

package com.dongcheol.search.domain.place.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularQueryResp {

    private int itemCount;
    private List<PopularQuery> queries;
}

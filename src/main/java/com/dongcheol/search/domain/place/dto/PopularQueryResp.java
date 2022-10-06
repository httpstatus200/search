package com.dongcheol.search.domain.place.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularQueryResp implements Serializable {

    private int itemCount;
    private List<PopularQuery> queries;
}

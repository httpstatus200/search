package com.dongcheol.search.infra.logservice.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class PlaceQueryLog implements Serializable {

    @NonNull
    private String query;
    private Date datetime;

    public PlaceQueryLog(String query) {
        this.query = query;
        this.datetime = new Date();
    }
}

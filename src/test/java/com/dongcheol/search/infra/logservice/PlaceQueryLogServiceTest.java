package com.dongcheol.search.infra.logservice;

import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@Import({PlaceQueryLogService.class})
public class PlaceQueryLogServiceTest {

    @Autowired
    private PlaceQueryLogService placeQueryLogService;

    @BeforeEach
    public void beforeEach() {
        this.placeQueryLogService.getAllDel();
    }

    @Test
    public void Put_And_getAllDel() {
        this.placeQueryLogService.put(new PlaceQueryLog("은행"));
        this.placeQueryLogService.put(new PlaceQueryLog("곱창"));
        this.placeQueryLogService.put(new PlaceQueryLog("베이커리"));

        Assertions.assertEquals(this.placeQueryLogService.getAllDel().size(), 3);
        Assertions.assertEquals(this.placeQueryLogService.getAllDel().size(), 0);
    }
}

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
@Import({PlaceQueryLogger.class})
public class PlaceQueryLoggerTest {

    @Autowired
    private PlaceQueryLogger placeQueryLogger;

    @BeforeEach
    public void beforeEach() {
        this.placeQueryLogger.getAllDel();
    }

    @Test
    public void Put_And_getAllDel() {
        this.placeQueryLogger.put(new PlaceQueryLog("은행"));
        this.placeQueryLogger.put(new PlaceQueryLog("곱창"));
        this.placeQueryLogger.put(new PlaceQueryLog("베이커리"));

        Assertions.assertEquals(this.placeQueryLogger.getAllDel().size(), 3);
        Assertions.assertEquals(this.placeQueryLogger.getAllDel().size(), 0);
    }
}

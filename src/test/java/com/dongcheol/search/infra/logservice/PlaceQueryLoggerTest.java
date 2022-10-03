package com.dongcheol.search.infra.logservice;

import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import java.security.SecureRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
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

    @Test
    @Disabled
    public void Put_And_getAllDel_When_MultiThread() {
        Thread putThread1 = new Thread(() -> {
            while (true) {
                this.placeQueryLogger.put(new PlaceQueryLog("뱅크"));
                try {
                    Thread.sleep(new SecureRandom().nextInt(10));
                } catch (Exception e) {
                }
            }
        });
        Thread putThread2 = new Thread(() -> {
            while (true) {
                this.placeQueryLogger.put(new PlaceQueryLog("뱅크"));
                try {
                    Thread.sleep(new SecureRandom().nextInt(10));
                } catch (Exception e) {
                }
            }
        });

        Thread getThread = new Thread(() -> {
            while (true) {
                this.placeQueryLogger.getAllDel();
                try {
                    Thread.sleep(new SecureRandom().nextInt(50));
                } catch (Exception e) {
                }
            }
        });

        putThread1.start();
        putThread2.start();
        getThread.start();

        try {
            Thread.sleep(5000);
            this.placeQueryLogger.getAllDel();
            LoggerFactory.getLogger(PlaceQueryLoggerTest.class)
                .info("---------------------------------------");
            String re = "totalGet = " +
                this.placeQueryLogger.totalGet +
                ", " +
                "totalPut = " +
                this.placeQueryLogger.totalPut;
            LoggerFactory.getLogger(PlaceQueryLoggerTest.class).info(re);
            LoggerFactory.getLogger(PlaceQueryLoggerTest.class)
                .info("---------------------------------------");
        } catch (Exception e) {
        }
    }
}

package com.dongcheol.search.infra.logservice;

import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaceQueryLogService {

    protected static BlockingQueue<PlaceQueryLog> queue = new ArrayBlockingQueue<>(1000000);

    private PlaceQueryLogStore placeQueryLogStore;

    public PlaceQueryLogService(PlaceQueryLogStore placeQueryLogStore) {
        this.placeQueryLogStore = placeQueryLogStore;
    }

    @PostConstruct
    public void init() {
        Thread logCountSaver = new Thread(new PlaceQueryLogSaver(queue, placeQueryLogStore));
        logCountSaver.start();
    }

    public void put(PlaceQueryLog qLog) throws InterruptedException {
        this.queue.put(qLog);
    }
}

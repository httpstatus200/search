package com.dongcheol.search.infra.logservice;

import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PlaceQueryLogSystem implements Runnable {

    private BlockingQueue<PlaceQueryLog> queue;

    private PlaceQueryLogStore placeQueryLogStore;

    public PlaceQueryLogSystem(
        BlockingQueue<PlaceQueryLog> queue,
        PlaceQueryLogStore placeQueryLogStore
    ) {
        this.queue = queue;
        this.placeQueryLogStore = placeQueryLogStore;
    }

    @Override
    public void run() {
        while (true) {
            try {
                PlaceQueryLog qLog = queue.take();
                this.placeQueryLogStore.put(qLog);
            } catch (InterruptedException e) {
                log.error("QueryLogSaver 에러: ", e);
            }
        }
    }
}

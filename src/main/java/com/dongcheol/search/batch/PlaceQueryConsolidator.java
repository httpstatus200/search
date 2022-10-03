package com.dongcheol.search.batch;

import com.dongcheol.search.infra.logservice.PlaceQueryLogger;
import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaceQueryConsolidator {

    private PlaceQueryLogger placeQueryLogger;

    public PlaceQueryConsolidator(PlaceQueryLogger placeQueryLogger) {
        this.placeQueryLogger = placeQueryLogger;
    }

    @Scheduled(fixedDelay = 10_000)
    public void queryCount() {
        List<PlaceQueryLog> list = placeQueryLogger.getAllDel();
        Map<String, Integer> counter = new HashMap<>();

        for (PlaceQueryLog log : list) {
            String query = log.getQuery();
            if (!counter.containsKey(query)) {
                counter.put(query, 0);
            }

            counter.put(query, counter.get(query) + 1);
        }

        log.info(counter.toString());
    }
}

package com.dongcheol.search.batch;

import com.dongcheol.search.domain.place.QueryLogCount;
import com.dongcheol.search.domain.place.QueryLogCountRepository;
import com.dongcheol.search.infra.logservice.PlaceQueryLogStore;
import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaceQueryConsolidator {

    private PlaceQueryLogStore placeQueryLogStore;

    private QueryLogCountRepository queryLogCountRepository;

    public PlaceQueryConsolidator(PlaceQueryLogStore placeQueryLogStore,
        QueryLogCountRepository queryLogCountRepository) {
        this.placeQueryLogStore = placeQueryLogStore;
        this.queryLogCountRepository = queryLogCountRepository;
    }

    @Scheduled(fixedDelay = 10_000)
    public void queryCount() {
        // TODO: 에러로 인한 데이터 유실을 막기 위해 가져온 로그 정보 백업 필요
        List<PlaceQueryLog> list = placeQueryLogStore.getAllDel();
        Map<String, Integer> counter = new HashMap<>();

        for (PlaceQueryLog log : list) {
            String query = log.getQuery();
            if (!counter.containsKey(query)) {
                counter.put(query, 0);
            }

            counter.put(query, counter.get(query) + 1);
        }

        counter.entrySet()
            .stream()
            .forEach(entry -> {
                String query = entry.getKey();
                Long value = entry.getValue().longValue();

                Optional<QueryLogCount> qLog = queryLogCountRepository.findByQuery(query);
                if (qLog.isPresent()) {
                    queryLogCountRepository.increaseCount(qLog.get().getId(), value);
                } else {
                    queryLogCountRepository.save(new QueryLogCount(entry.getKey(), value));
                }
            });
    }
}

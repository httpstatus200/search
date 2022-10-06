package com.dongcheol.search.batch;

import com.dongcheol.search.domain.place.QueryLogCount;
import com.dongcheol.search.domain.place.QueryLogCountRepository;
import com.dongcheol.search.domain.place.dto.PopularQuery;
import com.dongcheol.search.domain.place.dto.PopularQueryResp;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PopularQueryWorker {

    private QueryLogCountRepository queryLogCountRepository;

    private CacheManager cacheManager;

    private String CACHE_NAME = "places";

    public PopularQueryWorker(
        QueryLogCountRepository queryLogCountRepository,
        CacheManager cacheManager
    ) {
        this.queryLogCountRepository = queryLogCountRepository;
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelay = 10000)
    public void cacheTop10() {
        List<QueryLogCount> logCounts = this.queryLogCountRepository.findTop10ByOrderByCountDesc();
        List<PopularQuery> popularQueries = logCounts.stream()
            .map(lc -> new PopularQuery(lc.getQuery(), lc.getCount()))
            .collect(Collectors.toList());

        Cache cache = this.cacheManager.getCache(this.CACHE_NAME);
        cache.put("queryTop10", new PopularQueryResp(popularQueries.size(), popularQueries));
    }
}

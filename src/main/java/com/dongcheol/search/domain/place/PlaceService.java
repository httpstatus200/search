package com.dongcheol.search.domain.place;

import com.dongcheol.search.infra.place.Kakao;
import com.dongcheol.search.infra.place.Naver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class PlaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceService.class);
    private Kakao kakaoApi;
    private Naver naverApi;

    public PlaceService(Kakao kakaoApi, Naver naverApi) {
        this.kakaoApi = kakaoApi;
        this.naverApi = naverApi;
    }

    public void searchPlace(String query) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        long start = System.currentTimeMillis();
        Flux.merge(naverApi.search(query), kakaoApi.search(query))
            .parallel()
            .runOn(Schedulers.parallel())
            .sequential()
            .doFinally(signal -> countDownLatch.countDown())
            .subscribe(data -> LOGGER.debug("external api result=" + data.toString()));

        try {
            countDownLatch.await(5, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - start;
            LOGGER.debug("external API execution time=" + duration + "ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceResp;
import com.dongcheol.search.domain.place.dto.PopularQueryResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/place")
public class PlaceController {

    private PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public ResponseEntity<PlaceResp> searchPlace(@RequestParam("q") String query) {
        PlaceResp resp = this.placeService.searchPlace(query);
        return ResponseEntity.status(HttpStatus.OK).body(resp);
    }

    @GetMapping("/queries/top10")
    public ResponseEntity<PopularQueryResp> queryTop10() {
        log.info("top 10");
        PopularQueryResp resp = this.placeService.queryTop10();
        return ResponseEntity.status(HttpStatus.OK).body(resp);
    }
}

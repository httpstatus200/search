package com.dongcheol.search.domain.place;

import com.dongcheol.search.domain.place.dto.PlaceResp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

package com.dongcheol.search.domain.place;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/place")
public class PlaceController {

    @GetMapping
    public ResponseEntity<String> searchPlace(@RequestParam("q") String query) {
        return ResponseEntity.ok(query);
    }
}

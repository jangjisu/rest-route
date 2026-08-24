package com.restroute.placesearch.controller;

import com.restroute.common.ApiResponse;
import com.restroute.placesearch.controller.response.PlaceCandidateResponse;
import com.restroute.placesearch.service.PlaceSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place-search")
public class PlaceSearchController {

    private final PlaceSearchService placeSearchService;

    /**
     * @param query 카카오 로컬 검색 키워드(장소명/주소). 예: "부산역"
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceCandidateResponse>>> search(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(placeSearchService.search(query)));
    }
}

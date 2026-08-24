package com.restroute.placesearch.service;

import com.restroute.common.client.KakaoMapClient;
import com.restroute.common.client.response.KakaoLocalSearchResponse;
import com.restroute.placesearch.controller.response.PlaceCandidateResponse;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private final KakaoMapClient kakaoMapClient;

    public List<PlaceCandidateResponse> search(String query) {
        KakaoLocalSearchResponse response = kakaoMapClient.searchKeyword(query);
        if (response.isEmpty()) {
            return List.of();
        }

        return response.documents().stream()
                .map(this::toCandidate)
                .filter(Objects::nonNull)
                .toList();
    }

    private PlaceCandidateResponse toCandidate(KakaoLocalSearchResponse.Document document) {
        Double longitude = parseCoordinate(document.x());
        Double latitude = parseCoordinate(document.y());
        if (longitude == null || latitude == null) {
            return null;
        }
        return PlaceCandidateResponse.of(document.label(), document.addressName(), latitude, longitude);
    }

    private Double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

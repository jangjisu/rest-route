package com.restroute.placesearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.restroute.client.KakaoMapClient;
import com.restroute.client.response.KakaoLocalSearchResponse;
import com.restroute.client.response.KakaoLocalSearchResponse.Document;
import com.restroute.placesearch.controller.response.PlaceCandidateResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

    @Mock
    private KakaoMapClient kakaoMapClient;

    private PlaceSearchService service;

    @BeforeEach
    void setUp() {
        service = new PlaceSearchService(kakaoMapClient);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
    void emptyResult() {
        when(kakaoMapClient.searchKeyword("없는곳")).thenReturn(new KakaoLocalSearchResponse(List.of()));
        assertThat(service.search("없는곳")).isEmpty();

        when(kakaoMapClient.searchKeyword("널")).thenReturn(new KakaoLocalSearchResponse(null));
        assertThat(service.search("널")).isEmpty();
    }

    @Test
    @DisplayName("후보를 이름/주소/좌표로 변환하고, 좌표가 없거나 잘못된 항목은 제외한다")
    void mapsCandidatesAndSkipsInvalidCoordinates() {
        List<Document> documents = List.of(
                new Document("129.0", "35.0", "부산역", "부산 어딘가"),
                new Document(null, "35.0", "경도없음", null),
                new Document("129.5", null, "위도없음", null),
                new Document("  ", "35.0", "공백좌표", null),
                new Document("abc", "35.0", "숫자아님", null),
                new Document("128.0", "36.0", "", "주소만"));
        when(kakaoMapClient.searchKeyword("부산")).thenReturn(new KakaoLocalSearchResponse(documents));

        List<PlaceCandidateResponse> candidates = service.search("부산");

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).name()).isEqualTo("부산역");
        assertThat(candidates.get(0).address()).isEqualTo("부산 어딘가");
        assertThat(candidates.get(0).latitude()).isEqualTo(35.0);
        assertThat(candidates.get(0).longitude()).isEqualTo(129.0);
        assertThat(candidates.get(1).name()).isEqualTo("주소만");
    }
}

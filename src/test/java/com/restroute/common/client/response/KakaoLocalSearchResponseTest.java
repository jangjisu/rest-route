package com.restroute.common.client.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.common.client.response.KakaoLocalSearchResponse.Document;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoLocalSearchResponseTest {

    @Test
    @DisplayName("isEmpty는 documents가 null이거나 비었을 때 true다")
    void isEmpty() {
        assertThat(new KakaoLocalSearchResponse(null).isEmpty()).isTrue();
        assertThat(new KakaoLocalSearchResponse(List.of()).isEmpty()).isTrue();
        assertThat(new KakaoLocalSearchResponse(List.of(new Document("1", "2", "p", null))).isEmpty())
                .isFalse();
    }

    @Test
    @DisplayName("first는 첫 번째 document를 반환한다")
    void first() {
        Document document = new Document("129.0", "35.0", "부산역", null);
        KakaoLocalSearchResponse response = new KakaoLocalSearchResponse(List.of(document));

        assertThat(response.first()).isSameAs(document);
    }

    @Test
    @DisplayName("label은 placeName이 있으면 placeName, 없거나 비면 addressName을 쓴다")
    void label() {
        assertThat(new Document("1", "2", "부산역", "부산 어딘가").label()).isEqualTo("부산역");
        assertThat(new Document("1", "2", "", "부산 어딘가").label()).isEqualTo("부산 어딘가");
        assertThat(new Document("1", "2", null, "부산 어딘가").label()).isEqualTo("부산 어딘가");
    }
}

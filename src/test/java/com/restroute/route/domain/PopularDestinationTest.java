package com.restroute.route.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PopularDestinationTest {

    @Test
    @DisplayName("표시명으로 찾으면 그 목적지가 나온다")
    void findByDisplayName_returnsMatchingDestination() {
        assertThat(PopularDestination.findByDisplayName("부산역")).contains(PopularDestination.BUSAN_STATION);
        assertThat(PopularDestination.findByDisplayName("광주송정역"))
                .contains(PopularDestination.GWANGJU_SONGJEONG_STATION);
    }

    @Test
    @DisplayName("목록에 없는 이름이면 비어 있다")
    void findByDisplayName_returnsEmptyForUnknownName() {
        assertThat(PopularDestination.findByDisplayName("없는곳")).isEmpty();
        assertThat(PopularDestination.findByDisplayName(null)).isEmpty();
    }

    /**
     * 좌표가 대한민국 안에 있는지만 본다 — 정확한 값은 여기서 지킬 수 없지만, 부호가 뒤집히거나
     * 위경도가 서로 바뀌는 실수는 이 범위에서 걸린다.
     */
    @Test
    @DisplayName("모든 목적지 좌표가 대한민국 범위 안에 있다")
    void allDestinations_haveCoordinatesWithinKorea() {
        assertThat(Arrays.asList(PopularDestination.values())).allSatisfy(destination -> {
            assertThat(destination.latitude()).isBetween(33.0, 39.0);
            assertThat(destination.longitude()).isBetween(124.0, 132.0);
        });
    }

    @Test
    @DisplayName("표시명이 서로 겹치지 않는다")
    void displayNames_areUnique() {
        assertThat(Arrays.stream(PopularDestination.values())
                        .map(PopularDestination::displayName)
                        .distinct()
                        .count())
                .isEqualTo(PopularDestination.values().length);
    }

    /**
     * 프론트 칩이 보내는 이름과 이 enum이 아는 이름이 어긋나면 칩이 조용히 404를 받는다 — 컴파일도
     * 통과하고 각자의 단위 테스트도 통과하므로 배포 전까지 아무도 모른다. 두 목록이 다른 언어에 있어
     * 어느 한쪽이 상대 소스를 읽는 수밖에 없고, 프론트 상수를 사람이 손으로 옮겨 적는 JS 테스트로는
     * 그 경계를 지킬 수 없어 여기서 실제 파일을 읽어 비교한다.
     */
    @Test
    @DisplayName("프론트 칩 목록과 서버가 아는 목적지 이름이 정확히 일치한다")
    void chipNamesInFrontend_matchEnumDisplayNames() throws IOException {
        List<String> chipNames = destinationNamesFromChipModule();

        assertThat(chipNames)
                .as("finder-destination-chips.js의 destinationName과 PopularDestination의 표시명")
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(PopularDestination.values())
                        .map(PopularDestination::displayName)
                        .toList());
    }

    private List<String> destinationNamesFromChipModule() throws IOException {
        Path chipModule = Path.of("src/main/resources/static/js/finder-destination-chips.js");
        assertThat(chipModule).as("프론트 칩 모듈이 이 경로에 있어야 비교할 수 있다").exists();

        Matcher matcher = Pattern.compile("destinationName:\\s*'([^']+)'").matcher(Files.readString(chipModule));
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        assertThat(names).as("칩 모듈에서 destinationName을 하나도 읽지 못했다").isNotEmpty();
        return names;
    }
}

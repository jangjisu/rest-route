package com.restroute.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PagedFetchTemplateTest {

    private record Page(int totalPageCount, List<String> items) {}

    @Test
    @DisplayName("총 페이지 수만큼 순서대로 호출해 모든 항목을 모은다")
    void fetchAll_collectsItemsAcrossAllPages() {
        Map<Integer, Page> pages = Map.of(
                1, new Page(3, List.of("a1", "a2")),
                2, new Page(3, List.of("b1")),
                3, new Page(3, List.of("c1")));

        List<String> items = PagedFetchTemplate.fetchAll("테스트", pages::get, Page::totalPageCount, Page::items);

        assertThat(items).containsExactly("a1", "a2", "b1", "c1");
    }

    @Test
    @DisplayName("첫 페이지 호출이 실패(null)하면 빈 목록을 반환한다")
    void fetchAll_returnsEmptyWhenFirstPageFails() {
        List<String> items = PagedFetchTemplate.fetchAll("테스트", pageNo -> null, Page::totalPageCount, Page::items);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("중간 페이지 호출이 예외를 던지면 해당 페이지만 건너뛰고 나머지는 모은다")
    void fetchAll_skipsPageWhenFetcherThrows() {
        Map<Integer, Page> pages = Map.of(
                1, new Page(3, List.of("a1")),
                3, new Page(3, List.of("c1")));

        List<String> items = PagedFetchTemplate.fetchAll(
                "테스트",
                pageNo -> {
                    if (pageNo == 2) {
                        throw new RuntimeException("boom");
                    }
                    return pages.get(pageNo);
                },
                Page::totalPageCount,
                Page::items);

        assertThat(items).containsExactly("a1", "c1");
    }

    @Test
    @DisplayName("응답의 항목 목록이 null이면 그 페이지는 건너뛴다")
    void fetchAll_skipsPageWithNullItemList() {
        List<String> items =
                PagedFetchTemplate.fetchAll("테스트", pageNo -> new Page(1, null), Page::totalPageCount, Page::items);

        assertThat(items).isEmpty();
    }
}

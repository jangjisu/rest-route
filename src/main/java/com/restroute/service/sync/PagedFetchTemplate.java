package com.restroute.service.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부 API를 페이지 단위로 끝까지 순회하며 항목을 모으는 동기화 서비스들의 공통 패턴을 묶는다.
 * 첫 페이지에서 전체 페이지 수를 얻고, 이후 각 페이지는 개별적으로 실패해도(예외/응답 없음)
 * 전체 수집을 중단하지 않고 건너뛴다.
 */
@Slf4j
public final class PagedFetchTemplate {

    private static final int FIRST_PAGE = 1;

    private PagedFetchTemplate() {}

    public static <RESPONSE, ITEM> List<ITEM> fetchAll(
            String syncLabel,
            IntFunction<RESPONSE> pageFetcher,
            ToIntFunction<RESPONSE> totalPageCountFn,
            Function<RESPONSE, List<ITEM>> itemsFn) {
        RESPONSE firstPage = fetchPageSafely(syncLabel, pageFetcher, FIRST_PAGE);
        if (firstPage == null) {
            return List.of();
        }

        List<ITEM> items = new ArrayList<>();
        addItems(items, firstPage, itemsFn);

        int totalPageCount = totalPageCountFn.applyAsInt(firstPage);
        for (int pageNo = FIRST_PAGE + 1; pageNo <= totalPageCount; pageNo++) {
            RESPONSE response = fetchPageSafely(syncLabel, pageFetcher, pageNo);
            if (response != null) {
                addItems(items, response, itemsFn);
            }
        }

        return items;
    }

    private static <RESPONSE> RESPONSE fetchPageSafely(
            String syncLabel, IntFunction<RESPONSE> pageFetcher, int pageNo) {
        try {
            return pageFetcher.apply(pageNo);
        } catch (RuntimeException e) {
            log.warn("{} page fetch failed. pageNo={}, cause={}", syncLabel, pageNo, e.getMessage(), e);
            return null;
        }
    }

    private static <RESPONSE, ITEM> void addItems(
            List<ITEM> items, RESPONSE response, Function<RESPONSE, List<ITEM>> itemsFn) {
        List<ITEM> pageItems = itemsFn.apply(response);
        if (pageItems != null) {
            items.addAll(pageItems);
        }
    }
}
